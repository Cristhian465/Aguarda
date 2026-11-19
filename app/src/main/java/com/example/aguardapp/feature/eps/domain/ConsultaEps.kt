package com.example.aguardapp.feature.eps.domain

import com.example.aguardapp.feature.liga.domain.MesRecibo

data class CuentaEps(val usuario: String)

/** Total a pagar del documento, que puede incluir deuda; no equivale al cargo del mes. */
data class ReciboEps(val mes: MesRecibo, val metrosCubicos: Double, val totalPagar: Double)

interface ConsultaEps {
    fun cuenta(): CuentaEps?
    suspend fun vincular(usuario: String, clave: String)
    suspend fun actualizar(): ReciboEps
    suspend fun desvincular()
}

class ErrorEps(val detalle: String, val requiereVincular: Boolean = false) : Exception(detalle)
