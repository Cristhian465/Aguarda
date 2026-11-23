import json
import re
from pathlib import Path

from .cortes import normalizar

CATALOGO = Path(__file__).resolve().parents[1] / "app/src/main/assets/distritos_cortes_tacna.txt"
DISTRITOS = tuple(CATALOGO.read_text(encoding="utf-8").splitlines())
CATALOGO_EPS = json.loads(CATALOGO.with_name("sectores_eps_tacna.json").read_text(encoding="utf-8"))
DISTRITOS_PROVINCIA = frozenset(CATALOGO_EPS["distritos"])
SECTORES_EPS = {sector["id"]: sector for sector in CATALOGO_EPS["sectores"]}
NOMBRES = {normalizar(distrito): distrito for distrito in DISTRITOS}
NOMBRES.update({
    "gregorio albarracin": "Coronel Gregorio Albarracín Lanchipa",
    "crnel gregorio albarracin lanchipa": "Coronel Gregorio Albarracín Lanchipa",
    "la yarada": "La Yarada-Los Palos",
    "las yaras": "Sama"
})
NOMBRES_EN_AVISO = re.compile(r"\b(?:" + "|".join(
    re.escape(nombre) for nombre in sorted(NOMBRES, key=len, reverse=True) if nombre != "tacna"
) + r")\b")


def distrito_oficial(distrito):
    return NOMBRES.get(normalizar(distrito))


def claves_distrito(distrito):
    nombre = distrito_oficial(distrito)
    return tuple(alias for alias, oficial in NOMBRES.items() if oficial == nombre)


def distritos_del_aviso(texto):
    limpio = normalizar(texto)
    limpio = re.sub(r"\bprovincia de (?:candarave|tarata)\b", "", limpio)
    distritos = [NOMBRES[m.group()] for m in NOMBRES_EN_AVISO.finditer(limpio)]
    if re.search(r"\bdistrito de tacna\b", limpio):
        distritos.append("Tacna")
    return list(dict.fromkeys(distritos)) or [None]


def normalizar_zona(texto):
    limpio = normalizar(texto)
    for abreviado, completo in (
        (r"\b(?:asoc|asociac de viv)\b", "asociacion"),
        (r"\burb\b", "urbanizacion"), (r"\bp j\b", "pueblo joven"),
        (r"\bav\b", "avenida"), (r"\bj v\b", "junta vecinal"),
        (r"\bsta\b", "santa"), (r"\bc p m\b", "cpm")
    ):
        limpio = re.sub(abreviado, completo, limpio)
    return limpio


def _referencias_sectoriales():
    referencias = {}
    for sector, datos in SECTORES_EPS.items():
        zonas = datos["zonas"] + ([datos["nombre"]] if sector in ("IV", "V", "VI") else [])
        for zona in zonas:
            referencias.setdefault(normalizar_zona(zona), set()).add(sector)
    return {zona: next(iter(sectores)) for zona, sectores in referencias.items() if len(sectores) == 1}


REFERENCIAS_SECTORIALES = _referencias_sectoriales()
CODIGOS = tuple(SECTORES_EPS)
ROMANO = r"(?:vii|vi|iv|iii|ii|i|v|0?[1-7])"
LISTA_SECTORES = re.compile(rf"\bsector(?:es)?\s+({ROMANO}\b(?:\s+(?:y\s+)?{ROMANO}\b){{0,6}})")
LISTA_SUBSECTORES = re.compile(r"\bsubsector(?:es)?\s+(\d{1,2}\b(?:\s+(?:y\s+)?\d{1,2}\b){0,15})")


def identificar_sectores(texto):
    limpio = normalizar_zona(texto)
    limpio_sin_subsectores = re.sub(r"\bsub sector\b", "subsector", limpio)
    indices = {indice for lista in LISTA_SECTORES.findall(limpio_sin_subsectores) for indice in lista.split() if indice != "y"}
    explicitos = {indice.upper() if not indice.isdecimal() else CODIGOS[int(indice) - 1] for indice in indices}
    for sector, datos in SECTORES_EPS.items():
        if re.search(rf"\bsector(?:es)? {re.escape(normalizar(datos['nombre']))}\b", limpio_sin_subsectores):
            explicitos.add(sector)
    subsectores = {int(numero) for lista in LISTA_SUBSECTORES.findall(limpio_sin_subsectores)
                  for numero in lista.split() if numero.isdecimal()}
    explicitos.update(sector for sector, datos in SECTORES_EPS.items() if subsectores.intersection(datos["subsectores"]))
    if explicitos:
        return sorted(explicitos, key=CODIGOS.index), "INDICADO"

    coincidencias = {sector for zona, sector in REFERENCIAS_SECTORIALES.items() if f" {zona} " in f" {limpio} "}
    return sorted(coincidencias, key=CODIGOS.index), "REFERENCIA" if coincidencias else None
