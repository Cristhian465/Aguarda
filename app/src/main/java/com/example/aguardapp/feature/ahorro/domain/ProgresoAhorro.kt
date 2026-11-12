package com.example.aguardapp.feature.ahorro.domain

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

data class RetoAhorro(val id: String, val titulo: String, val litrosEstimados: Int)

val retosDeAhorro = listOf(
    RetoAhorro("carga-completa", "Lavar ropa solo con carga completa", 90),
    RetoAhorro("ducha-corta", "Duchas de 5 minutos toda la semana", 210),
    RetoAhorro("reusar-agua", "Reutilizar el agua del enjuague", 60)
)

fun calcularRacha(diasCumplidos: Set<LocalDate>, hoy: LocalDate): Int {
    var fecha = if (hoy in diasCumplidos) hoy else hoy.minus(1, DateTimeUnit.DAY)
    var dias = 0
    while (fecha in diasCumplidos) {
        dias++
        fecha = fecha.minus(1, DateTimeUnit.DAY)
    }
    return dias
}
