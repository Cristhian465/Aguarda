package com.example.aguardapp.feature.mapa.domain.repository

import com.example.aguardapp.feature.mapa.domain.model.Coordenada

interface UbicacionRepository {
    suspend fun ubicacionActual(): Coordenada?
}
