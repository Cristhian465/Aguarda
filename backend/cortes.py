import re
import sqlite3
import unicodedata
from contextlib import closing
from dataclasses import dataclass, fields
from datetime import datetime, timedelta, timezone
from pathlib import Path

HORA_PERU = timezone(timedelta(hours=-5))


def normalizar(texto):
    sin_tildes = unicodedata.normalize("NFKD", texto or "")
    sin_tildes = "".join(c for c in sin_tildes if not unicodedata.combining(c))
    return " ".join(re.findall(r"[a-z0-9]+", sin_tildes.lower()))


@dataclass(frozen=True)
class Corte:
    id: str
    fuente: str
    url: str
    titulo: str
    tipo: str
    distrito: str | None = None
    sector: str | None = None
    motivo: str | None = None
    inicio: str | None = None
    reposicion_estimada: str | None = None
    restablecido: str | None = None
    publicado: str | None = None
    categoria: str | None = None
    detalle: str | None = None


CAMPOS = tuple(campo.name for campo in fields(Corte))
TABLA = """
CREATE TABLE IF NOT EXISTS cortes (
    id TEXT PRIMARY KEY, fuente TEXT NOT NULL, url TEXT NOT NULL,
    titulo TEXT NOT NULL, tipo TEXT NOT NULL, distrito TEXT, sector TEXT,
    motivo TEXT, inicio TEXT, reposicion_estimada TEXT, restablecido TEXT,
    publicado TEXT, categoria TEXT, detalle TEXT, distrito_busqueda TEXT NOT NULL
)
"""


class BaseCortes:
    def __init__(self, ruta: Path):
        self.ruta = ruta

    def _conectar(self):
        self.ruta.parent.mkdir(parents=True, exist_ok=True)
        conexion = sqlite3.connect(self.ruta, timeout=15)
        try:
            conexion.row_factory = sqlite3.Row
            conexion.execute(TABLA)
            conexion.execute("CREATE INDEX IF NOT EXISTS cortes_distrito ON cortes(distrito_busqueda)")
            nuevos = {"categoria", "detalle"}
            if nuevos.difference(fila[1] for fila in conexion.execute("PRAGMA table_info(cortes)")):
                conexion.execute("BEGIN IMMEDIATE")
                existentes = {fila[1] for fila in conexion.execute("PRAGMA table_info(cortes)")}
                for campo in sorted(nuevos - existentes):
                    conexion.execute(f"ALTER TABLE cortes ADD COLUMN {campo} TEXT")
                conexion.commit()
            return conexion
        except BaseException:
            conexion.close()
            raise

    def guardar(self, cortes, reemplazar_articulo=None):
        columnas = ", ".join((*CAMPOS, "distrito_busqueda"))
        valores = ", ".join("?" for _ in range(len(CAMPOS) + 1))
        cambios = ", ".join(f"{c}=excluded.{c}" for c in (*CAMPOS[1:], "distrito_busqueda"))
        filas = [tuple(getattr(corte, c) for c in CAMPOS) + (normalizar(corte.distrito),) for corte in cortes]
        with closing(self._conectar()) as conexion, conexion:
            if reemplazar_articulo is not None:
                conexion.execute("DELETE FROM cortes WHERE url = ?", (reemplazar_articulo,))
            conexion.executemany(
                f"INSERT INTO cortes ({columnas}) VALUES ({valores}) ON CONFLICT(id) DO UPDATE SET {cambios}",
                filas
            )
        return len(filas)

    def listar(self, distrito, historial, limite, ahora=None, *, claves=(), notificaciones=False, segundos=None):
        hoy = (ahora or datetime.now(HORA_PERU)).date()
        filtros, parametros = [], []
        fecha_publicacion = "COALESCE(publicado, inicio, reposicion_estimada, restablecido)"
        if notificaciones:
            if segundos is not None:
                actual = ahora or datetime.now(HORA_PERU)
                fecha_sql = f"julianday(CASE WHEN length({fecha_publicacion}) = 10 THEN {fecha_publicacion} || 'T00:00:00-05:00' ELSE {fecha_publicacion} END)"
                filtros.append(f"{fecha_sql} BETWEEN julianday(?) AND julianday(?)")
                parametros.extend(((actual - timedelta(seconds=segundos)).isoformat(), actual.isoformat()))
        else:
            filtros.append("(categoria IS NULL OR categoria = 'CORTE')")
        if distrito:
            claves = claves or (normalizar(distrito),)
            filtros.append("distrito_busqueda IN (" + ", ".join("?" for _ in claves) + ")")
            parametros.extend(claves)
        if not historial:
            filtros.append(
                "(COALESCE(inicio, publicado, '') >= ? OR reposicion_estimada >= ? OR restablecido >= ?) "
                "AND (restablecido IS NULL OR restablecido >= ?)"
            )
            desde = (hoy - timedelta(days=7)).isoformat()
            parametros.extend((desde, hoy.isoformat(), desde, desde))
        condiciones = " WHERE " + " AND ".join(filtros) if filtros else ""
        orden = fecha_publicacion if notificaciones else "COALESCE(inicio, publicado, '')"
        with closing(self._conectar()) as conexion:
            filas = conexion.execute(
                f"SELECT {', '.join(CAMPOS)} FROM cortes{condiciones} "
                f"ORDER BY {orden} DESC, id LIMIT ?",
                (*parametros, limite)
            ).fetchall()
        return [Corte(**dict(fila)) for fila in filas]

