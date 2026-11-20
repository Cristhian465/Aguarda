package com.example.aguardapp.feature.liga.domain

import kotlinx.datetime.LocalDate

data class Duelo(val local: FilaRanking, val visitante: FilaRanking) {
    val ganador: FilaRanking?
        get() {
            val puntosLocal = local.puntaje ?: return null
            val puntosVisitante = visitante.puntaje ?: return null
            return when {
                puntosLocal > puntosVisitante -> local
                puntosVisitante > puntosLocal -> visitante
                else -> null
            }
        }
}

fun duelosDeLaSemana(filas: List<FilaRanking>, lunes: LocalDate): List<Duelo> {
    val compiten = filas.filter { it.puntaje != null }.sortedBy { it.ubigeo }
    if (compiten.size < 2) return emptyList()
    val semana = (lunes.toEpochDays() / DIAS_POR_SEMANA).toInt()
    val fijo = compiten.first()
    val resto = compiten.drop(1)
    val giro = Math.floorMod(semana, resto.size)
    val rotados = listOf(fijo) + resto.drop(giro) + resto.take(giro)
    return rotados.chunked(2).filter { it.size == 2 }.map { (local, visitante) -> Duelo(local, visitante) }
}

private const val DIAS_POR_SEMANA = 7
