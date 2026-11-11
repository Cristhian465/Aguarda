package com.example.aguardapp.feature.sensor.domain.repository

import kotlinx.coroutines.flow.Flow
import com.example.aguardapp.feature.sensor.domain.model.LecturaSensor

interface SensorRepository {
    val estaConfigurado: Boolean

    fun observarUltimaLectura(): Flow<LecturaSensor?>

    suspend fun actualizarDesdeApi(): Result<LecturaSensor>
}
