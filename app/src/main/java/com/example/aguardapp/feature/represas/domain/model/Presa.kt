package com.example.aguardapp.feature.represas.domain.model

// modelo de represa
data class Presa(
    val nombre: String,
    val distrito: String,
    val uso: String,
    val volumenHm3: Double?,
    val latitud: Double,
    val longitud: Double
)
