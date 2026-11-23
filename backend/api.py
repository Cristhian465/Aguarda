import os
from dataclasses import replace
from pathlib import Path
from typing import Annotated, Literal

from fastapi import FastAPI, HTTPException, Query

from .avisos import Aviso, Categoria, Periodo, consultar_avisos
from .cortes import BaseCortes, Corte
from .zonas import CATALOGO_EPS, DISTRITOS, claves_distrito, distrito_oficial

RUTA_BASE = Path(__file__).parent / "data" / "cortes.sqlite3"


def base_configurada():
    return BaseCortes(Path(os.environ.get("CORTES_DB", RUTA_BASE)))


def crear_app(base: BaseCortes):
    api = FastAPI(title="Aguarda · Avisos de agua y saneamiento", version="1.1")

    @api.get("/avisos")
    def listar_avisos(
        categoria: Categoria | None = None,
        periodo: Periodo = "7d",
        sector: Literal["I", "II", "III", "IV", "V", "VI", "VII"] | None = None,
        buscar: Annotated[str | None, Query(max_length=120)] = None,
        ambito: Literal["departamento", "provincia-tacna"] = "departamento",
        limite: Annotated[int, Query(ge=1, le=100)] = 50,
        desde: Annotated[int, Query(ge=0)] = 0
    ) -> list[Aviso]:
        return consultar_avisos(base, categoria, periodo, sector, buscar, ambito == "provincia-tacna", limite, desde)

    @api.get("/sectores")
    def listar_sectores():
        return CATALOGO_EPS

    @api.get("/cortes")
    def listar_cortes(
        distrito: Annotated[str | None, Query(max_length=120)] = None,
        historial: bool = False,
        limite: Annotated[int, Query(ge=1, le=100)] = 50
    ) -> list[Corte]:
        claves = claves_distrito(distrito) if distrito else ()
        if distrito and not claves:
            raise HTTPException(status_code=422, detail="Selecciona un distrito del departamento de Tacna.")
        avisos = base.listar(distrito, historial, limite, claves=claves)
        return [replace(aviso, distrito=distrito_oficial(aviso.distrito)) for aviso in avisos]

    @api.get("/distritos")
    def listar_distritos() -> list[str]:
        return list(DISTRITOS)

    return api


app = crear_app(base_configurada())
