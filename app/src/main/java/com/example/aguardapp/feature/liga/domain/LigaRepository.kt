package com.example.aguardapp.feature.liga.domain

import kotlinx.coroutines.flow.Flow

interface LigaRepository {
    fun observarMarcas(): Flow<List<MarcaNivel>>

    suspend fun guardarMarca(marca: MarcaNivel)
}
