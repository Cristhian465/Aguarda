package com.example.aguardapp.feature.sensor.domain.repository

import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import com.example.aguardapp.feature.sensor.domain.model.LecturaSensor

@OptIn(ExperimentalTime::class)
interface SensorRepository {
    val estaConfigurado: Boolean

    fun observarUltimaLectura(): Flow<LecturaSensor?>

    suspend fun actualizarDesdeApi(): Result<LecturaSensor>

    /** Lecturas que el sensor publicó entre dos momentos; sirve para buscar fugas de madrugada. */
    suspend fun lecturasEntre(desde: Instant, hasta: Instant): Result<List<LecturaSensor>>
}
