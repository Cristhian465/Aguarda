package com.example.aguardapp.feature.eps

import com.example.aguardapp.feature.eps.data.*
import com.example.aguardapp.feature.eps.domain.ErrorEps
import com.example.aguardapp.feature.eps.domain.MesRecibo
import com.example.aguardapp.feature.eps.domain.periodosImportables
import kotlinx.datetime.LocalDate
import org.jsoup.Jsoup
import org.junit.Assert.*
import org.junit.Test

class HistorialPortalEpsTest {
    private val inicio = "http://oficinavirtual.epstacna.com.pe/src/controlador/main.php"
    private val panel = """
        <a href="#" onclick="CargarListado('ctacte.php'); marcar('m1');">Historial de facturación</a>
        <script>function CargarListado(ruta) { var param = { ac:"listar" };
        ${'$'}.ajax({url:ruta, type:"post", data:param}); }</script>
    """
    private val tabla = """
        <table><thead><tr><th>PERIODO</th><th>N° RECIBO</th><th>CONSUMO</th><th>LECT. PROM.</th>
        <th>TOTAL MES</th><th>MES ATRAZ.</th><th>DEUDA</th><th>TOTAL</th></tr></thead><tbody>
        <tr><td>SETIEMBRE 2026</td><td>DEMO-1</td><td>0</td><td>5</td><td>S/ 4.50</td><td>0</td><td>S/ 0.00</td><td>S/ 4.50</td></tr>
        <tr><td>OCTUBRE 2026</td><td>DEMO-2</td><td>12,5</td><td>3</td><td>S/ 26.40</td><td>1</td><td>S/ 7.00</td><td>S/ 33.40</td></tr>
        <tr><td>NOVIEMBRE 2026</td><td>DEMO-3</td><td>—</td><td>0</td><td>S/ 0.00</td><td>0</td><td>S/ 0.00</td><td>S/ 0.00</td></tr>
        </tbody></table>
    """

    @Test fun usaLaSesionDelPortalYSeparaElCargoDelTotalConDeuda() {
        val cookies = mapOf("PHPSESSID" to "sesion-simulada")
        val cliente = ClientePortalEps(object : TransporteEps {
            override fun ejecutar(url: String, campos: Map<String, String>?, cookies: Map<String, String>, referencia: String?, ajax: Boolean): RespuestaEps {
                assertEquals("sesion-simulada", cookies["PHPSESSID"])
                return if (url == inicio) {
                    RespuestaEps(url, 200, "text/html", panel.toByteArray())
                } else {
                    assertEquals(inicio.replace("main.php", "ctacte.php"), url)
                    assertEquals(inicio, referencia)
                    assertEquals(mapOf("ac" to "listar"), campos)
                    assertTrue(ajax)
                    RespuestaEps(url, 200, "text/html", tabla.toByteArray())
                }
            }
        })
        val historial = cliente.historial(SesionEps("demo", inicio, cookies)).first
        assertEquals(listOf(MesRecibo(2026, 10), MesRecibo(2026, 9)), historial.map { it.mes })
        assertEquals(12.5, historial.first().metrosCubicos, 0.001)
        assertEquals(26.40, historial.first().totalMes!!, 0.001)
        assertEquals(7.0, historial.first().deuda!!, 0.001)
        assertEquals(33.40, historial.first().totalPagar, 0.001)
        assertEquals(0.0, historial.last().metrosCubicos, 0.001)
        assertNull(historial.first().diasFacturados)
    }

    @Test fun importaUnaTablaCompletaDeMasDeDocePeriodosIncluidoElUltimo() {
        val filas = (1..12).map { MesRecibo(2025, it) } + (1..12).map { MesRecibo(2026, it) }
        val html = filas.joinToString(
            prefix = "<table><tr><th>Periodo</th><th>Consumo</th><th>Total</th></tr>",
            postfix = "</table>", separator = ""
        ) { "<tr><td>$it</td><td>5</td><td>S/ 12.00</td></tr>" }

        val periodos = periodosImportables(leerHistorialEps(Jsoup.parse(html)), LocalDate(2026, 11, 21))

        assertEquals(23, periodos.size)
        assertEquals(MesRecibo(2026, 11), periodos.first().mes)
        assertEquals(MesRecibo(2025, 1), periodos.last().mes)
    }

    @Test fun noUsaLaLecturaPromedioNiElCargoDelMesComoSustituto() {
        val alterada = tabla.replace("<th>TOTAL</th>", "<th>OTRO</th>")
        assertThrows(ErrorEps::class.java) { leerHistorialEps(Jsoup.parse(alterada)) }
    }

    @Test fun noUtilizaLaSesionEnUnDestinoFueraDelHistorial() {
        val alterado = panel.replace("ctacte.php", "http://otro.example/ctacte.php")
        assertThrows(ErrorEps::class.java) { accesoHistorialEps(Jsoup.parse(alterado), inicio) }
    }
}
