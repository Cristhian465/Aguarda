package com.example.aguardapp.feature.sensor.domain.repository

import com.example.aguardapp.feature.sensor.domain.model.LecturaSensor

interface SensorRepository {
    /** `false` si no se configuró el canal del sensor: la app funciona igual, sin sensor. */
    val estaConfigurado: Boolean

    suspend fun ultimaLectura(): Result<LecturaSensor>
}
