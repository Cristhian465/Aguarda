package com.example.aguardapp.feature.eps.domain

data class CuentaEps(val usuario: String)

data class ReciboEps(
    val mes: MesRecibo,
    val metrosCubicos: Double,
    val totalPagar: Double,
    val diasFacturados: Int? = null,
    val totalMes: Double? = null,
    val deuda: Double? = null
)

interface ConsultaEps {
    fun cuenta(): CuentaEps?
    suspend fun vincular(usuario: String, clave: String)
    suspend fun descargarUltimoPdf(): ByteArray
    suspend fun actualizar(): ReciboEps
    suspend fun consultarHistorial(): List<ReciboEps>
    suspend fun desvincular()
}

class ErrorEps(val detalle: String, val requiereVincular: Boolean = false) : Exception(detalle)
