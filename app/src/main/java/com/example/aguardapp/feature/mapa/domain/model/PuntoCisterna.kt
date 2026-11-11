package com.example.aguardapp.feature.mapa.domain.model

import kotlinx.datetime.LocalTime

data class Coordenada(val latitud: Double, val longitud: Double) {
    init {
        require(latitud in -90.0..90.0) { "Latitud fuera de rango: $latitud" }
        require(longitud in -180.0..180.0) { "Longitud fuera de rango: $longitud" }
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
