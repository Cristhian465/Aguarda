package com.example.aguardapp.feature.mapa.domain.repository

import com.example.aguardapp.feature.mapa.domain.model.Coordenada
import com.example.aguardapp.feature.mapa.domain.model.PuntoCisterna

interface MapaRepository {
    /** Punto donde se centra el mapa al abrirlo. */
    fun centroDelMapa(): Coordenada

    suspend fun obtenerPuntosCisterna(): List<PuntoCisterna>
}
