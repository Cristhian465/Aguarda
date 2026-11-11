package com.example.aguardapp.feature.sensor.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/** API REST de ThingSpeak: el ESP32 escribe en el canal y la app lee el último dato. */
interface ThingSpeakApi {

    @GET("channels/{canal}/feeds/last.json")
    suspend fun ultimaEntrada(
        @Path("canal") canal: String,
        @Query("api_key") claveLectura: String?
    ): EntradaThingSpeakDto
}

/** ThingSpeak envía los valores de los campos como texto: field1 = porcentaje, field2 = distancia en cm. */
data class EntradaThingSpeakDto(
    @SerializedName("created_at") val creadaEn: String?,
    @SerializedName("field1") val porcentaje: String?,
    @SerializedName("field2") val distanciaCm: String?
)
