package com.example.aguardapp.feature.mapa.domain.model

/** Punto de cisterna con su distancia a la ubicación del usuario. */
data class CisternaCercana(
    val punto: PuntoCisterna,
    val distanciaKm: Double
)
