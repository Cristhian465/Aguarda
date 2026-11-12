package com.example.aguardapp.feature.mapa.domain.usecase

import com.example.aguardapp.feature.mapa.domain.model.CisternaCercana
import com.example.aguardapp.feature.mapa.domain.model.Coordenada
import com.example.aguardapp.feature.mapa.domain.model.PuntoCisterna

/**
 * Cisternas dentro del radio, de la más cercana a la más lejana.
 * Se incluyen las que ya terminaron su reparto: el estado se muestra aparte.
 */
class BuscarCisternasCercanas {

    fun buscar(
        puntos: List<PuntoCisterna>,
        ubicacion: Coordenada,
        radioKm: Double = RADIO_KM
    ): List<CisternaCercana> =
        puntos
            .map { CisternaCercana(it, it.ubicacion.distanciaKmHasta(ubicacion)) }
            .filter { it.distanciaKm <= radioKm }
            .sortedBy { it.distanciaKm }

    companion object {
        const val RADIO_KM = 10.0
    }
}
