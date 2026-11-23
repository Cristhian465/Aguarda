import csv
import hashlib
import re
from datetime import date, datetime
from io import StringIO

from .cortes import HORA_PERU, Corte, normalizar

FUENTE_SUNASS = "https://servicio.sunass.gob.pe/camiyaku/"


def fecha_sunass(fecha, hora=None):
    if not fecha:
        return None
    fecha = fecha.strip()
    if re.fullmatch(r"\d{4}-\d{2}-\d{2}", fecha) and not hora:
        try:
            return date.fromisoformat(fecha).isoformat()
        except ValueError:
            return None
    texto = f"{fecha} {hora}" if hora else fecha
    try:
        valor = datetime.fromisoformat(texto)
    except ValueError:
        for formato in ("%d/%m/%Y %H:%M:%S", "%d/%m/%Y %H:%M", "%d/%m/%Y", "%Y-%m-%d %H:%M"):
            try:
                valor = datetime.strptime(texto, formato)
                if formato == "%d/%m/%Y":
                    return valor.date().isoformat()
                break
            except ValueError:
                continue
        else:
            return None
    valor = valor.replace(tzinfo=HORA_PERU) if valor.tzinfo is None else valor.astimezone(HORA_PERU)
    return valor.isoformat(timespec="minutes")


def _fila_tacna(fila):
    def leer(*nombres):
        for nombre in nombres:
            valor = fila.get(nombre)
            if valor and normalizar(valor) not in ("", "null", "none", "nan", "sin dato"):
                return " ".join(valor.split())
        return None

    region = normalizar(leer("departamento", "region", "des departamento", "nombre departamento"))
    eps = normalizar(leer("eps", "empresa prestadora", "nombre eps"))
    if region not in ("tacna", "23") and (region or "eps tacna" not in eps):
        return None

    servicio = normalizar(leer("servicio afectado", "tipo servicio", "tipo de servicio", "servicio"))
    if "agua potable" not in servicio and servicio != "agua":
        return None

    distrito = leer("distrito", "des distrito", "nombre distrito")
    sector = leer("sector", "sectores afectados", "zona afectada", "localidad", "subzona")
    inicio = fecha_sunass(
        leer("fecha inicio", "fecha hora inicio", "fecha de inicio", "fecha hora de inicio", "fecha inicio interrupcion"),
        leer("hora inicio", "hora de inicio")
    )
    publicado = fecha_sunass(leer("fecha registro", "fecha hora de registro preliminar", "fecha hora registro"))
    reposicion = fecha_sunass(
        leer("fecha estimada restablecimiento", "fecha estimada de restablecimiento", "fecha hora estimada de restablecimiento",
             "fecha hora estimada restablecimiento", "fecha programada restablecimiento"),
        leer("hora estimada restablecimiento", "hora estimada de restablecimiento")
    )
    restablecido = fecha_sunass(
        leer("fecha real restablecimiento", "fecha real de restablecimiento", "fecha hora real de restablecimiento",
             "fecha restablecimiento"),
        leer("hora real restablecimiento", "hora restablecimiento")
    )
    tipo = normalizar(leer("tipo interrupcion", "tipo de interrupcion", "tipo corte"))
    if "imprevist" in tipo or "no programad" in tipo:
        tipo = "IMPREVISTO"
    elif "programad" in tipo:
        tipo = "PROGRAMADO"
    else:
        tipo = "POR_CONFIRMAR"
    codigo = leer("cod incidente", "codigo incidente", "codigo interrupcion", "codigo de interrupcion", "codigo")
    motivo = leer("motivo", "clasificacion", "detalle motivo", "causa")
    id_registro = leer("id registro", "id interrupcion", "id")
    if not (inicio or publicado or reposicion or restablecido):
        return None
    clave = (codigo or id_registro or f"{inicio}|{publicado}|{motivo}", normalizar(distrito), normalizar(sector))
    identificador = hashlib.sha256("|".join(clave).encode()).hexdigest()[:24]
    return Corte(
        f"sunass-{identificador}", "SUNASS", FUENTE_SUNASS, "Interrupción del servicio de agua potable",
        tipo, distrito, sector, motivo, inicio, reposicion, restablecido, publicado
    )


def leer_csv_sunass(contenido: bytes):
    try:
        texto = contenido.decode("utf-8-sig")
    except UnicodeDecodeError:
        texto = contenido.decode("cp1252")
    cabecera = texto.splitlines()[0] if texto.strip() else ""
    delimitador = max((",", ";", "\t"), key=cabecera.count)
    lector = csv.DictReader(StringIO(texto), delimiter=delimitador)
    campos = {normalizar(columna) for columna in lector.fieldnames or []}
    campos_region = {"region", "departamento", "nombre departamento", "des departamento", "eps", "nombre eps", "empresa prestadora"}
    campos_servicio = {"servicio afectado", "tipo servicio", "tipo de servicio", "servicio"}
    if not campos.intersection(campos_region) or not campos.intersection(campos_servicio):
        raise ValueError("El CSV de SUNASS necesita columnas de región o EPS y de servicio afectado.")
    cortes = {}
    for fila in lector:
        corte = _fila_tacna({normalizar(k): v for k, v in fila.items() if k is not None})
        if corte is not None:
            cortes[corte.id] = corte
    return list(cortes.values())
