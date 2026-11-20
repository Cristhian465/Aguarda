package com.example.aguardapp.feature.mapa.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface ReportesApi {
    @POST("rest/v1/rpc/enviar_reporte")
    suspend fun enviar(
        @Header("apikey") clave: String,
        @Header("Authorization") autorizacion: String,
        @Body cuerpo: EnviarReporteDto
    ): Response<Unit>

    @POST("rest/v1/rpc/reportes_recientes")
    suspend fun recientes(
        @Header("apikey") clave: String,
        @Header("Authorization") autorizacion: String,
        @Body cuerpo: ReportesRecientesDto
    ): Response<List<ReporteRecienteDto>>
}

data class EnviarReporteDto(
    @SerializedName("p_token") val token: String,
    @SerializedName("p_tipo") val tipo: String,
    @SerializedName("p_ubigeo") val ubigeo: String,
    @SerializedName("p_punto") val punto: String?
)

data class ReportesRecientesDto(@SerializedName("p_horas") val horas: Int)

data class ReporteRecienteDto(
    @SerializedName("tipo") val tipo: String,
    @SerializedName("ubigeo") val ubigeo: String,
    @SerializedName("punto_id") val puntoId: String?,
    @SerializedName("hogares") val hogares: Int,
    @SerializedName("hace_minutos") val haceMinutos: Int
)
