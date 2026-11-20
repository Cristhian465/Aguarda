package com.example.aguardapp.feature.liga.domain

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import com.example.aguardapp.feature.deposito.domain.model.horasEntre

data class MarcaNivel(val momento: LocalDateTime, val litros: Double)

data class ConsumoMedido(val litros: Double, val horas: Double) {
    fun litrosPorPersonaDia(habitantes: Int): Double = litros / horas * HORAS_POR_DIA / habitantes

    private companion object {
        const val HORAS_POR_DIA = 24.0
    }
}

fun lunesDe(fecha: LocalDate): LocalDate = fecha.minus(fecha.dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY)

class MedirConsumo {
    fun enLaSemana(marcas: List<MarcaNivel>, llenados: List<LocalDateTime>, lunes: LocalDate): ConsumoMedido? {
        val tramos = marcas
            .filter { lunesDe(it.momento.date) == lunes }
            .sortedBy { it.momento }
            .zipWithNext()
            .filter { (antes, despues) -> sinAguaNueva(antes, despues, llenados) }
        val horas = tramos.sumOf { (antes, despues) -> horasEntre(antes.momento, despues.momento) }
        if (horas < HORAS_MINIMAS) return null
        return ConsumoMedido(tramos.sumOf { (antes, despues) -> antes.litros - despues.litros }, horas)
    }

    private fun sinAguaNueva(antes: MarcaNivel, despues: MarcaNivel, llenados: List<LocalDateTime>): Boolean =
        despues.litros <= antes.litros && llenados.none { it > antes.momento && it <= despues.momento }

    companion object {
        const val HORAS_MINIMAS = 24.0
    }
}
