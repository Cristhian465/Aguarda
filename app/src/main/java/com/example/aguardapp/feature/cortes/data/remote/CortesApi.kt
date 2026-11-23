package com.example.aguardapp.feature.cortes.data.remote

import com.example.aguardapp.feature.cortes.domain.CorteAgua
import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

interface CortesApi {
    @GET("avisos?ambito=provincia-tacna")
    suspend fun obtenerAvisos(
        @Query("sector") sector: String?,
        @Query("categoria") categoria: String?,
        @Query("periodo") periodo: String,
        @Query("buscar") buscar: String?,
        @Query("limite") limite: Int,
        @Query("desde") desde: Int
    ): List<CorteDto>
}

data class CorteDto(
    val id: String,
    val titulo: String,
    val fuente: String,
    val url: String,
    val tipo: String,
    val distrito: String?,
    val sector: String?,
    val motivo: String?,
    val inicio: String?,
    @SerializedName("reposicion_estimada") val reposicionEstimada: String?,
    val restablecido: String?,
    val publicado: String?,
    val categoria: String?,
    val detalle: String?,
    val distritos: List<String>?,
    val sectores: List<String>?,
    @SerializedName("sector_origen") val sectorOrigen: String?
) {
    fun aDominio() = CorteAgua(
        id, titulo, fuente, url, tipo, distrito, sector, motivo, inicio, reposicionEstimada, restablecido, publicado,
        categoria ?: "CORTE", detalle, distritos ?: listOfNotNull(distrito), sectores.orEmpty(), sectorOrigen
    )
}
