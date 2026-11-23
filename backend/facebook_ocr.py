import hashlib
import subprocess
from pathlib import Path
from tempfile import NamedTemporaryFile
from urllib.parse import urlsplit
from urllib.request import Request, build_opener

from .fuentes import SinRedireccionesGraph

MAX_IMAGEN = 12 * 1024 * 1024


def imagenes_publicacion(post):
    if "imagenes_rss" in post:
        return post["imagenes_rss"]

    def imagenes(adjuntos):
        for adjunto in adjuntos:
            if not isinstance(adjunto, dict):
                continue
            hijos = (adjunto.get("subattachments") or {}).get("data") or []
            if hijos:
                yield from imagenes(hijos)
            elif (adjunto.get("media_type") or "").lower() in ("", "photo", "image", "album"):
                imagen = (adjunto.get("media") or {}).get("image") or {}
                if isinstance(imagen.get("src"), str):
                    yield imagen["src"]

    adjuntos = (post.get("attachments") or {}).get("data") or []
    urls = list(dict.fromkeys(imagenes(adjuntos)))
    imagen = post.get("full_picture")
    son_previsualizaciones = adjuntos and all(
        isinstance(a, dict) and (a.get("media_type") or "").lower() in ("link", "video", "event") for a in adjuntos
    )
    if not urls and isinstance(imagen, str) and not son_previsualizaciones:
        urls.append(imagen)
    return urls


def _clave_imagen(url, *, de_rss=False):
    datos = urlsplit(url)
    host = datos.hostname or ""
    es_meta = host.endswith((".fbcdn.net", ".fbsbx.com"))
    es_proveedor = host in ("rss.app", "fetchrss.com") or host.endswith((".rss.app", ".fetchrss.com"))
    if (datos.scheme != "https" or not (es_meta or (de_rss and es_proveedor)) or
            datos.username or datos.password or datos.port not in (None, 443) or not datos.path.strip("/")):
        raise ValueError("La imagen no pertenece a una CDN autorizada.")
    # La firma de la URL cambia entre consultas y la ruta del archivo identifica la imagen.
    clave = datos.path if es_meta else url
    return hashlib.sha256(clave.encode()).hexdigest()


def es_imagen_facebook(url, *, de_rss=False):
    try:
        _clave_imagen(url, de_rss=de_rss)
    except ValueError:
        return False
    return not urlsplit(url).path.casefold().startswith(("/rsrc.php", "/images/emoji"))


def _descargar_imagen(url):
    solicitud = Request(url, headers={"Accept": "image/*", "User-Agent": "Aguarda/1.0"})
    with build_opener(SinRedireccionesGraph()).open(solicitud, timeout=15) as respuesta:
        if respuesta.headers.get_content_maintype() != "image":
            raise ValueError("La fuente devolvió un archivo que no es una imagen.")
        imagen = respuesta.read(MAX_IMAGEN + 1)
    if not imagen or len(imagen) > MAX_IMAGEN:
        raise ValueError("La imagen está vacía o supera los 12 MB.")
    return imagen


def leer_imagen_facebook(url, cache: Path, *, de_rss=False):
    clave = _clave_imagen(url, de_rss=de_rss)
    archivo = cache / (clave + ".txt")
    if archivo.is_file():
        return archivo.read_text(encoding="utf-8")

    imagen = _descargar_imagen(url)
    resultado = subprocess.run(
        ["tesseract", "stdin", "stdout", "-l", "spa"], input=imagen,
        check=True, capture_output=True, timeout=45
    )
    texto = resultado.stdout.decode("utf-8", errors="replace").strip()[:10000]
    cache.mkdir(parents=True, exist_ok=True)
    with NamedTemporaryFile(mode="w", encoding="utf-8", dir=cache, delete=False) as temporal:
        temporal.write(texto)
    ruta_temporal = Path(temporal.name)
    try:
        ruta_temporal.replace(archivo)
    finally:
        ruta_temporal.unlink(missing_ok=True)
    return texto
