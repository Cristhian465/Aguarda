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
    val importes = lineas.filter { "total a pagar" in it || "importe total" in it }.mapNotNull { linea ->
        val valor = linea.substringAfter(if ("total a pagar" in linea) "total a pagar" else "importe total")
            .trim().removePrefix(":").trim().replace(Regex("^(s\\s*/\\.?|s/|pen)\\s*"), "")
            .replace(Regex("^[*\\s]+"), "")
        Regex("^([0-9]+(?:[.,][0-9]{3})*(?:[.,][0-9]{2})?)\\s*$")
            .matchEntire(valor)?.groupValues?.get(1)?.let(::importeDecimal)
    }.distinct()
    val metros = consumos.singleOrNull()
    val mes = meses.singleOrNull()
    val total = importes.singleOrNull()
    if (metros == null || mes == null || total == null) {
        throw ErrorEps("No pudimos identificar con certeza el consumo, periodo y total a pagar del recibo. Conservamos tu recibo anterior.")
    }
    return ReciboEps(mes, metros, total)
}

private fun importeDecimal(valor: String): Double? {
    val posicionDecimal = valor.indexOfLast { it == '.' || it == ',' }
    val tieneDecimales = posicionDecimal >= 0 && valor.length - posicionDecimal - 1 == 2
    val limpio = if (tieneDecimales) {
        valor.take(posicionDecimal).replace(",", "").replace(".", "") + "." + valor.substring(posicionDecimal + 1)
    } else valor.replace(",", "").replace(".", "")
    return limpio.toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0 }
}
