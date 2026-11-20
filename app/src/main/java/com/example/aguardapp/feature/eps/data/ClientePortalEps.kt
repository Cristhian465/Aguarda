package com.example.aguardapp.feature.eps.data

import com.example.aguardapp.feature.eps.domain.ErrorEps
import com.example.aguardapp.feature.eps.domain.leerMesDelRecibo
import java.net.URI
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

internal const val LOGIN_EPS = "http://oficinavirtual.epstacna.com.pe/src/vista/v_login.php"
private const val HOST_EPS = "oficinavirtual.epstacna.com.pe"
internal const val LIMITE_DOCUMENTO = 8 * 1024 * 1024

internal data class SesionEps(val usuario: String, val inicio: String, val cookies: Map<String, String>)
internal data class RespuestaEps(
    val url: String,
    val codigo: Int,
    val tipo: String,
    val contenido: ByteArray,
    val cookies: Map<String, String> = emptyMap(),
    val destino: String? = null
) {
    fun html(): Document = Jsoup.parse(contenido.inputStream(), null, url)
    fun esPdf(): Boolean = contenido.take(5).toByteArray().contentEquals("%PDF-".toByteArray())
}

internal fun urlPermitidaEps(url: String): String {
    val uri = runCatching { URI(url) }.getOrNull()
    if (uri == null || uri.scheme !in listOf("http", "https") || uri.host != HOST_EPS ||
        uri.port !in listOf(-1, if (uri.scheme == "http") 80 else 443) || uri.userInfo != null) {
        throw ErrorEps("EPS redirigió a una dirección ajena a su oficina virtual o a un puerto no permitido. No enviamos tus datos.")
    }
    return uri.toASCIIString()
}

internal fun resolverEps(base: String, ruta: String): String =
    urlPermitidaEps(runCatching { URI(base).resolve(ruta).toString() }.getOrDefault(""))

internal interface TransporteEps {
    fun ejecutar(url: String, campos: Map<String, String>?, cookies: Map<String, String>, referencia: String? = null, ajax: Boolean = false): RespuestaEps
}

internal class ClientePortalEps(private val transporte: TransporteEps) {
    fun vincular(usuario: String, clave: String): SesionEps {
        if (usuario.isBlank() || clave.isBlank()) throw ErrorEps("Escribe el usuario y la clave de tu recibo EPS.")
        val cookies = mutableMapOf<String, String>()
        val pagina = solicitar(LOGIN_EPS, null, cookies)
        val formulario = pagina.html().select("form").firstOrNull {
            it.selectFirst("input[name=usu]") != null && it.selectFirst("input[name=pas]") != null
        } ?: throw ErrorEps("El acceso de EPS cambió. No enviamos tu clave; necesitamos actualizar el conector.")
        if (!formulario.attr("method").equals("post", ignoreCase = true)) {
            throw ErrorEps("El formulario de EPS cambió y ya no permite enviar la clave mediante POST.")
        }
        val accion = resolverEps(pagina.url, formulario.attr("action"))
        val campos = formulario.select("input[type=hidden][name]").associate { it.attr("name") to it.attr("value") } +
            mapOf("usu" to usuario.trim(), "pas" to clave)
        val respuesta = solicitar(accion, campos, cookies, pagina.url)
        val html = respuesta.html()
        if (esLogin(html)) throw ErrorEps("EPS no aceptó el acceso. Revisa el usuario y la clave de tu recibo.")
        val evidencia = html.select("a[href]").any {
            Regex("recibo|factur|cerrar sesi[oó]n|salir|logout", RegexOption.IGNORE_CASE)
                .containsMatchIn(it.text() + " " + it.attr("href"))
        }
        if (cookies.isEmpty() || !evidencia) {
            throw ErrorEps("No pudimos confirmar el inicio de sesión en EPS. La cuenta no quedó vinculada.")
        }
        return SesionEps(usuario.trim(), respuesta.url, cookies.toMap())
    }

    fun documento(sesion: SesionEps): Pair<ByteArray, SesionEps> {
        val cookies = sesion.cookies.toMutableMap()
        var pagina = solicitar(sesion.inicio, null, cookies)
        val visitadas = mutableSetOf<String>()
        repeat(5) {
            if (pagina.esPdf()) return pagina.contenido to sesion.copy(cookies = cookies.toMap())
            val html = pagina.html()
            if (esLogin(html)) throw ErrorEps("Tu sesión de EPS venció. Vuelve a vincular la cuenta.", true)
            if (!visitadas.add(pagina.url)) throw ErrorEps("EPS no entregó el archivo del recibo.")
            val pasos = pasosReciboVirtual(html, pagina.url)
            if (pasos != null) {
                pasos.forEach { paso ->
                    if (!pagina.esPdf()) {
                        pagina = solicitar(paso.url, if (paso.post) emptyMap() else null, cookies, sesion.inicio, ajax = true)
                        if (!pagina.esPdf() && esLogin(pagina.html())) {
                            throw ErrorEps("Tu sesión de EPS venció. Vuelve a vincular la cuenta.", true)
                        }
                    }
                }
            } else pagina = solicitar(enlaceRecibo(html, pagina.url), null, cookies, sesion.inicio)
        }
        if (pagina.esPdf()) return pagina.contenido to sesion.copy(cookies = cookies.toMap())
        throw ErrorEps("El portal no entregó un PDF mediante enlaces directos. Este formato requiere adaptar el conector.")
    }

    private fun solicitar(url: String, campos: Map<String, String>?, cookies: MutableMap<String, String>, referencia: String? = null, ajax: Boolean = false): RespuestaEps {
        var destino = urlPermitidaEps(url)
        var datos = campos
        repeat(6) {
            val respuesta = transporte.ejecutar(destino, datos, cookies.toMap(), referencia, ajax)
            urlPermitidaEps(respuesta.url)
            cookies.putAll(respuesta.cookies)
            if (respuesta.contenido.size >= LIMITE_DOCUMENTO) throw ErrorEps("El archivo de EPS supera el límite de 8 MB.")
            if (respuesta.codigo in listOf(301, 302, 303, 307, 308)) {
                destino = resolverEps(destino, respuesta.destino ?: throw ErrorEps("EPS devolvió una redirección incompleta."))
                if (datos != null && respuesta.codigo in listOf(307, 308)) {
                    throw ErrorEps("EPS cambió el destino del acceso. No reenviamos tu clave.")
                }
                datos = null
            } else {
                if (respuesta.codigo == 401 || respuesta.codigo == 403) {
                    throw ErrorEps("EPS no permite continuar con esta sesión. Vuelve a vincular tu cuenta.", true)
                }
                if (respuesta.codigo !in 200..299) throw ErrorEps("EPS no está disponible en este momento. Intenta más tarde.")
                return respuesta
            }
        }
        throw ErrorEps("EPS devolvió demasiadas redirecciones.")
    }
}

internal fun esLogin(html: Document): Boolean = html.selectFirst("input[name=pas]") != null

internal data class PasoReciboEps(val url: String, val post: Boolean)

internal fun pasosReciboVirtual(html: Document, base: String): List<PasoReciboEps>? {
    val botones = html.select("a[onclick], button[onclick]").filter {
        Regex("recibo\\s+virtual|recibo\\s+pdf", RegexOption.IGNORE_CASE).containsMatchIn(it.text())
    }
    if (botones.isEmpty()) return null
    if (botones.size != 1) throw ErrorEps("EPS muestra varios accesos al recibo virtual y no pudimos identificar uno único.")
    val llamada = Regex("""\bCargarRecibo\(\s*(["'])([^"']+)\1\s*\)""")
        .find(botones.single().attr("onclick"))
        ?: throw ErrorEps("Cambió el botón del recibo virtual de EPS. Necesitamos adaptar su navegación.")
    val script = html.select("script:not([src])").joinToString("\n") { it.data() }
    fun funcion(nombre: String): String = Regex("""\bfunction\s+$nombre\s*\([^)]*\)\s*\{([\s\S]*?)(?=\bfunction\s+\w+\s*\(|$)""")
        .find(script)?.groupValues?.get(1)
        ?: throw ErrorEps("EPS no incluyó la función de navegación del recibo virtual.")
    fun paso(url: String, cuerpo: String): PasoReciboEps {
        if (Regex("""\bdata\s*:""").containsMatchIn(cuerpo)) {
            throw ErrorEps("El recibo requiere parámetros adicionales que todavía no podemos identificar.")
        }
        val metodo = Regex("""\btype\s*:\s*["'](get|post)["']""", RegexOption.IGNORE_CASE)
            .find(cuerpo)?.groupValues?.get(1)
            ?: throw ErrorEps("No pudimos identificar cómo solicita EPS el recibo virtual.")
        return PasoReciboEps(resolverEps(base, url), metodo.equals("post", ignoreCase = true))
    }
    val cargar = funcion("CargarRecibo")
    if (!Regex("""\bMostrarRecibo\s*\(\s*\)""").containsMatchIn(cargar)) {
        throw ErrorEps("Cambió el flujo del recibo virtual de EPS.")
    }
    val mostrar = funcion("MostrarRecibo")
    val visor = Regex("""\burl\s*:\s*(["'])([^"']+)\1""").find(mostrar)?.groupValues?.get(2)
        ?: throw ErrorEps("EPS no incluyó la dirección del visor del recibo.")
    return listOf(paso(llamada.groupValues[2], cargar), paso(visor, mostrar))
}

internal fun enlaceRecibo(html: Document, base: String): String {
    data class Enlace(val url: String, val prioridad: Int, val periodo: Int?)
    val candidatos = html.select("a[href], iframe[src], embed[src], object[data]").mapNotNull { elemento ->
        val ruta = elemento.attr(if (elemento.tagName() == "a") "href" else if (elemento.tagName() == "object") "data" else "src")
        val texto = (elemento.text() + " " + elemento.attr("title") + " " + ruta).lowercase()
        if (Regex("pagar|pago|eliminar|anular|cancelar|borrar|logout|salir|reclamo").containsMatchIn(texto)) return@mapNotNull null
        if (ruta.isBlank() || ruta.startsWith("#") || !Regex("recibo|factur|\\.pdf(?:[?#]|$)").containsMatchIn(texto)) return@mapNotNull null
        val url = runCatching { resolverEps(base, ruta) }.getOrNull() ?: return@mapNotNull null
        val contexto = elemento.closest("tr")?.text() ?: elemento.text()
        val mes = leerMesDelRecibo(contexto)
        val prioridad = when {
            Regex("[uú]ltimo recibo|recibo actual").containsMatchIn(texto) -> 3
            ruta.contains(".pdf", ignoreCase = true) || elemento.tagName() != "a" -> 2
            else -> 1
        }
        Enlace(url, prioridad, mes?.let { it.anio * 12 + it.mes })
    }.distinctBy { it.url }
    if (candidatos.isEmpty()) throw ErrorEps("No encontramos un enlace directo al recibo. El portal puede requerir un formato todavía no compatible.")
    val prioritarios = candidatos.filter { it.prioridad == candidatos.maxOf { c -> c.prioridad } }
    if (prioritarios.size == 1) return prioritarios.single().url
    if (prioritarios.all { it.periodo != null }) {
        val recientes = prioritarios.filter { it.periodo == prioritarios.maxOf { c -> c.periodo!! } }
        if (recientes.size == 1) return recientes.single().url
    }
    throw ErrorEps("Hay varios recibos y no pudimos identificar el más reciente. No elegimos uno al azar.")
}

internal class TransporteJsoupEps : TransporteEps {
    override fun ejecutar(url: String, campos: Map<String, String>?, cookies: Map<String, String>, referencia: String?, ajax: Boolean): RespuestaEps {
        val conexion = Jsoup.connect(urlPermitidaEps(url)).followRedirects(false)
            .ignoreContentType(true).ignoreHttpErrors(true).timeout(20_000)
            .maxBodySize(LIMITE_DOCUMENTO).cookies(cookies)
        conexion.header("Accept", "application/pdf,text/html,application/xhtml+xml,*/*;q=0.8")
            .header("Accept-Language", "es-PE,es;q=0.9")
            .userAgent("AguardApp/1.0 (Android; EPS Tacna receipt client)")
        referencia?.let { conexion.referrer(urlPermitidaEps(it)) }
        if (ajax) conexion.header("X-Requested-With", "XMLHttpRequest")
        if (campos != null) conexion.method(org.jsoup.Connection.Method.POST).data(campos)
        val respuesta = conexion.execute()
        return RespuestaEps(respuesta.url().toString(), respuesta.statusCode(), respuesta.contentType().orEmpty(),
            respuesta.bodyAsBytes(), respuesta.cookies(), respuesta.header("Location"))
    }
}
