package com.example.aguardapp.feature.liga.presentation

import kotlin.math.roundToInt
import com.example.aguardapp.feature.liga.domain.Duelo
import com.example.aguardapp.feature.liga.domain.FilaRanking
import com.example.aguardapp.feature.liga.domain.HOGARES_MINIMOS
import com.example.aguardapp.feature.liga.domain.ResumenSemanal

fun filaDeMiDistrito(filas: List<FilaRanking>, ubigeo: String?): FilaRanking? = filas.firstOrNull { it.ubigeo == ubigeo }

fun detalleDeMiDistrito(filas: List<FilaRanking>, ubigeo: String?): String {
    val fila = filaDeMiDistrito(filas, ubigeo) ?: return "Distritos de Tacna"
    val puesto = fila.puesto
    return when {
        puesto != null -> "Tu distrito va ${puesto}º"
        fila.hogares == 0 -> "Tu distrito aún no tiene hogares"
        else -> textoHogaresQueFaltan(HOGARES_MINIMOS - fila.hogares)
    }
}

fun detalleDeConsumo(resumen: ResumenSemanal?): String =
    resumen?.litrosPorPersonaDia?.let { "${it.roundToInt()} L por persona al día" } ?: "Aún sin medición"

fun detalleDePuntaje(resumen: ResumenSemanal?): String =
    resumen?.puntaje?.let { "${it.total} de 100" } ?: "Aún sin puntaje"

fun detalleDeDuelos(duelos: List<Duelo>): String = when (duelos.size) {
    0 -> "Aún sin duelos"
    1 -> "1 duelo esta semana"
    else -> "${duelos.size} duelos esta semana"
}

fun fraccionDelDuelo(puntosLocal: Double, puntosVisitante: Double): Float {
    val total = puntosLocal + puntosVisitante
    return if (total <= 0.0) 0.5f else (puntosLocal / total).toFloat()
}

fun fraccionDelPuntaje(puntaje: Double): Float = (puntaje / PUNTAJE_MAXIMO).coerceIn(0.0, 1.0).toFloat()

fun fraccionDeHogares(hogares: Int): Float = (hogares.toFloat() / HOGARES_MINIMOS).coerceIn(0f, 1f)

private const val PUNTAJE_MAXIMO = 100.0
