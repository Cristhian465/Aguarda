package com.example.aguardapp.feature.represas.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Tabla de Room con las represas guardadas para verlas sin internet. */
@Entity(tableName = "presa")
data class PresaEntity(
    @PrimaryKey val nombre: String,
    val distrito: String,
    val uso: String,
    val volumenHm3: Double?,
    val latitud: Double,
    val longitud: Double
)
