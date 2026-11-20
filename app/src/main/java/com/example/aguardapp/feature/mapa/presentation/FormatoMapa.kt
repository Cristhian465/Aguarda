package com.example.aguardapp.feature.mapa.presentation

import com.example.aguardapp.feature.deposito.presentation.formatearHora
import com.example.aguardapp.feature.mapa.domain.model.EstadoCisterna
import com.example.aguardapp.feature.mapa.domain.model.PuntoCisterna

fun detalleDe(punto: PuntoCisterna): String =
    "${formatearHora(punto.horarioInicio)} – ${formatearHora(punto.horarioFin)} · ${nombreDelEstado(punto.estado)}"

fun nombreDelEstado(estado: EstadoCisterna): String = when (estado) {
    EstadoCisterna.ACTIVO -> "Activo"
    EstadoCisterna.EN_RUTA -> "En ruta"
    EstadoCisterna.TERMINADO -> "Terminado"
}

fun formatearDistancia(km: Double): String {
    val metros = (Math.round(km * 100) * 10).coerceAtLeast(10)
    if (metros < 1000) return "a $metros m"
    val decimas = Math.round(km * 10)
    return "a ${decimas / 10}.${decimas % 10} km"
}
