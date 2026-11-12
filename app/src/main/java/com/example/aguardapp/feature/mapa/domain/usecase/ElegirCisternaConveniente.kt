package com.example.aguardapp.feature.mapa.domain.usecase

import kotlinx.datetime.LocalTime
import com.example.aguardapp.feature.mapa.domain.model.EstadoCisterna
import com.example.aguardapp.feature.mapa.domain.model.PuntoCisterna

/** Punto de cisterna con su distancia al usuario, o `null` si no se conoce su ubicación. */
data class CandidataCisterna(val punto: PuntoCisterna, val distanciaKm: Double?)

/**
 * Elige la cisterna a la que conviene ir hoy: la más cercana que todavía no terminó su reparto.
 * Sin distancias, respeta el orden recibido.
 */
class ElegirCisternaConveniente {

    fun elegir(candidatas: List<CandidataCisterna>, ahora: LocalTime): CandidataCisterna? =
        candidatas
            .filter { sigueRepartiendoHoy(it.punto, ahora) }
            .minWithOrNull(compareBy(nullsLast()) { it.distanciaKm })

    /** `true` si ya reparte o aún no empieza; `false` si terminó o ya pasó su hora de fin. */
    fun sigueRepartiendoHoy(punto: PuntoCisterna, ahora: LocalTime): Boolean =
        punto.estado != EstadoCisterna.TERMINADO && ahora < punto.horarioFin

    fun yaEmpezo(punto: PuntoCisterna, ahora: LocalTime): Boolean = ahora >= punto.horarioInicio
}
