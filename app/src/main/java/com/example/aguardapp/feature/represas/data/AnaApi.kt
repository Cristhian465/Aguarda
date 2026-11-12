package com.example.aguardapp.feature.represas.data

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

const val URL_ANA = "https://geosnirh.ana.gob.pe/server/rest/services/"

/** Inventario de presas de la Autoridad Nacional del Agua (ANA). Es público y no necesita clave. */
interface AnaApi {
    @GET("ONRH/Inventario_de_Presas/MapServer/0/query")
    suspend fun presasDeTacna(
        @Query("where") filtro: String = "DEPARTAMEN='TACNA'",
        @Query("outFields") campos: String = "NOM_PRESA,DISTRITO,VOL_UT_HM3,USO_EMBALS",
        @Query("outSR") coordenadas: Int = 4326, // latitud y longitud
        @Query("f") formato: String = "json"
    ): RespuestaAna
}

data class RespuestaAna(val features: List<PresaAna>?)
data class PresaAna(val attributes: DatosAna, val geometry: PuntoAna?)
data class PuntoAna(val x: Double, val y: Double) // x = longitud, y = latitud
data class DatosAna(
    @SerializedName("NOM_PRESA") val nombre: String?,
    @SerializedName("DISTRITO") val distrito: String?,
    @SerializedName("VOL_UT_HM3") val volumen: String?, // texto: "12.5" o "No disponible"
    @SerializedName("USO_EMBALS") val uso: String?
)

/** Convierte lo que manda la ANA en una [Presa]. Si falta el nombre o la ubicación, se descarta. */
fun PresaAna.aPresa(): Presa? {
    val nombre = attributes.nombre?.trim() ?: return null
    val punto = geometry ?: return null
    val uso = attributes.uso.orEmpty().lowercase()
    return Presa(
        nombre = nombre.lowercase().split(" ").joinToString(" ") { it.replaceFirstChar(Char::titlecase) },
        distrito = attributes.distrito?.trim().orEmpty(),
        uso = when {
            "sumin" in uso -> "Agua potable y riego"
            "miner" in uso -> "Minería"
            else -> "Riego"
        },
        volumenHm3 = attributes.volumen?.toDoubleOrNull(),
        latitud = punto.y,
        longitud = punto.x
    )
}
