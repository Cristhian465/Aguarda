package com.example.aguardapp.feature.mapa.presentation

import com.example.aguardapp.feature.mapa.domain.model.ReporteVecinos
import com.example.aguardapp.feature.mapa.domain.model.TipoReporte

fun avisoDeDistrito(reportes: List<ReporteVecinos>, ubigeo: String): String? =
    reportes.filter { it.ubigeo == ubigeo && !it.tipo.deCisterna }.minByOrNull { it.haceMinutos }?.let(::textoDeAviso)

fun avisoDeCisterna(reportes: List<ReporteVecinos>, puntoId: String): String? =
    reportes.filter { it.puntoId == puntoId }.minByOrNull { it.haceMinutos }?.let(::textoDeAviso)

internal fun textoDeAviso(reporte: ReporteVecinos): String {
    val vecinos = if (reporte.hogares == 1) "1 vecino" else "${reporte.hogares} vecinos"
    val cuando = textoHace(reporte.haceMinutos)
    return when (reporte.tipo) {
        TipoReporte.SIN_AGUA -> "$vecinos reportan sin agua · $cuando"
        TipoReporte.VOLVIO_AGUA -> "$vecinos dicen que ya volvió el agua · $cuando"
        TipoReporte.CISTERNA_LLEGO -> "$vecinos dicen que llegó la cisterna · $cuando"
        TipoReporte.CISTERNA_SE_FUE -> "$vecinos dicen que la cisterna ya se fue · $cuando"
    }
}

internal fun textoHace(minutos: Int): String = when {
    minutos < 1 -> "hace un momento"
    minutos < MINUTOS_POR_HORA -> "hace $minutos min"
    else -> "hace ${minutos / MINUTOS_POR_HORA} h"
}

fun nombreDelReporte(tipo: TipoReporte): String = when (tipo) {
    TipoReporte.SIN_AGUA -> "Sin agua en mi cuadra"
    TipoReporte.VOLVIO_AGUA -> "Ya llegó el agua a mi cuadra"
    TipoReporte.CISTERNA_LLEGO -> "Llegó la cisterna"
    TipoReporte.CISTERNA_SE_FUE -> "La cisterna ya se fue"
}

private const val MINUTOS_POR_HORA = 60
