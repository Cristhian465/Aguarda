package com.example.aguardapp.feature.sensor.data

import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import com.example.aguardapp.feature.sensor.data.local.LecturaSensorDao
import com.example.aguardapp.feature.sensor.data.local.LecturaSensorEntity
import com.example.aguardapp.feature.sensor.data.remote.ThingSpeakApi
import com.example.aguardapp.feature.sensor.domain.model.LecturaSensor
import com.example.aguardapp.feature.sensor.domain.repository.SensorRepository

class SensorRepositoryImpl(
    private val api: ThingSpeakApi,
    private val dao: LecturaSensorDao
) : SensorRepository {
    @Volatile private var historialEnCache: Pair<String, List<LecturaSensor>>? = null

    override fun observarUltimaLectura(): Flow<LecturaSensor?> = dao.observar().map { it?.aDominio() }

    override suspend fun actualizarDesdeApi(): Result<LecturaSensor> {
        return try {
            val lectura = api.ultimaLectura()
                ?: return Result.failure(IllegalStateException(SIN_DATOS))
            dao.guardar(lectura.aEntidad())
            Result.success(lectura)
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Result.failure(IllegalStateException("Sin conexión con el sensor", e))
        } catch (e: IllegalStateException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(IllegalStateException(SIN_DATOS, e))
        }
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun lecturasEntre(desde: Instant, hasta: Instant): Result<List<LecturaSensor>> {
        val periodo = "$desde/$hasta"
        historialEnCache?.takeIf { it.first == periodo }?.let { return Result.success(it.second) }
        return try {
            val lecturas = api.historial(desde.paraThingSpeak(), hasta.paraThingSpeak())
            historialEnCache = periodo to lecturas
            Result.success(lecturas)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(IllegalStateException("Sin historial del sensor", e))
        }
    }

    private companion object {
        const val SIN_DATOS = "El sensor aún no envió datos"
    }
}

@OptIn(ExperimentalTime::class)
private fun Instant.paraThingSpeak(): String {
    val utc = toLocalDateTime(TimeZone.UTC)
    fun dos(numero: Int) = numero.toString().padStart(2, '0')
    return "${utc.date} ${dos(utc.hour)}:${dos(utc.minute)}:${dos(utc.second)}"
}

@OptIn(ExperimentalTime::class)
private fun LecturaSensor.aEntidad() =
    LecturaSensorEntity(porcentaje = porcentaje, distanciaCm = distanciaCm, medidaEnMillis = medidaEn.toEpochMilliseconds())

@OptIn(ExperimentalTime::class)
private fun LecturaSensorEntity.aDominio() =
    LecturaSensor(porcentaje, distanciaCm, Instant.fromEpochMilliseconds(medidaEnMillis))
