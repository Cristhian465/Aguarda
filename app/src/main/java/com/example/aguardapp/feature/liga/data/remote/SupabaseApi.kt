package com.example.aguardapp.feature.liga.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/** Funciones de `supabase/liga_del_agua.sql`. La app no tiene acceso directo a la tabla. */
interface SupabaseApi {

    @POST("rest/v1/rpc/enviar_resumen")
    suspend fun enviarResumen(
        @Header("apikey") clave: String,
        @Header("Authorization") autorizacion: String,
        @Body cuerpo: EnviarResumenDto
    ): Response<Unit>

    @POST("rest/v1/rpc/ranking_semana")
    suspend fun rankingSemana(
        @Header("apikey") clave: String,
        @Header("Authorization") autorizacion: String,
        @Body cuerpo: RankingSemanaDto
    ): Response<List<PosicionDistritoDto>>

    @POST("rest/v1/rpc/borrar_mis_datos")
    suspend fun borrarMisDatos(
        @Header("apikey") clave: String,
        @Header("Authorization") autorizacion: String,
        @Body cuerpo: BorrarDatosDto
    ): Response<Unit>
}

data class EnviarResumenDto(
    @SerializedName("p_token") val token: String,
    @SerializedName("p_semana") val semana: String,
    @SerializedName("p_ubigeo") val ubigeo: String,
    @SerializedName("p_litros") val litros: Double,
    @SerializedName("p_puntaje") val puntaje: Int,
    @SerializedName("p_verificado") val verificado: Boolean
)

data class RankingSemanaDto(@SerializedName("p_semana") val semana: String)

data class BorrarDatosDto(@SerializedName("p_token") val token: String)

data class PosicionDistritoDto(
    @SerializedName("ubigeo") val ubigeo: String,
    @SerializedName("hogares") val hogares: Int,
    @SerializedName("verificados") val verificados: Int,
    @SerializedName("puntaje") val puntaje: Double?,
    @SerializedName("litros_persona_dia") val litrosPersonaDia: Double?
)
