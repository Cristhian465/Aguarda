package com.example.aguardapp.feature.mapa.presentation

import androidx.compose.ui.graphics.Color

private val COLORES = mapOf(
    "230101" to Color(0xFF26A69A),
    "230102" to Color(0xFFAB47BC),
    "230104" to Color(0xFFFFEE58),
    "230108" to Color(0xFF9CCC65),
    "230110" to Color(0xFFF06292)
)

private val COLOR_DESCONOCIDO = Color(0xFF9AAAAE)

fun colorDelDistrito(ubigeo: String): Color = COLORES[ubigeo] ?: COLOR_DESCONOCIDO

internal fun nombreCorto(nombre: String): String =
    if (nombre.startsWith("Coronel Gregorio Albarracín")) "Gregorio Albarracín" else nombre
