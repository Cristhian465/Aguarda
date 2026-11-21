package com.example.aguardapp.feature.eps.domain

import com.example.aguardapp.feature.eps.domain.leerMesDelRecibo
import java.text.Normalizer

fun extraerReciboEps(texto: String): ReciboEps {
    val normalizado = Normalizer.normalize(texto.lowercase(), Normalizer.Form.NFD)
        .replace(Regex("\\p{M}"), "")
    val originales = normalizado.lines().map(String::trim).filter(String::isNotEmpty)
    val lineas = originales.mapIndexed { indice, linea ->
        val siguiente = originales.getOrNull(indice + 1).orEmpty()
        val esValor = siguiente.matches(Regex("""(?:s\s*/\.?\s*)?\d+(?:[.,]\d+)*"""))
        val esMes = siguiente.matches(Regex("""(?:[a-z]+[\s/-]+(?:de\s+)?20\d{2}|\d{1,2}/20\d{2})"""))
        val etiqueta = Regex("consumo|volumen\\s+fac|total a pagar|importe total|facturacion|mes facturado")
        if (etiqueta.containsMatchIn(linea) && (esValor || esMes)) "$linea $siguiente" else linea
    }
    val consumos = lineas.filter {
        Regex("consumo|volumen\\s+fac(?:turado)?").containsMatchIn(it) &&
            !Regex("promedio|histor|anterior|lectura").containsMatchIn(it)
    }.flatMap { linea ->
        val numero = "(\\d{1,3}(?:[.,]\\d{1,2})?)"
        val despues = Regex("(?<![\\d.,])$numero\\s*m\\s*[3³](?![a-z\\d])").findAll(linea)
            .map { it.groupValues[1] }.toList()
        val antes = Regex("m\\s*[3³]\\W{0,6}$numero(?![\\d/-])").findAll(linea)
            .map { it.groupValues[1] }.toList()
        val volumen = Regex("""volumen\s+fac(?:turado)?\s+m(?:\s*[3³])?\s*[:.]?\s*$numero\s*$""")
            .find(linea)?.groupValues?.get(1)
        (despues + antes + listOfNotNull(volumen)).mapNotNull { it.replace(',', '.').toDoubleOrNull()?.takeIf { n -> n in 0.0..500.0 } }
    }.distinct()
    val meses = lineas.filter { Regex("periodo|mes facturado|facturacion").containsMatchIn(it) }
        .mapNotNull(::leerMesDelRecibo).distinct()
    val metros = consumos.singleOrNull()
    val mes = meses.singleOrNull()
    val total = leerTotalAPagar(texto)
    if (metros == null || mes == null || total == null) {
        throw ErrorEps("No pudimos identificar con certeza el consumo, periodo y total a pagar del recibo. Conservamos tu recibo anterior.")
    }
    return ReciboEps(mes, metros, total, leerDiasFacturados(texto))
}

fun leerTotalAPagar(texto: String): Double? {
    val lineas = Normalizer.normalize(texto.lowercase(), Normalizer.Form.NFD)
        .replace(Regex("""\p{M}"""), "").lines().map(String::trim).filter(String::isNotEmpty)
    fun limpiarImporte(valor: String) = valor.trim().removePrefix(":").trim()
        .replace(Regex("""^(s\s*/\.?|pen)\s*"""), "").trimStart('*', ' ')
    return lineas.mapIndexedNotNull { indice, linea ->
        val etiqueta = when {
            "total a pagar" in linea -> "total a pagar"
            "importe total" in linea -> "importe total"
            else -> return@mapIndexedNotNull null
        }
        val importeEnLinea = limpiarImporte(linea.substringAfter(etiqueta))
        val valor = importeEnLinea.ifBlank { limpiarImporte(lineas.getOrNull(indice + 1).orEmpty()) }
        importeDecimal(valor)
    }.distinct().singleOrNull()
}

internal fun importeDecimal(valor: String): Double? {
    if (!Regex("""[0-9]+(?:[.,][0-9]{3})*(?:[.,][0-9]{2})?""").matches(valor)) return null
    val posicionDecimal = valor.indexOfLast { it == '.' || it == ',' }
    val tieneDecimales = posicionDecimal >= 0 && valor.length - posicionDecimal - 1 == 2
    val limpio = if (tieneDecimales) {
        valor.take(posicionDecimal).replace(",", "").replace(".", "") + "." + valor.substring(posicionDecimal + 1)
    } else valor.replace(",", "").replace(".", "")
    return limpio.toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0 }
}
