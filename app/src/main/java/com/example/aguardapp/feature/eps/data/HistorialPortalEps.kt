package com.example.aguardapp.feature.eps.data

import com.example.aguardapp.feature.eps.domain.ErrorEps
import com.example.aguardapp.feature.eps.domain.ReciboEps
import com.example.aguardapp.feature.eps.domain.importeDecimal
import com.example.aguardapp.feature.eps.domain.leerMesDelRecibo
import java.net.URI
import java.text.Normalizer
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

internal data class AccesoHistorialEps(val url: String, val campos: Map<String, String>?, val ajax: Boolean)

private fun normalizar(texto: String): String = Normalizer.normalize(texto.lowercase(), Normalizer.Form.NFD)
    .replace(Regex("""\p{M}"""), "").trim().replace(Regex("""\s+"""), " ")

internal fun accesoHistorialEps(html: Document, base: String): AccesoHistorialEps {
    val enlace = html.select("a, button").filter { normalizar(it.text()).contains("historial de facturacion") }
        .singleOrNull() ?: throw ErrorEps("No encontramos Historial de facturación en el menú de EPS.")
    val llamada = Regex("""\b(CargarModulo|CargarListado)\s*\(\s*(['"])([^'"]+)\2\s*\)""")
        .find(enlace.attr("onclick"))

    if (llamada == null) {
        val href = enlace.attr("href").trim()
        if (href.isBlank() || href.startsWith("#")) throw ErrorEps("Cambió el acceso al historial de EPS.")
        return AccesoHistorialEps(rutaFacturacion(base, href), null, false)
    }

    val funcion = llamada.groupValues[1]
    val script = html.select("script:not([src])").joinToString("\n") { it.data() }
    val cuerpo = Regex("""\bfunction\s+$funcion\s*\([^)]*\)\s*\{([\s\S]*?)(?=\bfunction\s+\w+\s*\(|$)""")
        .find(script)?.groupValues?.get(1) ?: throw ErrorEps("EPS cambió el modo de consultar el historial.")
    val metodo = Regex("""(?i)\b(?:type|method)\s*:\s*['"](get|post)['"]""").find(cuerpo)
        ?.groupValues?.get(1)?.lowercase() ?: throw ErrorEps("No pudimos identificar el método del historial de EPS.")
    val accion = if (Regex("""\bdata\s*:""").containsMatchIn(cuerpo)) {
        Regex("""\bac\s*:\s*['"]([a-zA-Z0-9_-]{1,24})['"]""").find(cuerpo)?.groupValues?.get(1)
            ?: throw ErrorEps("El historial de EPS requiere un parámetro desconocido.")
    } else null
    val url = rutaFacturacion(base, llamada.groupValues[3])
    return when (metodo) {
        "post" -> AccesoHistorialEps(url, accion?.let { mapOf("ac" to it) }.orEmpty(), true)
        else -> AccesoHistorialEps(if (accion == null) url else "$url?ac=$accion", null, true)
    }
}

private fun rutaFacturacion(base: String, ruta: String): String {
    val url = resolverEps(base, ruta)
    val uri = URI(url)
    if (uri.path != "/src/controlador/ctacte.php" || uri.rawQuery != null || uri.rawFragment != null) {
        throw ErrorEps("El destino del historial de facturación de EPS cambió.")
    }
    return url
}

internal fun leerHistorialEps(html: Document): List<ReciboEps> {
    val candidatas = html.select("table").mapNotNull { tabla ->
        tabla.select("tr").firstNotNullOfOrNull { fila ->
            val columnas = fila.children().map { normalizar(it.text()) }
            val periodo = columnas.indexOf("periodo")
            val consumo = columnas.indexOfFirst { it == "consumo" || it in setOf("consumo (m3)", "consumo (m³)") }
            val total = columnas.indexOfFirst { it in setOf("total", "total a pagar") }
            if (periodo < 0 || consumo < 0 || total < 0) null else {
                val totalMes = columnas.indexOf("total mes")
                val deuda = columnas.indexOf("deuda")
                tabla.select("tr").filter { it != fila && it.closest("table") == tabla }.mapNotNull { registro ->
                    leerFila(registro, periodo, consumo, total, totalMes, deuda)
                }
            }
        }
    }

    val facturacion = candidatas.singleOrNull()
        ?: throw ErrorEps("No encontramos una tabla de facturación con periodo, consumo y total.")
    // Omite periodos ambiguos en vez de sumar o elegir entre varios recibos distintos del mismo mes.
    return facturacion.distinct().groupBy { it.mes }.values.mapNotNull { it.singleOrNull() }
        .sortedWith(compareByDescending<ReciboEps> { it.mes.anio }.thenByDescending { it.mes.mes })
}

private fun leerFila(fila: Element, periodo: Int, consumo: Int, total: Int, totalMes: Int, deuda: Int): ReciboEps? {
    val celdas = fila.children().filter { it.tagName() == "td" }
    val mes = celdas.getOrNull(periodo)?.text()?.let(::leerMesDelRecibo) ?: return null
    val metros = celdas.getOrNull(consumo)?.text()?.trim()?.replace(',', '.')?.toDoubleOrNull()
        ?.takeIf { it.isFinite() && it >= 0 } ?: return null
    fun importe(indice: Int): Double? = celdas.getOrNull(indice)?.text()?.let {
        importeDecimal(normalizar(it).replace(Regex("""^(s\s*/\.?|pen)\s*"""), ""))
    }
    return ReciboEps(mes, metros, importe(total) ?: return null, totalMes = importe(totalMes), deuda = importe(deuda))
}
