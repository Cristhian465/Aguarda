import argparse
import json
import os
import subprocess
import sys
import time
from dataclasses import replace
from datetime import datetime, timedelta
from pathlib import Path
from urllib.parse import urlsplit
from urllib.request import HTTPRedirectHandler, Request, build_opener

from lxml.etree import LxmlError

from .api import base_configurada
from .cortes import HORA_PERU
from .eps import LISTADO_EPS, LISTADO_SUNASS, enlaces_eps, extraer_aviso_eps, texto_eps, url_aviso
from .facebook_ocr import imagenes_publicacion, leer_imagen_facebook
from .facebook_rss import publicaciones_rss, url_rss_facebook
from .fuentes import RADIO_UNO_RSS, aviso_facebook, leer_rss_radio_uno, publicaciones_graph
from .sunass import fecha_sunass, leer_csv_sunass

MAX_BYTES = 30 * 1024 * 1024
ERRORES_FUENTE = (OSError, ValueError, LxmlError)


class RedireccionOficial(HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        comprobar_url(newurl)
        return super().redirect_request(req, fp, code, msg, headers, newurl)


def comprobar_url(url):
    partes = urlsplit(url)
    hosts = ("gob.pe", "sunass.gob.pe", "datosabiertos.gob.pe", "radiouno.pe")
    host = partes.hostname or ""
    oficial = any(host == dominio or host.endswith("." + dominio) for dominio in hosts)
    if host.removeprefix("www.") in ("rss.app", "fetchrss.com"):
        url_rss_facebook(url)
        return
    if partes.scheme != "https" or not (oficial or host == "camiyaku.vercel.app"):
        raise ValueError("Usa una URL HTTPS de una de las fuentes configuradas.")
    if partes.username or partes.password or partes.port not in (None, 443):
        raise ValueError("Usa la URL pública de la fuente.")


def descargar(url):
    comprobar_url(url)
    solicitud = Request(url, headers={"User-Agent": "Aguarda-Cortes/1.0 (avisos públicos)"})
    with build_opener(RedireccionOficial()).open(solicitud, timeout=15) as respuesta:
        contenido = respuesta.read(MAX_BYTES + 1)
    if len(contenido) > MAX_BYTES:
        raise ValueError("La fuente supera el tamaño admitido de 30 MB.")
    return contenido


def guardar_aviso(base, url, titulo, texto, publicado=None, distrito=None, sector=None):
    cortes = extraer_aviso_eps(url, titulo, texto, publicado)
    if distrito:
        cortes = [replace(cortes[0], distrito=distrito, sector=sector or cortes[0].sector)] if cortes else []
    elif sector:
        cortes = [replace(corte, sector=sector) for corte in cortes]
    if not cortes:
        return 0
    return base.guardar(cortes, reemplazar_articulo=url_aviso(url))


def importar_eps(base, args):
    if args.texto:
        contenido = args.texto.read_text(encoding="utf-8-sig")
        titulo = args.titulo
        publicado = fecha_sunass(args.publicado)
        if not titulo or (args.publicado and not publicado):
            raise ValueError("El texto revisado requiere --titulo. Usa AAAA-MM-DD en --publicado.")
    else:
        url = url_aviso(args.url)
        if url is None:
            raise ValueError("Usa la URL oficial del comunicado de EPS Tacna en gob.pe.")
        documento = args.html.read_bytes() if args.html else descargar(url)
        titulo, contenido, publicado = texto_eps(documento)
    return guardar_aviso(base, args.url, titulo, contenido, publicado, args.distrito, args.sector)


def importar_facebook(base, publicaciones, *, con_ocr=False):
    total, sin_texto = 0, 0
    errores = 0
    for post in publicaciones:
        if not isinstance(post, dict):
            continue
        texto = post.get("message") or ""
        texto_imagen = []
        imagen_incompleta = False
        if con_ocr:
            for imagen in imagenes_publicacion(post):
                try:
                    extraido = leer_imagen_facebook(imagen, base.ruta.parent / "facebook-ocr", de_rss="imagenes_rss" in post)
                    if extraido:
                        texto_imagen.append(extraido)
                except (OSError, ValueError, subprocess.SubprocessError) as error:
                    # La excepción de HTTP puede contener la URL firmada del archivo.
                    print(f"Facebook OCR: fallo de lectura ({type(error).__name__}). Se reintentará en la próxima consulta.",
                          file=sys.stderr)
                    imagen_incompleta = True
                    errores += 1
        if not texto.strip() and not texto_imagen:
            sin_texto += 1
            continue
        if imagen_incompleta:
            continue
        try:
            avisos = aviso_facebook(post.get("permalink_url") or "", texto, post.get("created_time"),
                                   texto_imagen="\n\n".join(texto_imagen))
        except ValueError as error:
            errores += 1
            print(f"Facebook: {error}", file=sys.stderr)
            continue
        total += base.guardar(avisos, reemplazar_articulo=avisos[0].url)
    if sin_texto:
        print(f"Facebook: {sin_texto} publicaciones sin texto recuperable.")
    if errores:
        print(f"Facebook: importación parcial. {total} registros guardados, {errores} errores.", file=sys.stderr)
        raise ValueError("Hubo publicaciones o imágenes que no se pudieron procesar.")
    return total


def sincronizar_noticias(base, listado, fuente, limite):
    urls = enlaces_eps(descargar(listado), listado)
    if not urls:
        raise ValueError("No se encontraron enlaces de comunicados en el listado.")
    total, errores = 0, 0
    for url in urls[:limite]:
        try:
            titulo, texto, publicado = texto_eps(descargar(url))
            total += guardar_aviso(base, url, titulo, texto, publicado)
        except ERRORES_FUENTE as error:
            errores += 1
            print(f"{fuente}, {url}: {error}", file=sys.stderr)
    print(f"{fuente}: {total} registros importados.")
    return errores


def sincronizar(base, args):
    errores = 0
    rss_url = os.environ.get("EPS_FACEBOOK_RSS_URL", "").strip()
    if (args.facebook or args.facebook_historial or args.solo_facebook or
            rss_url or os.environ.get("EPS_FACEBOOK_PAGE_ID") or os.environ.get("EPS_FACEBOOK_TOKEN")):
        try:
            if rss_url and not args.facebook_historial:
                publicaciones = publicaciones_rss(descargar(url_rss_facebook(rss_url)))
                proveedor = "RSS"
            else:
                desde = None if args.facebook_historial else datetime.now(HORA_PERU) - timedelta(days=14)
                publicaciones = publicaciones_graph(desde)
                proveedor = "Graph"
            total = importar_facebook(base, publicaciones, con_ocr=True)
            print(f"EPS Tacna · Facebook ({proveedor}): {total} registros importados.")
        except ERRORES_FUENTE as error:
            errores += 1
            print(f"Facebook: {error}", file=sys.stderr)
    if args.solo_facebook:
        return errores == 0
    if args.sunass_url:
        try:
            total = base.guardar(leer_csv_sunass(descargar(args.sunass_url)))
            print(f"SUNASS: {total} registros de Tacna importados.")
        except ERRORES_FUENTE as error:
            errores += 1
            print(f"SUNASS: {error}", file=sys.stderr)
    if args.eps or not args.sunass_url:
        for listado, fuente in ((LISTADO_EPS, "EPS Tacna"), (LISTADO_SUNASS, "SUNASS · Noticias")):
            try:
                errores += sincronizar_noticias(base, listado, fuente, args.limite_eps)
            except ERRORES_FUENTE as error:
                errores += 1
                print(f"{fuente}: {error}", file=sys.stderr)
        try:
            total = sum(base.guardar(avisos, reemplazar_articulo=avisos[0].url)
                        for avisos in leer_rss_radio_uno(descargar(RADIO_UNO_RSS)) if avisos)
            print(f"Radio Uno: {total} registros importados.")
        except ERRORES_FUENTE as error:
            errores += 1
            print(f"Radio Uno: {error}", file=sys.stderr)
    return errores == 0


def argumentos():
    cli = argparse.ArgumentParser(description="Importa avisos públicos de cortes para Aguarda.")
    comandos = cli.add_subparsers(dest="comando", required=True)
    sunass = comandos.add_parser("sunass", help="Importa un CSV descargado de SUNASS.")
    sunass.add_argument("archivo", type=Path)
    eps = comandos.add_parser("eps", help="Extrae datos de un comunicado oficial de EPS Tacna.")
    eps.add_argument("url")
    entrada = eps.add_mutually_exclusive_group()
    entrada.add_argument("--html", type=Path)
    entrada.add_argument("--texto", type=Path)
    eps.add_argument("--titulo")
    eps.add_argument("--publicado")
    eps.add_argument("--distrito")
    eps.add_argument("--sector")
    facebook = comandos.add_parser("facebook", help="Importa una publicación revisada o un archivo JSON de Meta.")
    entrada_fb = facebook.add_mutually_exclusive_group(required=True)
    entrada_fb.add_argument("--archivo", type=Path)
    entrada_fb.add_argument("--texto", type=Path)
    facebook.add_argument("--url")
    facebook.add_argument("--publicado")
    facebook.add_argument("--titulo")
    sync = comandos.add_parser("sincronizar", help="Consulta fuentes públicas y actualiza SQLite.")
    sync.add_argument("--sunass-url", help="Enlace directo al CSV público de SUNASS.")
    sync.add_argument("--eps", action="store_true", help="Incluye el listado de EPS al usar --sunass-url.")
    sync.add_argument("--facebook", action="store_true", help="Solicita acceso a la página oficial con Meta Graph.")
    sync.add_argument("--facebook-historial", action="store_true", help="Recorre todas las páginas de publicaciones que Meta devuelve.")
    sync.add_argument("--solo-facebook", action="store_true", help="Consulta los avisos de Facebook sin sincronizar otras fuentes.")
    sync.add_argument("--limite-eps", type=int, choices=range(1, 21), metavar="1-20", default=10)
    sync.add_argument("--cada-minutos", type=int, metavar="N", help="Repite la consulta cada N minutos (mínimo 5).")
    ocr = comandos.add_parser("ocr", help="Extrae texto de una imagen para revisión con Tesseract.")
    ocr.add_argument("imagen", type=Path)
    ocr.add_argument("salida", type=Path)
    comandos.add_parser("ejemplos", help="Carga dos comunicados históricos reales de EPS Tacna.")
    return cli.parse_args()


def main():
    args = argumentos()
    base = base_configurada()
    try:
        if args.comando == "sincronizar":
            if args.cada_minutos is not None:
                if args.cada_minutos < 5:
                    raise ValueError("--cada-minutos debe ser 5 o más.")
                while True:
                    sincronizar(base, args)
                    time.sleep(args.cada_minutos * 60)
            return 0 if sincronizar(base, args) else 1
        if args.comando == "ocr":
            resultado = subprocess.run(
                ["tesseract", str(args.imagen.resolve(strict=True)), "stdout", "-l", "spa"],
                check=True, capture_output=True, timeout=60
            )
            args.salida.write_bytes(resultado.stdout)
            print(f"Revisa el texto antes de importarlo: {args.salida}")
            return 0
        if args.comando == "sunass":
            total = base.guardar(leer_csv_sunass(args.archivo.read_bytes()))
        elif args.comando == "eps":
            total = importar_eps(base, args)
        elif args.comando == "facebook":
            if args.archivo:
                publicaciones = json.loads(args.archivo.read_text(encoding="utf-8-sig"))
                publicaciones = publicaciones.get("data") if isinstance(publicaciones, dict) else publicaciones
                if not isinstance(publicaciones, list) or not all(isinstance(post, dict) for post in publicaciones):
                    raise ValueError("El archivo debe contener una lista de publicaciones de la página oficial.")
                total = importar_facebook(base, publicaciones)
            else:
                if not args.url or not args.publicado:
                    raise ValueError("El texto revisado requiere --url y --publicado.")
                avisos = aviso_facebook(args.url, args.texto.read_text(encoding="utf-8-sig"), args.publicado, args.titulo)
                total = base.guardar(avisos, reemplazar_articulo=avisos[0].url) if avisos else 0
        else:
            archivo = Path(__file__).with_name("ejemplos_eps.json")
            ejemplos = json.loads(archivo.read_text(encoding="utf-8"))
            total = sum(guardar_aviso(base, **aviso) for aviso in ejemplos)
    except KeyboardInterrupt:
        return 130
    except (*ERRORES_FUENTE, subprocess.SubprocessError) as error:
        print(f"Importación fallida: {error}", file=sys.stderr)
        return 1
    print(f"{total} registros importados.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
