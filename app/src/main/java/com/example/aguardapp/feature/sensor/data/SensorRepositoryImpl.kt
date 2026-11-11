package com.example.aguardapp.feature.sensor.data

import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import com.example.aguardapp.feature.sensor.data.local.LecturaSensorDao
import com.example.aguardapp.feature.sensor.data.local.LecturaSensorEntity
import com.example.aguardapp.feature.sensor.data.remote.EntradaThingSpeakDto
import com.example.aguardapp.feature.sensor.data.remote.ThingSpeakApi
import com.example.aguardapp.feature.sensor.domain.model.LecturaSensor
import com.example.aguardapp.feature.sensor.domain.repository.SensorRepository

class SensorRepositoryImpl(
    private val api: ThingSpeakApi,
    private val dao: LecturaSensorDao,
    private val canal: String,
    private val claveLectura: String
) : SensorRepository {
    override val estaConfigurado: Boolean get() = canal.isNotBlank()

    override fun observarUltimaLectura(): Flow<LecturaSensor?> = dao.observar().map { it?.aDominio() }

    override suspend fun actualizarDesdeApi(): Result<LecturaSensor> {
        return try {
            val lectura = api.ultimaEntrada(canal, claveLectura.ifBlank { null }).aLectura()
                ?: return Result.failure(IllegalStateException(SIN_DATOS))
            dao.guardar(lectura.aEntidad())
            Result.success(lectura)
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Result.failure(IllegalStateException("Sin conexión con el sensor", e))
        } catch (e: HttpException) {
            Result.failure(IllegalStateException("El servidor del sensor respondió con error ${e.code()}", e))
        } catch (e: Exception) {
            Result.failure(IllegalStateException(SIN_DATOS, e))
        }
    }

    private companion object {
        const val SIN_DATOS = "El sensor aún no envió datos"
    }
}

@OptIn(ExperimentalTime::class)
private fun EntradaThingSpeakDto.aLectura(): LecturaSensor? {
    val porcentaje = porcentaje?.toDoubleOrNull()?.coerceIn(0.0, 100.0) ?: return null
    val momento = creadaEn?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: return null
    return LecturaSensor(porcentaje, distanciaCm?.toDoubleOrNull(), momento)
}

@OptIn(ExperimentalTime::class)
private fun LecturaSensor.aEntidad() =
    LecturaSensorEntity(porcentaje = porcentaje, distanciaCm = distanciaCm, medidaEnMillis = medidaEn.toEpochMilliseconds())

@OptIn(ExperimentalTime::class)
private fun LecturaSensorEntity.aDominio() =
    LecturaSensor(porcentaje, distanciaCm, Instant.fromEpochMilliseconds(medidaEnMillis))
