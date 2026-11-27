package com.example.aguardapp.feature.notificaciones.data

import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import com.example.aguardapp.feature.ahorro.data.AhorroLocal
import com.example.aguardapp.feature.ahorro.domain.calcularRacha
import com.example.aguardapp.feature.cortes.data.PreferenciasCortes
import com.example.aguardapp.feature.cortes.domain.CortesRepository
import com.example.aguardapp.feature.cortes.domain.FiltrosAvisos
import com.example.aguardapp.feature.cortes.domain.PeriodoAviso
import com.example.aguardapp.feature.deposito.domain.repository.DepositoRepository
import com.example.aguardapp.feature.notificaciones.domain.model.AvisoReciente
import com.example.aguardapp.feature.notificaciones.domain.model.EstadoRacha
import com.example.aguardapp.feature.notificaciones.domain.model.LecturaNivel
import com.example.aguardapp.feature.notificaciones.domain.repository.FuentesNotificaciones
import com.example.aguardapp.feature.sensor.domain.repository.SensorRepository

private val LECTURA_VIGENTE = 2.hours

class FuentesNotificacionesImpl(
    private val sensor: SensorRepository,
    private val deposito: DepositoRepository,
    private val cortes: CortesRepository,
    private val preferenciasCortes: PreferenciasCortes,
    private val ahorro: AhorroLocal,
    private val ahora: () -> LocalDateTime
) : FuentesNotificaciones {

    @OptIn(ExperimentalTime::class)
    override suspend fun nivelActual(): LecturaNivel? {
        val lectura = sensor.actualizarDesdeApi().getOrNull()
        if (lectura != null && Clock.System.now() - lectura.medidaEn < LECTURA_VIGENTE) {
            return LecturaNivel(lectura.porcentaje, delSensor = true)
        }
        val estimado = deposito.observarDeposito().first()?.nivelEn(ahora()) ?: return null
        return LecturaNivel(estimado.porcentaje, delSensor = false)
    }

    override suspend fun avisoMasReciente(): AvisoReciente? {
        if (!cortes.configurado) return null
        val filtros = FiltrosAvisos(sector = preferenciasCortes.sectorEps, periodo = PeriodoAviso.SEMANA)
        return cortes.obtenerAvisos(filtros, desde = 0).firstOrNull()?.let { AvisoReciente(it.id, it.titulo, it.fuente) }
    }

    override fun racha(hoy: LocalDate): EstadoRacha {
        val cumplidos = ahorro.historial().filterValues { it.isNotEmpty() }.keys
        return EstadoRacha(calcularRacha(cumplidos, hoy), cumplidaHoy = hoy in cumplidos)
    }
}
