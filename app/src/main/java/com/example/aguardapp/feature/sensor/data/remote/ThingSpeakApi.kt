package com.example.aguardapp.feature.sensor.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ThingSpeakApi {
    @GET("channels/{canal}/feeds/last.json")
    suspend fun ultimaEntrada(
        @Path("canal") canal: String,
        @Query("api_key") claveLectura: String?
    ): EntradaThingSpeakDto

    /** Entradas del canal entre dos fechas en UTC, con el formato "AAAA-MM-DD HH:MM:SS". */
    @GET("channels/{canal}/feeds.json")
    suspend fun historial(
        @Path("canal") canal: String,
        @Query("api_key") claveLectura: String?,
        @Query("start") desde: String,
        @Query("end") hasta: String
    ): HistorialThingSpeakDto
}

data class EntradaThingSpeakDto(
    @SerializedName("created_at") val creadaEn: String?,
    @SerializedName("field1") val porcentaje: String?,
    @SerializedName("field2") val distanciaCm: String?
)

data class HistorialThingSpeakDto(
    @SerializedName("feeds") val entradas: List<EntradaThingSpeakDto>?
)
