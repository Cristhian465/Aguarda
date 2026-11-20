package com.example.aguardapp.feature.mapa.domain.usecase

import kotlinx.datetime.LocalTime
import com.example.aguardapp.feature.mapa.domain.model.EstadoCisterna
import com.example.aguardapp.feature.mapa.domain.model.PuntoCisterna

data class CandidataCisterna(val punto: PuntoCisterna, val distanciaKm: Double?)

class ElegirCisternaConveniente {
    fun elegir(candidatas: List<CandidataCisterna>, ahora: LocalTime): CandidataCisterna? =
        candidatas
            .filter { sigueRepartiendoHoy(it.punto, ahora) }
            .minWithOrNull(compareBy(nullsLast()) { it.distanciaKm })

    fun sigueRepartiendoHoy(punto: PuntoCisterna, ahora: LocalTime): Boolean =
        punto.estado != EstadoCisterna.TERMINADO && ahora < punto.horarioFin

    fun yaEmpezo(punto: PuntoCisterna, ahora: LocalTime): Boolean = ahora >= punto.horarioInicio
}
