package com.example.aguardapp.feature.represas.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

const val URL_NOMINATIM = "https://nominatim.openstreetmap.org/"

/**
 * Buscador de direcciones de OpenStreetMap: convierte coordenadas en una dirección.
 * Sus reglas de uso piden identificar la app (cabecera User-Agent) y no más de una consulta por segundo.
 */
interface NominatimApi {

    @GET("reverse")
    suspend fun direccion(
        @Query("lat") latitud: Double,
        @Query("lon") longitud: Double,
        @Query("format") formato: String = "jsonv2",
        @Query("accept-language") idioma: String = "es"
    ): DireccionDto
}

/** En Tacna, el distrito llega en `city` (por ejemplo "Coronel Gregorio Albarracín Lanchipa"). */
data class DireccionDto(
    @SerializedName("address") val partes: PartesDireccionDto?,
    @SerializedName("error") val error: String?
)

data class PartesDireccionDto(
    @SerializedName("city") val ciudad: String?,
    @SerializedName("town") val pueblo: String?,
    @SerializedName("village") val caserio: String?,
    @SerializedName("suburb") val sector: String?
)
