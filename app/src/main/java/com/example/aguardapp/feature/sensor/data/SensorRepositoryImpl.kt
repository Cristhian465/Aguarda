package com.example.aguardapp.feature.sensor.data

import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import retrofit2.HttpException
import com.example.aguardapp.feature.sensor.data.remote.EntradaThingSpeakDto
import com.example.aguardapp.feature.sensor.data.remote.ThingSpeakApi
import com.example.aguardapp.feature.sensor.domain.model.LecturaSensor
import com.example.aguardapp.feature.sensor.domain.repository.SensorRepository

class SensorRepositoryImpl(
    private val api: ThingSpeakApi,
    private val canal: String,
    private val claveLectura: String
) : SensorRepository {

    override val estaConfigurado: Boolean get() = canal.isNotBlank()

    override suspend fun ultimaLectura(): Result<LecturaSensor> = try {
        val entrada = api.ultimaEntrada(canal, claveLectura.ifBlank { null })
        entrada.aLectura()?.let { Result.success(it) }
            ?: Result.failure(IllegalStateException("El sensor aún no envió datos"))
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        Result.failure(IllegalStateException("Sin conexión con el sensor", e))
    } catch (e: HttpException) {
        Result.failure(IllegalStateException("El servidor del sensor respondió con error ${e.code()}", e))
    } catch (e: Exception) {
        // Un canal vacío responde "-1" en lugar de un JSON.
        Result.failure(IllegalStateException("El sensor aún no envió datos", e))
    }
}

@OptIn(ExperimentalTime::class)
private fun EntradaThingSpeakDto.aLectura(): LecturaSensor? {
    val porcentaje = porcentaje?.toDoubleOrNull()?.coerceIn(0.0, 100.0) ?: return null
    val momento = creadaEn?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: return null
    return LecturaSensor(porcentaje, distanciaCm?.toDoubleOrNull(), momento)
}
