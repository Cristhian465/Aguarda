package com.example.aguardapp.feature.mapa.domain.model

enum class TipoReporte(val clave: String, val deCisterna: Boolean) {
    SIN_AGUA("sin_agua", false),
    VOLVIO_AGUA("volvio_agua", false),
    CISTERNA_LLEGO("cisterna_llego", true),
    CISTERNA_SE_FUE("cisterna_se_fue", true);

    companion object {
        fun deClave(clave: String): TipoReporte? = entries.firstOrNull { it.clave == clave }
    }
}

data class ReporteVecinos(
    val tipo: TipoReporte,
    val ubigeo: String,
    val puntoId: String?,
    val hogares: Int,
    val haceMinutos: Int
)
