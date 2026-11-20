package com.example.aguardapp.feature.mapa.data

import com.google.gson.JsonParser
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException
import retrofit2.Response
import com.example.aguardapp.feature.liga.data.remote.CLAVE_SUPABASE
import com.example.aguardapp.feature.mapa.data.remote.EnviarReporteDto
import com.example.aguardapp.feature.mapa.data.remote.ReportesApi
import com.example.aguardapp.feature.mapa.data.remote.ReportesRecientesDto
import com.example.aguardapp.feature.mapa.domain.model.ReporteVecinos
import com.example.aguardapp.feature.mapa.domain.model.TipoReporte
import com.example.aguardapp.feature.mapa.domain.repository.ReportesRepository

private const val HORAS_RECIENTES = 6
private const val CODIGO_MENSAJE_PROPIO = "P0001"

class ReportesRepositoryImpl(
    private val api: ReportesApi,
    private val token: () -> String,
    private val clave: String = CLAVE_SUPABASE
) : ReportesRepository {
    private val autorizacion = "Bearer $clave"

    override suspend fun recientes(): Result<List<ReporteVecinos>> = llamar { servicio ->
        val respuesta = servicio.recientes(clave, autorizacion, ReportesRecientesDto(HORAS_RECIENTES))
        respuesta.exigirExito()
        respuesta.body().orEmpty().mapNotNull { dto ->
            TipoReporte.deClave(dto.tipo)?.let { ReporteVecinos(it, dto.ubigeo, dto.puntoId, dto.hogares, dto.haceMinutos) }
        }
    }

    override suspend fun enviar(tipo: TipoReporte, ubigeo: String, puntoId: String?): Result<Unit> = llamar { servicio ->
        servicio.enviar(clave, autorizacion, EnviarReporteDto(token(), tipo.clave, ubigeo, puntoId)).exigirExito()
    }

    private suspend fun <T> llamar(accion: suspend (ReportesApi) -> T): Result<T> = try {
        Result.success(accion(api))
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        Result.failure(IllegalStateException("Sin conexión. Revisa tu internet e inténtalo de nuevo", e))
    } catch (e: RuntimeException) {
        Result.failure(IllegalStateException(e.message ?: "No se pudo completar el reporte", e))
    }

    private fun Response<*>.exigirExito() {
        if (isSuccessful) return
        throw IllegalStateException(mensajeDelServidor(errorBody()?.string()) ?: "No se pudo completar el reporte (error ${code()})")
    }
}

internal fun mensajeDelServidor(cuerpo: String?): String? = runCatching {
    val json = JsonParser.parseString(cuerpo).asJsonObject
    json.get("message")?.asString?.takeIf { json.get("code")?.asString == CODIGO_MENSAJE_PROPIO }
}.getOrNull()
