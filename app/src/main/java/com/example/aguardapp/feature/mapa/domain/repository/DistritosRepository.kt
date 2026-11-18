package com.example.aguardapp.feature.mapa.domain.repository

import com.example.aguardapp.feature.mapa.domain.model.Distrito

interface DistritosRepository {
    suspend fun obtenerDistritos(): List<Distrito>
}
