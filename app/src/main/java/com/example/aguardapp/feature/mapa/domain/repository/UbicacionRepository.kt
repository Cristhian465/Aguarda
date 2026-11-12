package com.example.aguardapp.feature.mapa.domain.repository

import com.example.aguardapp.feature.mapa.domain.model.Coordenada

interface UbicacionRepository {
    /** Ubicación actual del teléfono, o `null` si no hay permiso, está apagada o no se obtuvo a tiempo. */
    suspend fun ubicacionActual(): Coordenada?
}
