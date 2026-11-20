package com.example.aguardapp.feature.liga.domain

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.minus

const val SEMANAS_DEL_HISTORIAL = 6

data class LitrosSemana(val lunes: LocalDate, val litrosPorPersonaDia: Double?)

fun historialSemanal(
    marcas: List<MarcaNivel>,
    llenados: List<LocalDateTime>,
    habitantes: Int,
    hoy: LocalDate,
    semanas: Int = SEMANAS_DEL_HISTORIAL,
    medir: MedirConsumo = MedirConsumo()
): List<LitrosSemana> {
    val lunesActual = lunesDe(hoy)
    return (semanas - 1 downTo 0).map { atras ->
        val lunes = lunesActual.minus(atras * DIAS_POR_SEMANA, DateTimeUnit.DAY)
        LitrosSemana(lunes, medir.enLaSemana(marcas, llenados, lunes)?.litrosPorPersonaDia(habitantes))
    }
}

private const val DIAS_POR_SEMANA = 7
