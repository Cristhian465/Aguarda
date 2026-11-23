import re
from email.utils import parsedate_to_datetime
from urllib.parse import urlsplit

from lxml import etree, html

from .facebook_ocr import es_imagen_facebook
from .fuentes import url_facebook
from .sunass import fecha_sunass

PAGINA_EPS = "epstacna"
RUTAS_PROVEEDORES = {
    "rss.app": r"/feeds/[A-Za-z0-9_-]+\.xml",
    "fetchrss.com": r"/(?:rss/[A-Za-z0-9_-]+(?:\.xml)?|feed/[A-Za-z0-9_-]+\.rss)",
}
MEDIA = "{http://search.yahoo.com/mrss/}"
CONTENIDO = "{http://purl.org/rss/1.0/modules/content/}encoded"
FECHA_DC = "{http://purl.org/dc/elements/1.1/}date"


def url_rss_facebook(url):
    partes = urlsplit(url)
    patron = RUTAS_PROVEEDORES.get((partes.hostname or "").removeprefix("www."))
    if (partes.scheme != "https" or not patron or not re.fullmatch(patron, partes.path)
            or partes.username or partes.password or partes.port not in (None, 443)):
        raise ValueError("Usa el enlace XML del feed de EPS Tacna generado en RSS.app o FetchRSS.")
    return url


def _fecha_item(item):
    fecha = item.findtext("pubDate", "").strip()
    if fecha:
        try:
            return fecha_sunass(parsedate_to_datetime(fecha).isoformat())
        except (TypeError, ValueError):
            pass
    return fecha_sunass(item.findtext(FECHA_DC, "").strip() or fecha)


def _enlace_publicacion(item, canal_eps):
    for valor in (item.findtext("link", ""), item.findtext("guid", "")):
        try:
            valor = valor.strip()
            if valor.startswith("http://"):
                valor = "https://" + valor[len("http://"):]
            url = url_facebook(valor)
        except ValueError:
            continue
        partes = urlsplit(url)
        ruta = partes.path.strip("/").casefold().split("/")
        pagina = ruta[0] == PAGINA_EPS or (canal_eps and ruta[0].isascii() and ruta[0].isdecimal())
        if len(ruta) >= 3 and pagina and ruta[1] in ("posts", "photos", "videos", "reel", "permalink"):
            return url
        if ruta[0] in ("share", "reel") and len(ruta) >= 2:
            return url
        if ruta[0] in ("photo", "photo.php", "permalink.php", "story.php", "watch") and partes.query:
            return url
    return None


def _contenido_item(item):
    fotos = [n.get("url") for n in item.iter(f"{MEDIA}content")
             if n.get("medium") == "image" or (n.get("type") or "").startswith("image/") or (
                 not n.get("medium") and not n.get("type")
             )]
    fotos.extend(n.get("url") for n in item.findall("enclosure") if (n.get("type") or "").startswith("image/"))
    descripcion = ""
    for campo in (CONTENIDO, "description"):
        valor = item.findtext(campo, "").strip()
        if not valor:
            continue
        fragmento = html.fragment_fromstring(valor, create_parent="div")
        fotos.extend(fragmento.xpath(".//img/@src"))
        for nodo in fragmento.xpath(".//script | .//style"):
            nodo.drop_tree()
        for salto in fragmento.xpath(".//br | .//p | .//div"):
            salto.tail = "\n" + (salto.tail or "")
        texto = fragmento.text_content().strip()
        if len(texto) > len(descripcion):
            descripcion = texto
    urls = [foto for foto in fotos if foto and es_imagen_facebook(foto, de_rss=True)]
    if not urls:
        urls = [foto for n in item.iter(f"{MEDIA}thumbnail")
                if (foto := n.get("url")) and es_imagen_facebook(foto, de_rss=True)]
    return descripcion, list(dict.fromkeys(urls))


def publicaciones_rss(documento):
    parser = etree.XMLParser(resolve_entities=False, no_network=True, load_dtd=False)
    canal = etree.fromstring(documento, parser).find("channel")
    if canal is None:
        raise ValueError("El proveedor no devolvió un feed RSS válido.")

    enlace = urlsplit(canal.findtext("link", "").strip())
    pagina = enlace.path.strip("/").casefold().split("/")[0]
    es_facebook = enlace.hostname in ("facebook.com", "www.facebook.com", "m.facebook.com", "web.facebook.com")
    canal_eps = es_facebook and pagina == PAGINA_EPS
    if es_facebook and pagina not in (PAGINA_EPS, "share", "profile.php"):
        raise ValueError("El feed identifica otra página de Facebook. Genera uno desde facebook.com/EPSTacna.")

    rechazados = 0
    for item in canal.findall("item"):
        url, fecha = _enlace_publicacion(item, canal_eps), _fecha_item(item)
        if not url or not fecha:
            rechazados += 1
            continue
        texto, fotos = _contenido_item(item)
        if not texto:
            titulo = item.findtext("title", "").strip()
            if titulo.casefold() not in ("facebook", "eps tacna", "eps tacna s.a.", "photo", "foto"):
                texto = titulo
        yield {"message": texto, "created_time": fecha, "permalink_url": url, "imagenes_rss": fotos}
    if rechazados:
        raise ValueError(f"{rechazados} entradas del RSS no tenían una fecha o enlace de Facebook utilizable.")
