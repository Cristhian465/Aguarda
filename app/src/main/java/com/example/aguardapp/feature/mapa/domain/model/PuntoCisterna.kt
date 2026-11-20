package com.example.aguardapp.feature.mapa.domain.model

import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.datetime.LocalTime

data class Coordenada(val latitud: Double, val longitud: Double) {
    init {
        require(latitud in -90.0..90.0) { "Latitud fuera de rango: $latitud" }
        require(longitud in -180.0..180.0) { "Longitud fuera de rango: $longitud" }
    }

    fun distanciaKmHasta(otra: Coordenada): Double {
        val dLat = radianes(otra.latitud - latitud)
        val dLon = radianes(otra.longitud - longitud)
        val a = sin(dLat / 2).pow(2) +
            cos(radianes(latitud)) * cos(radianes(otra.latitud)) * sin(dLon / 2).pow(2)
        return 2 * RADIO_TIERRA_KM * asin(sqrt(a))
    }

    private fun radianes(grados: Double): Double = grados * PI / 180

    private companion object {
        const val RADIO_TIERRA_KM = 6371.0
    }
}

enum class EstadoCisterna { ACTIVO, EN_RUTA, TERMINADO }

data class PuntoCisterna(
    val id: String,
    val nombre: String,
    val ubicacion: Coordenada,
    val horarioInicio: LocalTime,
    val horarioFin: LocalTime,
    val estado: EstadoCisterna
)
