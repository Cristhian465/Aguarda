package com.example.aguardapp.feature.sensor.domain.model

import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/** Última medición del sensor ultrasónico instalado en el tanque. */
@OptIn(ExperimentalTime::class)
data class LecturaSensor(
    val porcentaje: Double,
    val distanciaCm: Double?,
    val medidaEn: Instant
) {
    init {
        require(porcentaje in 0.0..100.0) { "Porcentaje fuera de rango: $porcentaje" }
    }

    fun litros(capacidadLitros: Double): Double = capacidadLitros * porcentaje / 100
}
