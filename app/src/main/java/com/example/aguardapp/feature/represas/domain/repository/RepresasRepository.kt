package com.example.aguardapp.feature.represas.domain.repository

import kotlinx.coroutines.flow.Flow
import com.example.aguardapp.feature.represas.domain.model.Presa

interface RepresasRepository {
    /** Las represas guardadas, de la más grande a la más chica. */
    val presas: Flow<List<Presa>>

    /** Trae los datos de la ANA y los guarda. Devuelve true si se pudo hablar con la ANA. */
    suspend fun actualizar(): Boolean
}
