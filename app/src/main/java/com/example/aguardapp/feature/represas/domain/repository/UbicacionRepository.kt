package com.example.aguardapp.feature.represas.domain.repository

import com.example.aguardapp.feature.mapa.domain.model.Coordenada

/** El GPS del teléfono. El permiso lo pide la pantalla; aquí solo se lee la ubicación. */
interface UbicacionRepository {
    /** La ubicación actual, o null si no hay permiso, la ubicación está apagada o no respondió a tiempo. */
    suspend fun ubicacionActual(): Coordenada?
}
