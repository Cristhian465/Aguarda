package com.example.aguardapp.feature.sensor.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lectura_sensor")
data class LecturaSensorEntity(
    @PrimaryKey val id: Int = 0,
    val porcentaje: Double,
    val distanciaCm: Double?,
    val medidaEnMillis: Long
)
