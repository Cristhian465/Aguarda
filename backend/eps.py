import hashlib
import re
from datetime import date, datetime, time, timedelta
from urllib.parse import urljoin, urlsplit, urlunsplit

from lxml import html

from .avisos import clasificar
from .cortes import HORA_PERU, Corte, normalizar
from .sunass import fecha_sunass
from .zonas import distritos_del_aviso

LISTADO_EPS = "https://www.gob.pe/institucion/epstacna/noticias"
LISTADO_SUNASS = "https://www.gob.pe/institucion/sunass/noticias?filter%5Bterms%5D=Tacna"
ARTICULO = re.compile(r"/institucion/(epstacna|sunass)/noticias/(\d+)(?:-[^/]+)?/?")
MESES = "enero febrero marzo abril mayo junio julio agosto septiembre octubre noviembre diciembre".split()
FECHA = re.compile(r"\b(\d{1,2}) de (" + "|".join(MESES) + r") de (\d{4})\b", re.I)
HORA = r"\d{1,2}(?::\d{2})?\s*(?:[ap]\s*\.?\s*m\.?\s*|h(?:oras)?\b)?"
RANGO = re.compile(rf"\b(?:de|desde las|horario\s*:?)\s+({HORA})\s*(?:a|hasta(?: las)?|[-–—])\s*({HORA})(?=\W|$)", re.I)
FECHA_NUMERICA = re.compile(r"\b(\d{1,2})\s*[/.-]\s*(\d{1,2})\s*[/.-]\s*(20\d{2})\b")


def url_aviso(url):
    datos = urlsplit(url)
    if datos.scheme != "https" or datos.netloc not in ("gob.pe", "www.gob.pe"):
        return None
    if not ARTICULO.fullmatch(datos.path):
        return None
    return urlunsplit(("https", "www.gob.pe", datos.path.rstrip("/"), "", ""))


def enlaces_eps(documento, listado=LISTADO_EPS):
    pagina = html.fromstring(documento)
    institucion = urlsplit(listado).path.split("/")[2]
    enlaces = (url_aviso(urljoin(listado, ruta)) for ruta in pagina.xpath("//a/@href"))
    return list(dict.fromkeys(enlace for enlace in enlaces if enlace and f"/institucion/{institucion}/" in enlace))


def texto_eps(documento):
    pagina = html.fromstring(documento)
    titulo = pagina.xpath("string((//h1)[1])").strip()
    publicado = pagina.xpath(
        "(//meta[@property='article:published_time']/@content | //main//time/@datetime | //article//time/@datetime)[1]"
    )
    cuerpo = pagina.xpath("//main//article | //article")
    if not cuerpo:
        cuerpo = pagina.xpath("//main")
    if not titulo or not cuerpo:
        raise ValueError("La página de EPS no contiene un comunicado legible.")
    partes = cuerpo[0].xpath(".//p[not(ancestor::aside)] | .//li[not(ancestor::nav) and not(ancestor::aside)]")
    texto = "\n".join(" ".join(parte.text_content().split()) for parte in partes)
    if not texto.strip():
        raise ValueError("El comunicado no tiene texto legible. Puedes usar OCR y revisar el resultado.")
    fecha = fecha_sunass(publicado[0]) if publicado else None
    if fecha is None:
        principal = (pagina.xpath("//main") or cuerpo)[0]
        encabezado = " ".join(principal.xpath(".//text()[not(ancestor::p) and not(ancestor::script)]"))
        fecha_escrita = re.search(rf"({FECHA.pattern})\s*[-–]\s*({HORA})", encabezado, re.I)
        if fecha_escrita and (dia := _fecha_texto(fecha_escrita[0])):
            hora = _hora(fecha_escrita.groups()[-1])
            fecha = _momento(dia, hora) if hora else dia.isoformat()
    return titulo, texto, fecha


def _fecha_texto(texto):
    fecha = FECHA.search(texto)
    if fecha is not None:
        dia, mes, anio = fecha.groups()
        try:
            return date(int(anio), MESES.index(mes.lower()) + 1, int(dia))
        except ValueError:
            return None
    fecha_numerica = FECHA_NUMERICA.search(texto)
    if fecha_numerica is not None:
        dia, mes, anio = map(int, fecha_numerica.groups())
        try:
            return date(anio, mes, dia)
        except ValueError:
            return None
    return None


def _hora(texto, meridiano_por_contexto=None):
    m = re.fullmatch(r"(\d{1,2})(?::(\d{2}))?\s*([ap])?\s*m?", normalizar_hora(texto))
    if m is None:
        return None
    horas, minutos, meridiano = m.groups()
    formato_24h = re.search(r"h(?:oras)?\b", texto, re.I)
    if not meridiano and not formato_24h and int(horas) <= 12:
        meridiano = meridiano_por_contexto
    if minutos is None and not meridiano and not formato_24h:
        return None
    horas = int(horas)
    if meridiano:
        if not 1 <= horas <= 12:
            return None
        horas = horas % 12 + (12 if meridiano == "p" else 0)
    try:
        return time(horas, int(minutos or 0))
    except ValueError:
        return None


def normalizar_hora(texto):
    return re.sub(r"\s*h(?:oras)?\b", "", texto.lower()).replace(".", "").strip().replace(" ", "")


def _momento(dia, hora):
    return datetime.combine(dia, hora, tzinfo=HORA_PERU).isoformat(timespec="minutes")


def _horarios(texto, dia):
    if not dia:
        return None, None, None
    inicio, reposicion, restablecido = dia.isoformat(), None, None
    for rango in RANGO.finditer(texto):
        primera, segunda = rango.groups()
        meridiano = re.search(r"([ap])m$", normalizar_hora(segunda))
        desde = _hora(primera, meridiano.group(1) if meridiano else None)
        hasta = _hora(segunda)
        if desde is not None and hasta is not None:
            if desde > hasta and meridiano and meridiano.group(1) == "p":
                desde = _hora(primera, "a") or desde
            inicio = _momento(dia, desde)
            dia_fin = dia + timedelta(days=1) if hasta < desde else dia
            reposicion = _momento(dia_fin, hasta)
            break
    if reposicion is None:
        desde = re.search(rf"\b(?:a partir de|desde) las\s+({HORA})", texto, re.I)
        hasta = re.search(rf"\b(?:reposici[oó]n|restablecimiento)\b[^.\n]{{0,80}}?\ba las\s+({HORA})", texto, re.I)
        if desde and (hora := _hora(desde.group(1))) is not None:
            inicio = _momento(dia, hora)
        if hasta and (hora := _hora(hasta.group(1))) is not None:
            reposicion = _momento(dia, hora)
    cierre = re.search(
        rf"\b(?:logr[óo](?:\s+restablecer)|logrando\s+restablecer|se\s+restableci[óo]|qued[óo]\s+restablecido)"
        rf"\s+(?:el\s+servicio\s+)?a\s+las\s+({HORA})", texto, re.I
    )
    if cierre and (hora := _hora(cierre.group(1))) is not None:
        restablecido = _momento(dia, hora)
    return inicio, reposicion, restablecido


def _sector(texto):
    coincidencia = re.search(
        r"(?:sectores afectados\s*[:\-]|en (?:los )?sectores de|en (?:la )?asociaci[oó]n|en (?:la )?asoc\.?|"
        r"(?:zonas|calles) afectadas\s*[:\-]|en (?:la )?urbanizaci[oó]n)\s*([^\n;]+)", texto, re.I
    )
    if coincidencia is None:
        coincidencia = re.search(r"\ben\s+(?:(?:la|el)\s+)?((?:av(?:\.|enida)|calle|jr\.?|jir[oó]n)\s+[^\n;]+)", texto, re.I)
    if coincidencia is None:
        return None
    fin = r"(?<!(?i:\bav))(?<!(?i:\bmz))(?<!\b[A-Z])\.\s+(?=[A-ZÁÉÍÓÚ])|\.\s*$|,\s*(?:debido|incidente)"
    sector = re.split(fin, coincidencia.group(1))[0]
    if len(sector) > 160:
        return None
    return sector.strip(" .,") or None


def extraer_aviso_eps(url, titulo, texto, publicado=None):
    url = url_aviso(url)
    if url is None:
        raise ValueError("Usa la URL oficial del comunicado de EPS Tacna en gob.pe.")
    institucion, codigo = ARTICULO.fullmatch(urlsplit(url).path).groups()
    if institucion == "sunass":
        return extraer_comunicado(f"sunass-noticia-{codigo}", "SUNASS · Noticias", url, titulo, texto, publicado, exige_contexto=True)
    return extraer_comunicado(f"eps-{codigo}", "EPS Tacna", url, titulo, texto, publicado)


def extraer_comunicado(id_base, fuente, url, titulo, texto, publicado=None, *, exige_contexto=False):
    contenido = f"{titulo}\n{texto}"
    limpio = normalizar(contenido)
    categoria = clasificar(titulo, texto)
    if exige_contexto and not (re.search(r"\btacna\b", limpio) and categoria != "COMUNICADO"):
        return []
    if re.search(r"\b(corte\s+(?:temporal\s+)?no programado|imprevist[oa])\b", limpio):
        tipo = "IMPREVISTO"
    elif re.search(r"\b(?:corte|interrupcion|suspension) programad[oa]\b", limpio):
        tipo = "PROGRAMADO"
    elif re.search(r"\brotura de (?:una )?tuberia\b", limpio):
        tipo = "IMPREVISTO"
    else:
        tipo = "POR_CONFIRMAR"
    # La fecha del aviso y la del corte son distintas. Solo se usa la primera si el texto dice «hoy» o «mañana».
    dia = _fecha_texto(texto)
    if dia is None and publicado:
        referencia = re.search(r"\b(hoy|mañana)\b", texto, re.I)
        if referencia:
            dia = date.fromisoformat(publicado[:10])
            if referencia.group(1).lower() == "mañana":
                dia += timedelta(days=1)
    inicio, reposicion, restablecido = _horarios(texto, dia) if categoria in {"CORTE", "MANTENIMIENTO", "REPARACION"} else (None, None, None)
    distritos = distritos_del_aviso(contenido)
    sector = _sector(texto) if len(distritos) == 1 else None
    motivo = next((valor for patron, valor in (
        (r"\brotura de (?:una )?tuberia\b", "Rotura de tubería"),
        (r"\breparacion de (?:una )?tuberia\b", "Reparación de tubería"),
        (r"\bmantenimiento\b", "Mantenimiento"),
        (r"\bcorte de energia\b", "Corte de energía")
    ) if re.search(patron, limpio)), None)
    return [
        Corte(
            f"{id_base}-{hashlib.sha256(normalizar(distrito).encode()).hexdigest()[:12]}",
            fuente, url, titulo, tipo, distrito, sector, motivo, inicio, reposicion, restablecido, publicado,
            categoria, " ".join(texto.split())[:1800] or None
        )
        for distrito in distritos
    ]
