package com.example.aguardapp.feature.represas.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Una represa de Tacna. Es a la vez el modelo de la app y la tabla de Room, para no duplicar código. */
@Entity(tableName = "presa")
data class Presa(
    @PrimaryKey val nombre: String,
    val distrito: String,
    val uso: String,
    val volumenHm3: Double?,
    val latitud: Double,
    val longitud: Double
)
