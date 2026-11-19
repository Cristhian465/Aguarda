package com.example.aguardapp.feature.eps.domain

import java.text.Normalizer
import kotlin.math.max
import kotlin.math.min

data class LineaReciboEps(val texto: String, val izquierda: Int, val arriba: Int, val derecha: Int, val abajo: Int)

/** Une solo las etiquetas conocidas con una cifra de la misma fila y columna del recibo EPS. */
fun alinearCamposEps(lineas: List<LineaReciboEps>, anchoPagina: Int): String {
    fun normalizar(texto: String) = Normalizer.normalize(texto.lowercase(), Normalizer.Form.NFD)
        .replace(Regex("\\p{M}"), "").trim()
    val numero = Regex("""(?:s\s*/\.?\s*)?\**\s*\d+(?:[.,]\d+)*""")
    return lineas.mapNotNull { etiqueta ->
        val texto = normalizar(etiqueta.texto)
        val volumen = texto.matches(Regex("""volumen\s+fac(?:turado)?\s+m[3³?]?"""))
        val total = texto.matches(Regex("""total a pagar\s*:?\s*(?:s\s*/\.?)?\s*\**"""))
        if (!volumen && !total) return@mapNotNull null
        val altura = (etiqueta.abajo - etiqueta.arriba).coerceAtLeast(1)
        val candidatos = lineas.filter { valor ->
            val solapamiento = min(etiqueta.abajo, valor.abajo) - max(etiqueta.arriba, valor.arriba)
            val alturaValor = (valor.abajo - valor.arriba).coerceAtLeast(1)
            val distancia = valor.izquierda - etiqueta.derecha
            numero.matches(normalizar(valor.texto)) &&
                (solapamiento.toDouble() / min(altura, alturaValor) >= 0.6 ||
                    (total && valor.arriba - etiqueta.abajo in 0..altura * 2)) &&
                distancia >= altura * 0.5 && distancia <= anchoPagina * (if (volumen) 0.20 else 0.35)
        }.sortedBy { it.izquierda }
        val elegido = candidatos.firstOrNull() ?: return@mapNotNull null
        // Dos cifras prácticamente a la misma distancia indican ambigüedad de OCR.
        if (candidatos.getOrNull(1)?.let { it.izquierda - elegido.izquierda < altura } == true) return@mapNotNull null
        val valor = normalizar(elegido.texto).replace(Regex("^(?:s\\s*/\\.?\\s*)?[*\\s]*"), "")
        if (volumen) "Consumo del mes: $valor m3" else "Total a pagar: S/ $valor"
    }.joinToString("\n")
}
