package com.example.aguardapp.feature.liga.presentation

import androidx.compose.ui.graphics.Color

val Oro = Color(0xFFF4C430)
val Plata = Color(0xFFC0C8D0)
val Bronce = Color(0xFFD08A4E)
val VerdeLiga = Color(0xFF2E9E6B)
val GrisLiga = Color(0xFFE5EFF7)

fun colorDeMedalla(puesto: Int?): Color = when (puesto) {
    1 -> Oro
    2 -> Plata
    3 -> Bronce
    else -> GrisLiga
}
