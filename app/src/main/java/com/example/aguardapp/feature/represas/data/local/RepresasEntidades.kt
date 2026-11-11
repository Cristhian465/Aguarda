package com.example.aguardapp.feature.represas.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Copia local del inventario de la ANA, para mostrar las represas sin internet. */
@Entity(tableName = "presa")
data class PresaEntity(
    @PrimaryKey val nombre: String,
    val latitud: Double,
    val longitud: Double,
    val rio: String?,
    val distrito: String?,
    val volumenUtilHm3: Double?,
    val alturaMetros: Double?,
    val altitudMetros: Double?,
    val anioInicio: Int?,
    val usos: String,
    val operador: String?,
    val descargadaEn: String
)

@Entity(tableName = "distrito_hogar")
data class DistritoHogarEntity(
    @PrimaryKey val usuarioId: String,
    val distrito: String,
    val detectadoPorGps: Boolean
)
