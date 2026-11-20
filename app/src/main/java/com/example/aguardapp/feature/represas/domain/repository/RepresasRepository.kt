package com.example.aguardapp.feature.represas.domain.repository

import kotlinx.coroutines.flow.Flow
import com.example.aguardapp.feature.represas.domain.model.Presa

interface RepresasRepository {
    val presas: Flow<List<Presa>>

    suspend fun actualizar(): Boolean
}
