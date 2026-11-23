import hashlib
import json
import os
import re
from dataclasses import replace
from email.utils import parsedate_to_datetime
from urllib.error import HTTPError, URLError
from urllib.parse import parse_qsl, urlencode, urlsplit, urlunsplit
from urllib.request import HTTPRedirectHandler, Request, build_opener

from lxml import etree, html

from .avisos import clasificar
from .cortes import HORA_PERU, normalizar
from .eps import extraer_comunicado
from .sunass import fecha_sunass

RADIO_UNO_RSS = "https://radiouno.pe/feed/"


def url_facebook(url):
    datos = urlsplit(url)
    if datos.scheme != "https" or datos.netloc not in ("www.facebook.com", "facebook.com", "m.facebook.com", "web.facebook.com"):
        raise ValueError("Usa una URL de publicación de facebook.com.")
    parametros = [(k, v) for k, v in parse_qsl(datos.query)
                  if k in ("id", "story_fbid", "fbid", "v") and re.fullmatch(r"[a-zA-Z0-9_]+", v)]
    if not datos.path.strip("/"):
        raise ValueError("Copia el enlace de la publicación de EPS Tacna.")
    return urlunsplit(("https", "www.facebook.com", datos.path.rstrip("/"), urlencode(parametros), ""))


def aviso_facebook(url, texto, publicado, titulo=None, texto_imagen=""):
    url = url_facebook(url)
    publicado = fecha_sunass(publicado) if isinstance(publicado, str) else None
    if not publicado:
        raise ValueError("La publicación requiere una fecha válida.")
    contenido = "\n\n".join(dict.fromkeys(p.strip() for p in (texto, texto_imagen) if p.strip()))
    if not contenido:
        return []
    if not titulo:
        primera_linea = next((linea.strip() for linea in contenido.splitlines() if clasificar(linea, "") != "COMUNICADO"),
                             contenido.splitlines()[0].strip())
        titulo = re.sub(r"^[^\w]+", "", primera_linea).strip()[:150] or "Comunicado de EPS Tacna"
    clave = hashlib.sha256(url.encode()).hexdigest()[:24]
    avisos = extraer_comunicado(f"eps-facebook-{clave}", "EPS Tacna · Facebook", url, titulo, contenido, publicado)
    if texto_imagen:
        detalle = " ".join(contenido.split())[:12000]
        return [replace(aviso, detalle=f"Texto de la publicación y de sus imágenes (OCR). {detalle}") for aviso in avisos]
    return avisos


class SinRedireccionesGraph(HTTPRedirectHandler):
    def redirect_request(self, *args, **kwargs):
        return None


def publicaciones_graph(desde=None):
    pagina = os.environ.get("EPS_FACEBOOK_PAGE_ID", "")
    token = os.environ.get("EPS_FACEBOOK_TOKEN", "")
    version = os.environ.get("EPS_FACEBOOK_GRAPH_VERSION", "v27.0")
    if not re.fullmatch(r"[0-9]+", pagina) or not token.strip() or not re.fullmatch(r"v[0-9]+\.0", version):
        raise ValueError("Configura el ID numérico, el token autorizado y la versión de Meta Graph.")
    opener = build_opener(SinRedireccionesGraph())
    campos = {
        "fields": "id,message,created_time,permalink_url,full_picture,"
                  "attachments{media_type,media,subattachments.limit(100){media_type,media}}",
        "limit": 50
    }
    if desde is not None:
        campos["since"] = int(desde.timestamp())
    vistos = set()
    while True:
        url = f"https://graph.facebook.com/{version}/{pagina}/published_posts?{urlencode(campos)}"
        solicitud = Request(url, headers={"Authorization": f"Bearer {token}"})
        try:
            with opener.open(solicitud, timeout=15) as respuesta:
                datos = json.loads(respuesta.read(5 * 1024 * 1024))
        except HTTPError as error:
            raise ValueError(f"Meta Graph devolvió HTTP {error.code}. Revisa el token y los permisos de la página.") from None
        except URLError:
            raise ValueError("No se pudo conectar con Meta Graph.") from None
        if not isinstance(datos, dict) or not isinstance(datos.get("data"), list):
            raise ValueError("Meta Graph no devolvió una lista de publicaciones.")
        yield from datos["data"]
        pagina_siguiente = datos.get("paging", {})
        cursor = pagina_siguiente.get("cursors", {}).get("after")
        if not cursor or not pagina_siguiente.get("next"):
            return
        if cursor in vistos:
            raise ValueError("Meta Graph repitió el cursor de publicaciones.")
        vistos.add(cursor)
        campos["after"] = cursor


def leer_rss_radio_uno(documento):
    parser = etree.XMLParser(resolve_entities=False, no_network=True, load_dtd=False)
    raiz = etree.fromstring(documento, parser)
    if raiz.tag != "rss":
        raise ValueError("Radio Uno no devolvió un RSS válido.")
    for nota in raiz.findall("./channel/item"):
        titulo = nota.findtext("title", "").strip()
        url = nota.findtext("link", "").strip()
        descripcion = nota.findtext("description", "")
        texto = html.fromstring(descripcion).text_content() if descripcion.strip() else ""
        datos = urlsplit(url)
        codigo = re.fullmatch(r"/noticias/(\d+)(?:/[^/]+)?/?", datos.path)
        if datos.netloc not in ("radiouno.pe", "www.radiouno.pe") or datos.scheme != "https" or not codigo or not titulo:
            continue
        if "eps tacna" not in normalizar(f"{titulo} {texto}") or clasificar(titulo, texto) in ("COMUNICADO", "ATENCION"):
            continue
        try:
            publicado = parsedate_to_datetime(nota.findtext("pubDate", "")).astimezone(HORA_PERU).isoformat(timespec="minutes")
        except (ValueError, TypeError):
            publicado = None
        url = urlunsplit(("https", "radiouno.pe", datos.path, "", ""))
        yield extraer_comunicado(f"radiouno-{codigo[1]}", "Radio Uno", url, titulo, texto[:320], publicado)
