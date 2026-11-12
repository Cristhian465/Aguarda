package com.example.aguardapp.feature.represas.domain.model

/** Una represa de Tacna, tal como la usa la app (sin depender de Room ni de la ANA). */
data class Presa(
    val nombre: String,
    val distrito: String,
    val uso: String,
    val volumenHm3: Double?,
    val latitud: Double,
    val longitud: Double
)
