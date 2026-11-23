import re
from dataclasses import asdict, dataclass, field
from datetime import datetime
from typing import Literal

from .cortes import BaseCortes, Corte
from .zonas import DISTRITOS_PROVINCIA, distrito_oficial, identificar_sectores, normalizar_zona

Categoria = Literal["CORTE", "MANTENIMIENTO", "REPARACION", "ABASTECIMIENTO", "ATENCION", "COMUNICADO"]
Periodo = Literal["24h", "7d", "30d", "365d", "todos"]
SEGUNDOS = {"24h": 86400, "7d": 7 * 86400, "30d": 30 * 86400, "365d": 365 * 86400, "todos": None}

PATRONES = {
    "CORTE": r"\b(?:cortes?|interrupci(?:on|ones)|suspension|restriccion)\s+(?:(?:temporal|imprevist[oa]|programad[oa]|no|del|de|el|servicio|suministro)\s+){0,6}(?:agua|abastecimiento)\b|\bcorte\s+(?:imprevisto|programado|por rotura)\b|\bfalta de agua\b",
    "MANTENIMIENTO": r"\b(?:mantenimiento|lavado|limpieza|desinfeccion)\b",
    "REPARACION": r"\b(?:reparaci(?:on|ones)|roturas?|averias?|fugas?|atoros?)\b",
    "ABASTECIMIENTO": r"\b(?:abastecimiento|distribucion de agua|cisternas?|baja presion|restablecimiento|restablece|racionamiento)\b",
    "ATENCION": r"\b(?:atencion|recibos?|facturacion|reclamos?|oficina virtual|call center|centro de llamadas|canales telefonicos|pagos?)\b",
}
OTRA_PROVINCIA = re.compile(r"\bprovincia\s+(?:de\s+)?(?:candarave|jorge basadre|tarata)\b")


def categorias_del_aviso(texto):
    limpio = normalizar_zona(texto)
    return [categoria for categoria, patron in PATRONES.items() if re.search(patron, limpio)]


def clasificar(titulo, texto):
    titulos = categorias_del_aviso(titulo)
    cuerpo = categorias_del_aviso(texto)
    if "CORTE" in cuerpo and re.search(r"\b(?:cortes?|suspension|interrupcion)\b", normalizar_zona(titulo)):
        return "CORTE"
    return next(iter(titulos or cuerpo), "COMUNICADO")


@dataclass(frozen=True)
class Aviso(Corte):
    distritos: list[str] = field(default_factory=list)
    sectores: list[str] = field(default_factory=list)
    sector_origen: str | None = None


def consultar_avisos(base: BaseCortes, categoria, periodo, sector, buscar, provincia, limite, desde, ahora: datetime | None = None):
    registros = base.listar(None, True, -1, ahora, notificaciones=True, segundos=SEGUNDOS[periodo])
    grupos = {}
    for fila in registros:
        clave = (fila.fuente, fila.id if fila.fuente == "SUNASS" else fila.url)
        grupos.setdefault(clave, []).append(fila)

    avisos = []
    palabras = set(normalizar_zona(buscar).split())
    for filas in grupos.values():
        principal = filas[0]
        distritos = list(dict.fromkeys(d for fila in filas if (d := distrito_oficial(fila.distrito))))
        texto = " ".join(filter(None, (principal.titulo, principal.sector, principal.motivo, principal.detalle)))
        if provincia and distritos:
            distritos = [d for d in distritos if d in DISTRITOS_PROVINCIA]
            if not distritos:
                continue
        elif provincia and OTRA_PROVINCIA.search(normalizar_zona(texto)):
            continue
        tipo = principal.categoria or "CORTE"
        if categoria and categoria != tipo and categoria not in categorias_del_aviso(texto):
            continue
        if palabras and not palabras.issubset(normalizar_zona(" ".join([texto, *distritos, principal.fuente])).split()):
            continue
        sectores, origen = identificar_sectores(texto)
        if sector and sector not in sectores:
            continue
        datos = asdict(principal)
        datos.update(categoria=tipo, distrito=distritos[0] if len(distritos) == 1 else None)
        avisos.append(Aviso(**datos, distritos=distritos, sectores=sectores, sector_origen=origen))
        if len(avisos) == desde + limite:
            break
    return avisos[desde:]
