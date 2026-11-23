import json
import os
import sqlite3
import unittest
from argparse import Namespace
from contextlib import closing
from datetime import datetime
from pathlib import Path
from tempfile import TemporaryDirectory
from unittest.mock import MagicMock, patch

from fastapi import HTTPException

from .api import crear_app
from .avisos import consultar_avisos
from .cortes import HORA_PERU, TABLA, BaseCortes
from .eps import enlaces_eps, texto_eps, extraer_aviso_eps
from .facebook_ocr import imagenes_publicacion, leer_imagen_facebook
from .facebook_rss import publicaciones_rss, url_rss_facebook
from .fuentes import aviso_facebook, leer_rss_radio_uno, publicaciones_graph
from .importar import guardar_aviso, importar_facebook, sincronizar
from .sunass import leer_csv_sunass
from .zonas import identificar_sectores

CSV = """DEPARTAMENTO;DISTRITO;SECTOR;SERVICIO_AFECTADO;TIPO_INTERRUPCION;COD_INCIDENTE;FECHA_INICIO;HORA_INICIO;FECHA_ESTIMADA_RESTABLECIMIENTO;MOTIVO
TACNA;Ciudad Nueva;Sector A;AGUA POTABLE;PROGRAMADA;A1;22/11/2026;08:30;;Mantenimiento
TACNA;Ciudad Nueva;Sector B;AGUA POTABLE;PROGRAMADA;A1;22/11/2026;08:30;2026-11-23;Mantenimiento
TACNA;Pocollay;;AGUA POTABLE;IMPREVISTA;A2;2025-01-03;;;Rotura
TACNA;Ciudad Nueva;;ALCANTARILLADO;IMPREVISTA;A3;2026-11-22;;;Atoro
LIMA;Lima;;AGUA POTABLE;PROGRAMADA;A4;2026-11-22;;;Reparación
"""


class CortesTest(unittest.TestCase):
    def setUp(self):
        temporal = TemporaryDirectory()
        self.addCleanup(temporal.cleanup)
        self.base = BaseCortes(Path(temporal.name) / "prueba.db")

    def test_csv_filtra_agua_potable_tacna_y_conserva_subzonas(self):
        avisos = leer_csv_sunass(CSV.encode("cp1252"))
        self.base.guardar(avisos)
        self.base.guardar(avisos)
        self.assertEqual(len(self.base.listar(None, True, 50)), 3)
        ciudad = self.base.listar(" CIUDAD  NUEVA ", True, 50)
        self.assertEqual({aviso.sector for aviso in ciudad}, {"Sector A", "Sector B"})
        self.assertEqual(ciudad[0].inicio, "2026-11-22T08:30-05:00")
        self.assertIn(None, [aviso.reposicion_estimada for aviso in ciudad])
        ahora = datetime(2026, 11, 22, tzinfo=HORA_PERU)
        self.assertEqual(len(self.base.listar(None, False, 50, ahora)), 2)

    def test_rechaza_error_html_pasado_como_csv(self):
        with self.assertRaises(ValueError):
            leer_csv_sunass(b"<html>Servicio no disponible</html>")

    def test_las_americas_y_los_cedros_se_importan_con_fuente(self):
        casos = json.loads(Path(__file__).with_name("ejemplos_eps.json").read_text(encoding="utf-8"))
        for aviso in casos:
            guardar_aviso(self.base, **aviso)
        americas, cedros = self.base.listar(None, True, 50)
        self.assertEqual(americas.tipo, "IMPREVISTO")
        self.assertEqual(americas.inicio, "2025-06-05T12:15-05:00")
        self.assertEqual(americas.reposicion_estimada, "2025-06-05T15:00-05:00")
        self.assertEqual(americas.restablecido, "2025-06-05T14:40-05:00")
        self.assertIn("1198584-", americas.url)
        self.assertIsNone(cedros.inicio)
        self.assertIsNone(cedros.reposicion_estimada)
        self.assertIsNone(cedros.distrito)
        self.assertTrue(cedros.sector.endswith("Los Cedros"))
        self.assertEqual(self.base.listar("Tacna", True, 50), [])

    def test_html_y_cambio_del_horario_sin_duplicar_el_comunicado(self):
        listado = b"""
            <a href="/institucion/epstacna/noticias/100-aviso?tag=1">Aviso</a>
            <a href="/institucion/epstacna/noticias/100-aviso">Duplicado</a>
            <a href="https://otro.pe/institucion/epstacna/noticias/100">Otro sitio</a>
        """
        urls = enlaces_eps(listado)
        self.assertEqual(urls, ["https://www.gob.pe/institucion/epstacna/noticias/100-aviso"])
        pagina = """
            <main><h1>Corte programado de agua potable en Ciudad Nueva</h1>
            <time datetime="2026-11-21T09:30:00-05:00"></time><article>
            <p>Mañana habrá un corte programado de 8:00 a 12 p. m.</p>
            <p>Sectores afectados: Sector A.</p></article></main>
        """
        titulo, cuerpo, publicado = texto_eps(pagina)
        guardar_aviso(self.base, urls[0], titulo, cuerpo, publicado)
        guardar_aviso(self.base, urls[0], titulo, cuerpo.replace("12 p.", "3 p."), publicado)
        aviso, = self.base.listar("Ciudad Nueva", True, 50)
        self.assertEqual(aviso.inicio, "2026-11-22T08:00-05:00")
        self.assertEqual(aviso.reposicion_estimada, "2026-11-22T15:00-05:00")
        self.assertEqual(aviso.sector, "Sector A")
        aviso_general, = extraer_aviso_eps(urls[0], "Otro aviso", "Convocatoria para prácticas")
        self.assertEqual(aviso_general.categoria, "COMUNICADO")

    def test_contrato_publico(self):
        app = crear_app(self.base)
        self.assertEqual(set(app.openapi()["paths"]), {"/cortes", "/distritos", "/avisos", "/sectores"})
        listar = next(r.endpoint for r in app.routes if r.path == "/cortes")
        distritos = next(r.endpoint for r in app.routes if r.path == "/distritos")()
        self.assertEqual(len(set(distritos)), 28)
        self.assertIn("Estique Pampa", distritos)
        self.assertIn("La Yarada-Los Palos", distritos)
        csv_alias = CSV.replace("Ciudad Nueva", "Gregorio Albarracín")
        self.base.guardar(leer_csv_sunass(csv_alias.encode()))
        self.assertEqual(len(listar(distrito="Pocollay", historial=True)), 1)
        avisos = listar(distrito="Coronel Gregorio Albarracín Lanchipa", historial=True)
        self.assertEqual(len(avisos), 2)
        self.assertTrue(all(aviso.distrito == "Coronel Gregorio Albarracín Lanchipa" for aviso in avisos))
        with self.assertRaises(HTTPException) as rechazo:
            listar(distrito="Miraflores", historial=True)
        self.assertEqual(rechazo.exception.status_code, 422)

    def test_eps_reconoce_estique_pampa_sin_atribuir_aviso_a_estique(self):
        avisos = extraer_aviso_eps(
            "https://www.gob.pe/institucion/epstacna/noticias/100-aviso",
            "Corte programado de agua potable en Estique Pampa",
            "El distrito de Estique Pampa, provincia de Tarata, tendrá un corte el 22 de noviembre de 2026."
        )
        self.assertEqual([aviso.distrito for aviso in avisos], ["Estique Pampa"])

    def test_migra_base_existente_y_agrupa_distritos_de_la_misma_publicacion(self):
        with closing(sqlite3.connect(self.base.ruta)) as conexion, conexion:
            conexion.execute(TABLA.replace("categoria TEXT, detalle TEXT, ", ""))
            conexion.execute("INSERT INTO cortes (id, fuente, url, titulo, tipo, distrito_busqueda) VALUES (?, ?, ?, ?, ?, ?)",
                             ("anterior", "SUNASS", "https://sunass.gob.pe/", "Interrupción", "PROGRAMADO", ""))
        guardar_aviso(self.base, "https://www.gob.pe/institucion/epstacna/noticias/200-aviso",
                      "Corte de agua potable", "Sectores I y IV, distritos de Ciudad Nueva y Alto de la Alianza.", "2026-11-22")
        avisos = consultar_avisos(self.base, None, "todos", "IV", None, True, 50, 0)
        aviso, = avisos
        self.assertEqual(set(aviso.distritos), {"Ciudad Nueva", "Alto de la Alianza"})
        self.assertEqual(aviso.sectores, ["I", "IV"])
        self.assertEqual(aviso.sector_origen, "INDICADO")
        self.assertEqual(len(self.base.listar(None, True, 50)), 3)

    def test_filtros_sector_tipo_fecha_y_busqueda_por_calle(self):
        post = {"permalink_url": "https://www.facebook.com/eps/posts/123", "created_time": "2026-11-22T17:00:00+0000",
                "message": "Mantenimiento de la red\nTrabajos en Av. Cacique Rosa Ara y calle Lima."}
        self.assertEqual(importar_facebook(self.base, [post, {"message": ""}]), 1)
        self.assertEqual(importar_facebook(self.base, [post]), 1)
        guardar_aviso(self.base, "https://www.gob.pe/institucion/epstacna/noticias/201-aviso",
                      "Mantenimiento del reservorio de Ticaco", "Trabajos de limpieza.", "2026-11-22T09:00:00-05:00")
        guardar_aviso(self.base, "https://www.gob.pe/institucion/epstacna/noticias/202-aviso",
                      "Mantenimiento en Pocollay", "Trabajos en Las Peañas.", "2026-11-20T09:00:00-05:00")
        ahora = datetime(2026, 11, 22, 17, tzinfo=HORA_PERU)
        encontrados = consultar_avisos(self.base, "MANTENIMIENTO", "24h", "V", "avenida Rosa Ara", True, 50, 0, ahora)
        aviso, = encontrados
        self.assertEqual(aviso.fuente, "EPS Tacna · Facebook")
        self.assertEqual(aviso.sector_origen, "REFERENCIA")
        self.assertEqual(aviso.distritos, [])
        self.assertIn("Cacique Rosa Ara", aviso.sector)
        self.assertEqual(len(consultar_avisos(self.base, "MANTENIMIENTO", "7d", None, None, True, 50, 0, ahora)), 2)
        self.assertEqual(len(consultar_avisos(self.base, "MANTENIMIENTO", "7d", None, None, False, 50, 0, ahora)), 3)
        self.assertFalse(consultar_avisos(self.base, "CORTE", "todos", None, None, True, 50, 0))
        self.assertEqual(identificar_sectores("Asociación San Pedro"), ([], None))
        self.assertEqual(identificar_sectores("El 28 de agosto se publicará el horario"), ([], None))
        self.assertEqual(identificar_sectores("Asociación 28 de Agosto"), (["I"], "REFERENCIA"))
        self.assertEqual(identificar_sectores("Subsectores 05, 17 y 25"), (["II", "IV", "VII"], "INDICADO"))

    def test_rss_restringe_fuente_y_avisos_de_eps_tacna(self):
        rss = b"""<rss><channel>
          <item><title>Corte de agua potable</title><link>https://radiouno.pe/noticias/1234/nota/</link>
          <description>EPS Tacna informa de un corte en Las Americas.</description>
          <pubDate>Sun, 22 Nov 2026 07:00:00 GMT</pubDate></item>
          <item><title>Mantenimiento de parques</title><link>https://radiouno.pe/noticias/1235/nota/</link>
          <description>Municipalidad de Tacna</description></item></channel></rss>"""
        avisos, = leer_rss_radio_uno(rss)
        self.assertEqual(avisos[0].fuente, "Radio Uno")
        self.assertEqual(avisos[0].publicado, "2026-11-22T02:00-05:00")
        with self.assertRaises(ValueError):
            aviso_facebook("https://facebook.com.example.com/post/123", "Comunicado", "2026-11-22")

    def test_graph_solicita_solo_publicaciones_y_mantiene_token_en_cabecera(self):
        cliente = MagicMock()
        respuesta = cliente.open.return_value.__enter__.return_value
        respuesta.read.side_effect = [
            b'{"data": [{"id": "123_1"}], "paging": {"next": "https://example.invalid/", "cursors": {"after": "mas"}}}',
            b'{"data": [{"id": "123_2"}]}'
        ]
        entorno = {"EPS_FACEBOOK_PAGE_ID": "123", "EPS_FACEBOOK_TOKEN": "prueba", "EPS_FACEBOOK_GRAPH_VERSION": "v27.0"}
        with patch.dict(os.environ, entorno), patch("backend.fuentes.build_opener", return_value=cliente):
            self.assertEqual([post["id"] for post in publicaciones_graph()], ["123_1", "123_2"])
        solicitudes = [llamada.args[0] for llamada in cliente.open.call_args_list]
        self.assertTrue(all(p.full_url.startswith("https://graph.facebook.com/v27.0/123/published_posts?") for p in solicitudes))
        self.assertTrue(all("prueba" not in p.full_url and p.get_header("Authorization") == "Bearer prueba" for p in solicitudes))
        self.assertIn("after=mas", solicitudes[1].full_url)
        self.assertIn("attachments", solicitudes[0].full_url)

    def test_publicacion_solo_imagen_importa_corte_con_sector_hora_y_fecha(self):
        url_a = "https://scontent.example.fbcdn.net/aviso-a.jpg?firma=nueva"
        url_b = "https://scontent.example.fbcdn.net/aviso-b.jpg"
        adjuntos = [{"media_type": "photo", "media": {"image": {"src": url}}} for url in (url_a, url_b)]
        post = {"permalink_url": "https://www.facebook.com/eps/posts/321", "created_time": "2026-11-22T17:00:00+0000",
                "attachments": {"data": [{"media_type": "album", "subattachments": {"data": adjuntos}}]}}
        self.assertEqual(imagenes_publicacion(post), [url_a, url_b])
        with patch("backend.importar.leer_imagen_facebook", side_effect=[
            "Corte programado de agua potable\nSector IV\n23/11/2026\nHORARIO: 06:00 - 12:00",
            "Zonas afectadas: Asociación La Florida."
        ]):
            self.assertEqual(importar_facebook(self.base, [post], con_ocr=True), 1)
        aviso, = consultar_avisos(self.base, "CORTE", "todos", "IV", "la florida", True, 50, 0)
        self.assertEqual(aviso.inicio, "2026-11-23T06:00-05:00")
        self.assertEqual(aviso.reposicion_estimada, "2026-11-23T12:00-05:00")
        self.assertIn("OCR", aviso.detalle)
        with patch("backend.importar.leer_imagen_facebook", side_effect=OSError("imagen no disponible")):
            with self.assertRaises(ValueError):
                importar_facebook(self.base, [post], con_ocr=True)
        self.assertEqual(len(consultar_avisos(self.base, "CORTE", "todos", "IV", "la florida", True, 50, 0)), 1)

    def test_cache_de_ocr_no_depende_de_la_firma_y_rechaza_url_ajena(self):
        cache = self.base.ruta.parent / "ocr"
        url = "https://scontent.example.fbcdn.net/123.jpg"
        with patch("backend.facebook_ocr._descargar_imagen", return_value=b"foto") as descarga, \
                patch("backend.facebook_ocr.subprocess.run", return_value=MagicMock(stdout="Asociación".encode())) as ocr:
            self.assertEqual(leer_imagen_facebook(f"{url}?firma=1", cache), "Asociación")
            self.assertEqual(leer_imagen_facebook(f"{url}?firma=2", cache), "Asociación")
            descarga.assert_called_once()
            ocr.assert_called_once()
        with self.assertRaises(ValueError):
            leer_imagen_facebook("https://scontent.fbcdn.net.example.com/foto.jpg", cache)

    def test_rss_recupera_aviso_de_eps_solo_en_imagen_y_omite_emojis(self):
        feed = b'''<rss version="2.0" xmlns:media="http://search.yahoo.com/mrss/">
          <channel><title>EPS Tacna</title><link>https://www.facebook.com/EPSTacna</link>
          <item><title>EPS Tacna</title><link>https://www.facebook.com/EPSTacna/posts/654</link>
          <description><![CDATA[<img src="https://static.fbcdn.net/images/emoji/a.png">]]></description>
          <media:content url="https://img.rss.app/aviso.jpg" medium="image"/>
          <pubDate>Sun, 22 Nov 2026 16:00:00 GMT</pubDate></item>
          </channel></rss>'''
        post, = publicaciones_rss(feed)
        self.assertEqual(post["message"], "")
        self.assertEqual(imagenes_publicacion(post), ["https://img.rss.app/aviso.jpg"])
        url_feed = "https://fetchrss.com/feed/avisoeps.rss"
        args = Namespace(facebook=False, facebook_historial=False, solo_facebook=True)
        with (patch.dict(os.environ, {"EPS_FACEBOOK_RSS_URL": url_feed}, clear=True),
              patch("backend.importar.descargar", return_value=feed) as descarga,
              patch("backend.importar.publicaciones_graph") as graph,
              patch("backend.importar.leer_imagen_facebook", return_value="Corte programado de agua potable\nSector IV") as ocr):
            self.assertTrue(sincronizar(self.base, args))
            descarga.assert_called_once_with(url_feed)
            graph.assert_not_called()
            self.assertTrue(ocr.call_args.kwargs["de_rss"])
        aviso, = consultar_avisos(self.base, "CORTE", "todos", "IV", None, True, 50, 0)
        self.assertTrue(aviso.url.endswith("/EPSTacna/posts/654"))
        self.assertEqual(aviso.publicado, "2026-11-22T11:00-05:00")
        numerico, = publicaciones_rss(feed.replace(b"/EPSTacna/posts/", b"/123456/posts/"))
        self.assertTrue(numerico["permalink_url"].endswith("/123456/posts/654"))
        self.assertEqual(url_rss_facebook("https://fetchrss.com/rss/abc.xml"), "https://fetchrss.com/rss/abc.xml")
        with self.assertRaises(ValueError):
            list(publicaciones_rss(feed.replace(b"/EPSTacna", b"/OtraPagina")))
        with self.assertRaises(ValueError):
            url_rss_facebook("https://rss.app.example.com/feeds/abc.xml")


if __name__ == "__main__":
    unittest.main()
