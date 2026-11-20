package com.example.aguardapp.feature.sensor.domain.repository

import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import com.example.aguardapp.feature.sensor.domain.model.LecturaSensor

@OptIn(ExperimentalTime::class)
interface SensorRepository {
    fun observarUltimaLectura(): Flow<LecturaSensor?>

    suspend fun actualizarDesdeApi(): Result<LecturaSensor>

    suspend fun lecturasEntre(desde: Instant, hasta: Instant): Result<List<LecturaSensor>>
}
