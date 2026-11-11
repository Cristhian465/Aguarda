package com.example.aguardapp.feature.represas.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

const val URL_ANA = "https://geosnirh.ana.gob.pe/server/rest/services/"

private const val CAMPOS_DE_PRESA =
    "NOM_PRESA,RIO_PRESA,DISTRITO,ANO_FIN_OB,H_SOB_CIM,ELE_CRESTA,VOL_UT_HM3,USO_EMBALS,FINALIDAD_USO,TITUL_PROP"

/** Inventario de presas de la Autoridad Nacional del Agua (servicio ArcGIS REST público, sin clave). */
interface AnaApi {

    @GET("ONRH/Inventario_de_Presas/MapServer/0/query")
    suspend fun presas(
        @Query("where") filtro: String = "DEPARTAMEN='TACNA'",
        @Query("outFields") campos: String = CAMPOS_DE_PRESA,
        @Query("returnGeometry") conUbicacion: Boolean = true,
        @Query("outSR") sistemaDeCoordenadas: Int = 4326,  // latitud y longitud, no UTM
        @Query("f") formato: String = "json"
    ): RespuestaPresasDto
}

/** Si la consulta falla, ArcGIS responde HTTP 200 con `error` en vez de `features`. */
data class RespuestaPresasDto(
    @SerializedName("features") val presas: List<PresaDto>?,
    @SerializedName("error") val error: ErrorAnaDto?
)

data class ErrorAnaDto(
    @SerializedName("code") val codigo: Int?,
    @SerializedName("message") val mensaje: String?
)

data class PresaDto(
    @SerializedName("attributes") val atributos: AtributosPresaDto?,
    @SerializedName("geometry") val punto: PuntoDto?
)

/** x es la longitud e y la latitud. */
data class PuntoDto(
    @SerializedName("x") val x: Double?,
    @SerializedName("y") val y: Double?
)

/** Casi todos los campos llegan como texto, y los que faltan dicen "No disponible". */
data class AtributosPresaDto(
    @SerializedName("NOM_PRESA") val nombre: String?,
    @SerializedName("RIO_PRESA") val rio: String?,
    @SerializedName("DISTRITO") val distrito: String?,
    @SerializedName("ANO_FIN_OB") val anioInicio: String?,
    @SerializedName("H_SOB_CIM") val alturaMetros: String?,
    @SerializedName("ELE_CRESTA") val altitudMetros: Double?,
    @SerializedName("VOL_UT_HM3") val volumenUtilHm3: String?,
    @SerializedName("USO_EMBALS") val uso: String?,
    @SerializedName("FINALIDAD_USO") val finalidad: String?,
    @SerializedName("TITUL_PROP") val operador: String?
)
