package com.example.aguardapp.feature.familia.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/** Funciones de supabase/modo_familia.sql. */
interface FamiliaApi {
    @POST("rest/v1/rpc/familia_crear")
    suspend fun crear(
        @Header("apikey") clave: String,
        @Header("Authorization") autorizacion: String,
        @Body cuerpo: CrearFamiliaDto
    ): Response<Unit>

    @POST("rest/v1/rpc/familia_invitar")
    suspend fun invitar(
        @Header("apikey") clave: String,
        @Header("Authorization") autorizacion: String,
        @Body cuerpo: MiembroDto
    ): Response<InvitacionDto>

    @POST("rest/v1/rpc/familia_unirse")
    suspend fun unirse(
        @Header("apikey") clave: String,
        @Header("Authorization") autorizacion: String,
        @Body cuerpo: UnirseDto
    ): Response<UnionDto>

    @POST("rest/v1/rpc/familia_sincronizar")
    suspend fun sincronizar(
        @Header("apikey") clave: String,
        @Header("Authorization") autorizacion: String,
        @Body cuerpo: SincronizarDto
    ): Response<SincronizacionDto>

    @POST("rest/v1/rpc/familia_salir")
    suspend fun salir(
        @Header("apikey") clave: String,
        @Header("Authorization") autorizacion: String,
        @Body cuerpo: MiembroDto
    ): Response<Unit>

    @POST("rest/v1/rpc/familia_reiniciar")
    suspend fun reiniciar(
        @Header("apikey") clave: String,
        @Header("Authorization") autorizacion: String,
        @Body cuerpo: ReiniciarDto
    ): Response<Unit>
}

data class CrearFamiliaDto(
    @SerializedName("p_secreto") val secreto: String,
    @SerializedName("p_miembro") val miembro: String,
    @SerializedName("p_apodo") val apodo: String,
    @SerializedName("p_perfil") val perfil: PerfilFamiliaDto?
)

data class MiembroDto(
    @SerializedName("p_secreto") val secreto: String,
    @SerializedName("p_miembro") val miembro: String
)

data class UnirseDto(
    @SerializedName("p_codigo") val codigo: String,
    @SerializedName("p_miembro") val miembro: String,
    @SerializedName("p_apodo") val apodo: String
)

data class ReiniciarDto(
    @SerializedName("p_secreto") val secreto: String,
    @SerializedName("p_nuevo") val nuevo: String,
    @SerializedName("p_miembro") val miembro: String,
    @SerializedName("p_apodo") val apodo: String
)

data class SincronizarDto(
    @SerializedName("p_secreto") val secreto: String,
    @SerializedName("p_miembro") val miembro: String,
    @SerializedName("p_eventos") val eventos: List<EventoFamiliaDto>,
    @SerializedName("p_desde") val desde: String,
    @SerializedName("p_perfil") val perfil: PerfilFamiliaDto?
)

data class InvitacionDto(
    @SerializedName("codigo") val codigo: String?,
    @SerializedName("minutos") val minutos: Int?
)

data class UnionDto(
    @SerializedName("secreto") val secreto: String?,
    @SerializedName("error") val error: String?
)

data class SincronizacionDto(
    @SerializedName("perfil") val perfil: PerfilFamiliaDto?,
    @SerializedName("eventos") val eventos: List<EventoFamiliaDto>?,
    @SerializedName("miembros") val miembros: List<MiembroFamiliaDto>?
)

data class EventoFamiliaDto(
    @SerializedName("id") val id: String?,
    @SerializedName("tipo") val tipo: String?,
    @SerializedName("momento") val momento: String?,
    @SerializedName("litros") val litros: Double?,
    @SerializedName("inicio_observado") val inicioObservado: String?,
    @SerializedName("autor") val autor: String? = null,
    @SerializedName("propio") val propio: Boolean? = null
)

data class MiembroFamiliaDto(
    @SerializedName("apodo") val apodo: String?,
    @SerializedName("propio") val propio: Boolean?
)

data class PerfilFamiliaDto(
    @SerializedName("tipoReservorio") val tipoReservorio: String?,
    @SerializedName("capacidadLitros") val capacidadLitros: Double?,
    @SerializedName("habitantes") val habitantes: Int?,
    @SerializedName("duchasPorDia") val duchasPorDia: Int?,
    @SerializedName("usaLavadora") val usaLavadora: Boolean?,
    @SerializedName("riegaJardin") val riegaJardin: Boolean?,
    @SerializedName("horaProximoLlenado") val horaProximoLlenado: String?,
    @SerializedName("consumoPorHabitosLitrosHora") val consumoPorHabitosLitrosHora: Double?,
    @SerializedName("consumoVigenteLitrosHora") val consumoVigenteLitrosHora: Double?
)
