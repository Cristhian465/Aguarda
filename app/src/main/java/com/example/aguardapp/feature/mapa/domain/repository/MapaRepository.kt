package com.example.aguardapp.feature.mapa.domain.repository

import com.example.aguardapp.feature.mapa.domain.model.Coordenada
import com.example.aguardapp.feature.mapa.domain.model.PuntoCisterna

interface MapaRepository {
    fun centroDelMapa(): Coordenada

    suspend fun obtenerPuntosCisterna(): List<PuntoCisterna>
}
