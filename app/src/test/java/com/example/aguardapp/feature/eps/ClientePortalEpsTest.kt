package com.example.aguardapp.feature.eps

import com.example.aguardapp.feature.eps.data.*
import com.example.aguardapp.feature.eps.domain.ErrorEps
import org.jsoup.Jsoup
import org.junit.Assert.*
import org.junit.Test

class ClientePortalEpsTest {
    private val base = "http://oficinavirtual.epstacna.com.pe"
    private val login = """<form method="post" action="../controlador/login.php"><input name="usu"><input name="pas" type="password"><input type="hidden" name="token" value="csrf"></form>"""
    private val panel = """<a href="recibos.php">Mis recibos</a><a href="salir.php">Salir</a>"""
    private val panelDinamico = """
        <a href="" id="m4" onclick="CargarRecibo('recibo.php'); marcar('m4');">Recibo virtual PDF</a>
        <script>
        function CargarRecibo(jRuta) {
            ${'$'}.ajax({url: jRuta, type: 'post', success: function(response) { MostrarRecibo(); }});
        }
        function MostrarRecibo() {
            ${'$'}.ajax({url: '../vista/mostrar_pdf.php', type: 'post', success: function(response) {}});
        }
        function marcar(jId) {}
        </script>
    """.trimIndent()

    private class Falso(vararg respuestas: RespuestaEps) : TransporteEps {
        val pendientes = respuestas.toMutableList()
        val peticiones = mutableListOf<Triple<String, Map<String, String>?, Map<String, String>>>()
        override fun ejecutar(url: String, campos: Map<String, String>?, cookies: Map<String, String>, referencia: String?, ajax: Boolean): RespuestaEps {
            peticiones += Triple(url, campos, cookies)
            return pendientes.removeAt(0)
        }
    }

    private fun respuesta(ruta: String, html: String, codigo: Int = 200,
        cookies: Map<String, String> = emptyMap(), destino: String? = null) =
        RespuestaEps(base + ruta, codigo, "text/html", html.toByteArray(), cookies, destino)

    @Test fun vinculaConElFormularioRealConservandoCamposOcultosYLaSesion() {
        val red = Falso(respuesta("/src/vista/v_login.php", login),
            respuesta("/src/controlador/login.php", "", 302, mapOf("PHPSESSID" to "sesion-prueba"), "../vista/inicio.php"),
            respuesta("/src/vista/inicio.php", panel))
        val sesion = ClientePortalEps(red).vincular(" 123 ", "clave-prueba")
        assertEquals("123", sesion.usuario)
        assertEquals("sesion-prueba", sesion.cookies["PHPSESSID"])
        assertEquals(base + "/src/controlador/login.php", red.peticiones[1].first)
        assertEquals(mapOf("usu" to "123", "pas" to "clave-prueba", "token" to "csrf"), red.peticiones[1].second)
        assertNull(red.peticiones[2].second)
    }

    @Test fun rechazaClaveIncorrectaSinDeclararCuentaVinculada() {
        val red = Falso(respuesta("/src/vista/v_login.php", login), respuesta("/src/controlador/login.php", login))
        assertThrows(ErrorEps::class.java) { ClientePortalEps(red).vincular("123", "incorrecta") }
    }

    @Test fun noEnviaCredencialesSiElFormularioApuntaAOtroHost() {
        val inseguro = login.replace("../controlador/login.php", "http://otro.example/login.php")
        val red = Falso(respuesta("/src/vista/v_login.php", inseguro))
        assertThrows(ErrorEps::class.java) { ClientePortalEps(red).vincular("123", "clave") }
        assertEquals(1, red.peticiones.size)
    }

    @Test fun bloqueaRedireccionHaciaOtroServidorAntesDeEnviarCookies() {
        val red = Falso(respuesta("/src/vista/inicio.php", "", 302, destino = "https://otro.example/recibo.pdf"))
        assertThrows(ErrorEps::class.java) { ClientePortalEps(red).documento(SesionEps("123", base + "/src/vista/inicio.php", mapOf("PHPSESSID" to "privada"))) }
        assertEquals(1, red.peticiones.size)
    }

    @Test fun nuncaReenviaLaClaveEnUnaRedireccion307() {
        val red = Falso(respuesta("/src/vista/v_login.php", login),
            respuesta("/src/controlador/login.php", "", 307, destino = "otro.php"))
        assertThrows(ErrorEps::class.java) { ClientePortalEps(red).vincular("123", "clave") }
        assertEquals(2, red.peticiones.size)
    }

    @Test fun unaSesionVencidaSolicitaNuevaVinculacion() {
        val red = Falso(respuesta("/src/vista/inicio.php", login))
        val error = assertThrows(ErrorEps::class.java) { ClientePortalEps(red).documento(SesionEps("123", base + "/src/vista/inicio.php", emptyMap())) }
        assertTrue(error.requiereVincular)
    }

    @Test fun descargaElPdfUsandoLaSesionSinSolicitarLaClave() {
        val red = Falso(respuesta("/src/vista/inicio.php", "<a href='recibo.pdf'>Último recibo</a>"),
            respuesta("/src/vista/recibo.pdf", "%PDF-1.7 prueba"))
        val (pdf, _) = ClientePortalEps(red).documento(SesionEps("123", base + "/src/vista/inicio.php", mapOf("PHPSESSID" to "sesion")))
        assertTrue(String(pdf).startsWith("%PDF-"))
        assertTrue(red.peticiones.all { it.second == null && it.third["PHPSESSID"] == "sesion" })
    }

    @Test fun escogeElPeriodoMasRecienteSinDependerDelOrdenDeLasFilas() {
        val html = Jsoup.parse("""<table><tr><td>09/2026</td><td><a href="sep.pdf">Recibo</a></td></tr><tr><td>11/2026</td><td><a href="nov.pdf">Recibo</a></td></tr></table>""")
        assertEquals(base + "/nov.pdf", enlaceRecibo(html, "$base/"))
    }

    @Test fun noAdivinaElReciboCuandoHayVariosSinPeriodo() {
        val html = Jsoup.parse("<a href='a.pdf'>Recibo</a><a href='b.pdf'>Recibo</a>")
        assertThrows(ErrorEps::class.java) { enlaceRecibo(html, "$base/") }
    }

    @Test fun noSigueEnlacesDePagoOBorradoAunqueMencionenRecibos() {
        val html = Jsoup.parse("<a href='pagar-recibo.php'>Pagar recibo</a><a href='anular-factura.php'>Anular factura</a><a href='actual.pdf'>Último recibo</a>")
        assertEquals(base + "/actual.pdf", enlaceRecibo(html, "$base/"))
    }

    @Test fun permiteHttpSoloEnElHostExactoDeEpsYConservaHttps() {
        assertEquals(LOGIN_EPS, urlPermitidaEps(LOGIN_EPS))
        assertTrue(LOGIN_EPS.startsWith("http://"))
        assertEquals("http://oficinavirtual.epstacna.com.pe:80/", urlPermitidaEps("http://oficinavirtual.epstacna.com.pe:80/"))
        assertEquals("https://oficinavirtual.epstacna.com.pe/", urlPermitidaEps("https://oficinavirtual.epstacna.com.pe/"))
        assertThrows(ErrorEps::class.java) { urlPermitidaEps("http://sub.oficinavirtual.epstacna.com.pe/") }
    }

    @Test fun rechazaOtrosProtocolosCredencialesEnUrlPuertosYServidoresParecidos() {
        listOf("ftp://oficinavirtual.epstacna.com.pe/", "http://oficinavirtual.epstacna.com.pe:443/", "https://oficinavirtual.epstacna.com.pe.otro.example/",
            "https://usuario:clave@oficinavirtual.epstacna.com.pe/", "https://oficinavirtual.epstacna.com.pe:8443/")
            .forEach { assertThrows(ErrorEps::class.java) { urlPermitidaEps(it) } }
    }

    @Test fun interpretaLasDosSolicitudesAjaxSinEjecutarJavascript() {
        val pasos = pasosReciboVirtual(Jsoup.parse(panelDinamico), "$base/src/controlador/main.php")!!
        assertEquals(listOf(PasoReciboEps("$base/src/controlador/recibo.php", true),
            PasoReciboEps("$base/src/vista/mostrar_pdf.php", true)), pasos)
    }

    @Test fun recorreMenuAjaxVisorYPdfConLasCookiesDeLaSesion() {
        val red = Falso(respuesta("/src/controlador/main.php", panelDinamico),
            respuesta("/src/controlador/recibo.php", "<div>Visor</div>"),
            respuesta("/src/vista/mostrar_pdf.php", "<embed src='actual.pdf' type='application/pdf'>"),
            respuesta("/src/vista/actual.pdf", "%PDF-1.7 prueba"))
        val (pdf, _) = ClientePortalEps(red).documento(SesionEps("123", "$base/src/controlador/main.php", mapOf("PHPSESSID" to "prueba")))
        assertTrue(String(pdf).startsWith("%PDF-"))
        assertEquals(emptyMap<String, String>(), red.peticiones[1].second)
        assertEquals(emptyMap<String, String>(), red.peticiones[2].second)
        assertNull(red.peticiones[3].second)
        assertTrue(red.peticiones.all { it.third["PHPSESSID"] == "prueba" })
    }

    @Test fun bloqueaElDestinoExternoEnElJavascriptDelVisor() {
        val html = panelDinamico.replace("../vista/mostrar_pdf.php", "http://otro.example/recibo.php")
        assertThrows(ErrorEps::class.java) { pasosReciboVirtual(Jsoup.parse(html), "$base/src/controlador/main.php") }
    }

    @Test fun noAdivinaParametrosExtraDelAjax() {
        val html = panelDinamico.replace("url: jRuta,", "url: jRuta, data: {accion: 'desconocida'},")
        assertThrows(ErrorEps::class.java) { pasosReciboVirtual(Jsoup.parse(html), "$base/src/controlador/main.php") }
    }

    @Test fun reconoceSesionVencidaAntesDeSolicitarElVisor() {
        val red = Falso(respuesta("/src/controlador/main.php", panelDinamico), respuesta("/src/controlador/recibo.php", login))
        val error = assertThrows(ErrorEps::class.java) { ClientePortalEps(red).documento(SesionEps("123", "$base/src/controlador/main.php", emptyMap())) }
        assertTrue(error.requiereVincular)
        assertEquals(2, red.peticiones.size)
    }
}
