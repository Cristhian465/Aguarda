package com.example.aguardapp.feature.represas.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

const val URL_ANA = "https://geosnirh.ana.gob.pe/server/rest/services/"

// api de la ANA
interface AnaApi {
    @GET("ONRH/Inventario_de_Presas/MapServer/0/query")
    suspend fun presasDeTacna(
        @Query("where") filtro: String = "DEPARTAMEN='TACNA'",
        @Query("outFields") campos: String = "NOM_PRESA,DISTRITO,VOL_UT_HM3,USO_EMBALS",
        @Query("outSR") coordenadas: Int = 4326, // lat y long
        @Query("f") formato: String = "json"
    ): RespuestaAna
}

data class RespuestaAna(val features: List<PresaAna>?)
data class PresaAna(val attributes: DatosAna, val geometry: PuntoAna?)
data class PuntoAna(val x: Double, val y: Double) // x = long, y = lat
data class DatosAna(
    @SerializedName("NOM_PRESA") val nombre: String?,
    @SerializedName("DISTRITO") val distrito: String?,
    @SerializedName("VOL_UT_HM3") val volumen: String?, // a veces viene "No disponible"
    @SerializedName("USO_EMBALS") val uso: String?
)
