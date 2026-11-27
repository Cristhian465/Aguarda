package com.example.aguardapp.feature.mapa.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header

interface CisternasApi {
    @GET("rest/v1/punto_cisterna?select=id,nombre,latitud,longitud,horario_inicio,horario_fin,estado&order=id.asc")
    suspend fun puntos(
        @Header("apikey") clave: String,
        @Header("Authorization") autorizacion: String
    ): Response<List<PuntoCisternaDto>>
}

data class PuntoCisternaDto(
    @SerializedName("id") val id: String?,
    @SerializedName("nombre") val nombre: String?,
    @SerializedName("latitud") val latitud: Double?,
    @SerializedName("longitud") val longitud: Double?,
    @SerializedName("horario_inicio") val horarioInicio: String?,
    @SerializedName("horario_fin") val horarioFin: String?,
    @SerializedName("estado") val estado: String?
)
