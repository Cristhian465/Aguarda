package com.example.aguardapp.feature.eps.domain

import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil

internal fun periodosImportables(historial: List<ReciboEps>, hoy: LocalDate): List<ReciboEps> =
    historial.filter { it.mes.mesesHasta(hoy) >= 0 }

internal fun actualizarHistorial(historial: List<ReciboGuardado>, nuevo: ReciboGuardado): List<ReciboGuardado> {
    val anterior = historial.find { it.mes == nuevo.mes }
    val actualizado = if (anterior?.leidoDeEps == true && !nuevo.leidoDeEps) {
        anterior.copy(diasFacturados = anterior.diasFacturados ?: nuevo.diasFacturados)
    } else {
        nuevo.copy(
            totalPagar = nuevo.totalPagar ?: anterior?.totalPagar,
            totalMes = nuevo.totalMes ?: anterior?.totalMes,
            deuda = nuevo.deuda ?: anterior?.deuda,
            diasFacturados = nuevo.diasFacturados ?: anterior?.diasFacturados
        )
    }
    return (historial.filterNot { it.mes == nuevo.mes } + actualizado)
        .sortedWith(compareByDescending<ReciboGuardado> { it.mes.anio }.thenByDescending { it.mes.mes })
}

internal data class ComparacionRecibos(
    val metrosCubicos: Double,
    val totalPagar: Double?,
    val litrosPorDia: Double?
)

internal fun compararRecibos(reciente: ReciboGuardado, anterior: ReciboGuardado): ComparacionRecibos =
    ComparacionRecibos(
        metrosCubicos = reciente.metrosCubicos - anterior.metrosCubicos,
        totalPagar = diferenciaOpcional(reciente.totalPagar, anterior.totalPagar),
        litrosPorDia = diferenciaOpcional(reciente.litrosPorDia, anterior.litrosPorDia)
    )

private fun diferenciaOpcional(reciente: Double?, anterior: Double?): Double? =
    if (reciente != null && anterior != null) reciente - anterior else null

private val diasFacturados = Regex(
    """(?i)\b(?:d[ií]as\s+facturados\s*[:=]?\s*(\d{1,3})|(\d{1,3})\s+d[ií]as\s+facturados)\b"""
)
private val rangoFacturado = Regex(
    """(?i)\b(?:per[ií]odo\s+(?:de\s+consumo|facturado|de\s+facturaci[oó]n))\s*[:=]?\s*(?:del?\s+)?(\d{1,2}/\d{1,2}/20\d{2})\s*(?:al?|[-–])\s*(\d{1,2}/\d{1,2}/20\d{2})\b"""
)

private val bloquePeriodoConsumo = Regex(
    """(?i)\bper[ií]odo\s+de\s+consumo[ \t]*:?[ \t]*\r?\n((?:[^\r\n]*(?:\r?\n|$)){1,4})"""
)
private val fechaAnterior = Regex("""(?i)\bfecha\s+anterior\s*:?\s*(\d{1,2}/\d{1,2}/20\d{2})\b""")
private val fechaActual = Regex("""(?i)\bfecha\s+actual\s*:?\s*(\d{1,2}/\d{1,2}/20\d{2})\b""")

internal fun leerDiasFacturados(texto: String): Int? {
    val explicitos = diasFacturados.findAll(texto).map { coincidencia ->
        coincidencia.groupValues[1].ifEmpty { coincidencia.groupValues[2] }.toInt()
    }.distinct().toList()
    if (explicitos.isNotEmpty()) return explicitos.singleOrNull()?.takeIf { it in 1..366 }
    val rangos = rangoFacturado.findAll(texto).mapNotNull { coincidencia ->
        diasEntre(coincidencia.groupValues[1], coincidencia.groupValues[2])
    }
    val bloques = bloquePeriodoConsumo.findAll(texto).mapNotNull { coincidencia ->
        val bloque = coincidencia.groupValues[1]
        val inicio = fechaAnterior.findAll(bloque).singleOrNull() ?: return@mapNotNull null
        val fin = fechaActual.findAll(bloque).singleOrNull() ?: return@mapNotNull null
        diasEntre(inicio.groupValues[1], fin.groupValues[1])
    }
    return (rangos + bloques).distinct().toList().singleOrNull()
}

private fun diasEntre(desde: String, hasta: String): Int? {
    val inicio = fechaDelRecibo(desde) ?: return null
    val fin = fechaDelRecibo(hasta) ?: return null
    return inicio.daysUntil(fin).takeIf { it in 1..366 }
}

private fun fechaDelRecibo(texto: String): LocalDate? = runCatching {
    val (dia, mes, anio) = texto.split('/').map(String::toInt)
    LocalDate(anio, mes, dia)
}.getOrNull()
