package com.example.aguardapp.feature.widget.domain.usecase

import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.coroutines.flow.first
import com.example.aguardapp.feature.deposito.domain.repository.DepositoRepository
import com.example.aguardapp.feature.sensor.domain.model.LecturaSensor
import com.example.aguardapp.feature.sensor.domain.repository.SensorRepository
import com.example.aguardapp.feature.widget.domain.model.EstadoTanque

private val MAXIMO_SIN_SENAL = 10.minutes

@OptIn(ExperimentalTime::class)
class ObtenerEstadoTanque(
    private val sensor: SensorRepository,
    private val deposito: DepositoRepository,
    private val ahora: () -> Instant
) {
    suspend operator fun invoke(): EstadoTanque {
        val capacidad = deposito.observarPerfil().first()?.capacidad?.litros?.valor ?: 0.0
        val nueva = sensor.actualizarDesdeApi().getOrNull()
        val lectura = nueva ?: sensor.observarUltimaLectura().first()
        return estadoTanque(lectura, capacidad, conectado = nueva != null && estaConectado(nueva, ahora()))
    }
}

@OptIn(ExperimentalTime::class)
fun estaConectado(lectura: LecturaSensor, ahora: Instant): Boolean = ahora - lectura.medidaEn < MAXIMO_SIN_SENAL

fun estadoTanque(lectura: LecturaSensor?, capacidad: Double, conectado: Boolean) = EstadoTanque(
    porcentaje = lectura?.porcentaje?.roundToInt() ?: 0,
    litros = lectura?.litros(capacidad)?.roundToInt() ?: 0,
    capacidad = capacidad.roundToInt(),
    conectado = conectado
)
