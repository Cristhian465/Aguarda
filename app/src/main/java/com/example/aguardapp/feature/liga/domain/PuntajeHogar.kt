package com.example.aguardapp.feature.liga.domain

import kotlin.math.roundToInt
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.minus
import com.example.aguardapp.feature.deposito.domain.usecase.mediana

/** Puntaje semanal del hogar, de 0 a 100. */
data class PuntajeHogar(
    val total: Int,
    val puntosMejora: Int,
    val puntosRetos: Int,
    val puntosConstancia: Int,
    val mejoraPct: Double?
)

/** Todo lo que se sabe del hogar en una semana. Sin medición de la semana no hay puntaje. */
data class ResumenSemanal(
    val lunes: LocalDate,
    val litrosPorPersonaDia: Double?,
    val lineaBase: Double?,
    val diasConMarca: Int,
    val diasConRetos: Int,
    val puntaje: PuntajeHogar?
)

/**
 * Premia el esfuerzo y no el consumo bajo: con racionamiento, consumir poco puede significar
 * que no llegó agua. Pesos: mejora frente a su propio consumo 50, retos 30 y constancia 20.
 */
class CalcularPuntaje(private val medir: MedirConsumo = MedirConsumo()) {

    fun resumen(
        marcas: List<MarcaNivel>,
        llenados: List<LocalDateTime>,
        diasConReto: Set<LocalDate>,
        habitantes: Int,
        hoy: LocalDate,
        baseExterna: Double? = null
    ): ResumenSemanal {
        val lunes = lunesDe(hoy)
        val domingo = lunes.plusDias(DIAS_POR_SEMANA - 1)
        val actual = medir.enLaSemana(marcas, llenados, lunes)?.litrosPorPersonaDia(habitantes)
        val base = lineaBase(marcas, llenados, habitantes, lunes) ?: baseExterna
        val conMarca = marcas.map { it.momento.date }.filter { lunesDe(it) == lunes }.toSet().size
        val conRetos = diasConReto.count { it in lunes..domingo }
        return ResumenSemanal(lunes, actual, base, conMarca, conRetos, actual?.let { puntaje(it, base, conRetos, conMarca) })
    }

    fun puntaje(actual: Double, base: Double?, diasConRetos: Int, diasConMarca: Int): PuntajeHogar {
        val mejora = base?.takeIf { it > 0 }?.let { (it - actual) / it * 100 }
        val mejoraAcotada = ((mejora ?: 0.0) / MEJORA_PARA_PUNTAJE_MAXIMO).coerceIn(0.0, 1.0)
        val puntosMejora = (mejoraAcotada * PESO_MEJORA).roundToInt()
        val puntosRetos = proporcion(diasConRetos, PESO_RETOS)
        val puntosConstancia = proporcion(diasConMarca, PESO_CONSTANCIA)
        return PuntajeHogar(puntosMejora + puntosRetos + puntosConstancia, puntosMejora, puntosRetos, puntosConstancia, mejora)
    }

    // La línea base es la mediana de las últimas semanas medidas, sin contar la actual.
    private fun lineaBase(marcas: List<MarcaNivel>, llenados: List<LocalDateTime>, habitantes: Int, lunes: LocalDate): Double? =
        (1..SEMANAS_DE_BASE)
            .mapNotNull { atras -> medir.enLaSemana(marcas, llenados, lunes.plusDias(-atras * DIAS_POR_SEMANA)) }
            .map { it.litrosPorPersonaDia(habitantes) }
            .takeIf { it.isNotEmpty() }
            ?.mediana()

    private fun proporcion(dias: Int, peso: Int): Int =
        (dias.coerceIn(0, DIAS_POR_SEMANA).toDouble() / DIAS_POR_SEMANA * peso).roundToInt()

    private fun LocalDate.plusDias(dias: Int): LocalDate = minus(-dias, DateTimeUnit.DAY)

    companion object {
        const val PESO_MEJORA = 50
        const val PESO_RETOS = 30
        const val PESO_CONSTANCIA = 20
        const val MEJORA_PARA_PUNTAJE_MAXIMO = 20.0
        const val SEMANAS_DE_BASE = 4
        const val DIAS_POR_SEMANA = 7
    }
}
