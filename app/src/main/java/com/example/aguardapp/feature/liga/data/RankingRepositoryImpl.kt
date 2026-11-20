package com.example.aguardapp.feature.liga.data

import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.round
import kotlinx.datetime.LocalDate
import retrofit2.Response
import com.example.aguardapp.feature.liga.data.remote.BorrarDatosDto
import com.example.aguardapp.feature.liga.data.remote.CLAVE_SUPABASE
import com.example.aguardapp.feature.liga.data.remote.EnviarResumenDto
import com.example.aguardapp.feature.liga.data.remote.RankingSemanaDto
import com.example.aguardapp.feature.liga.data.remote.SupabaseApi
import com.example.aguardapp.feature.liga.domain.EnvioSemanal
import com.example.aguardapp.feature.liga.domain.PosicionDistrito
import com.example.aguardapp.feature.liga.domain.RankingRepository

class RankingRepositoryImpl(private val api: SupabaseApi, private val clave: String = CLAVE_SUPABASE) : RankingRepository {
    private val autorizacion = "Bearer $clave"

    override suspend fun enviar(envio: EnvioSemanal): Result<Unit> = llamar { servicio ->
        val litros = round(envio.litrosPorPersonaDia * 10) / 10
        val cuerpo = EnviarResumenDto(envio.token, envio.lunes.toString(), envio.ubigeo, litros, envio.puntaje, envio.verificado)
        servicio.enviarResumen(clave, autorizacion, cuerpo).exigirExito()
    }

    override suspend fun ranking(lunes: LocalDate): Result<List<PosicionDistrito>> = llamar { servicio ->
        val respuesta = servicio.rankingSemana(clave, autorizacion, RankingSemanaDto(lunes.toString()))
        respuesta.exigirExito()
        respuesta.body().orEmpty().map {
            PosicionDistrito(it.ubigeo, it.hogares, it.verificados, it.puntaje, it.litrosPersonaDia)
        }
    }

    override suspend fun borrarMisDatos(token: String): Result<Unit> = llamar { servicio ->
        servicio.borrarMisDatos(clave, autorizacion, BorrarDatosDto(token)).exigirExito()
    }

    private suspend fun <T> llamar(accion: suspend (SupabaseApi) -> T): Result<T> {
        return try {
            Result.success(accion(api))
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Result.failure(IllegalStateException("Sin conexión con la Liga", e))
        } catch (e: RuntimeException) {
            Result.failure(IllegalStateException(e.message ?: "La Liga no respondió bien", e))
        }
    }

    private fun Response<*>.exigirExito() {
        check(isSuccessful) { "La Liga respondió con error ${code()}" }
    }
}
