package com.example.aguardapp.feature.sensor.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.roundToInt
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import com.example.aguardapp.feature.deposito.domain.model.Litros
import com.example.aguardapp.feature.deposito.domain.repository.DepositoRepository
import com.example.aguardapp.feature.sensor.domain.model.LecturaSensor
import com.example.aguardapp.feature.sensor.domain.repository.SensorRepository

data class SensorUiState(
    val cargando: Boolean = true,
    val porcentaje: Int? = null,
    val litros: Int? = null,
    val antiguedad: String? = null,
    val error: String? = null,
    val guardando: Boolean = false,
    val mensaje: String? = null
)

private const val INTERVALO_CONSULTA_MS = 20_000L

class SensorViewModel(
    private val sensor: SensorRepository,
    private val deposito: DepositoRepository,
    private val ahora: () -> LocalDateTime
) : ViewModel() {
    private val accion = MutableStateFlow(SensorUiState())

    private val consultas = flow {
        emit(Unit)
        while (true) {
            sensor.actualizarDesdeApi()
                .onSuccess { lectura ->
                    accion.update { it.copy(error = null) }
                    aplicarSiEsNueva(lectura)
                }
                .onFailure { falla -> accion.update { it.copy(error = falla.message) } }
            emit(Unit)
            delay(INTERVALO_CONSULTA_MS)
        }
    }

    val uiState: StateFlow<SensorUiState> =
        combine(consultas, sensor.observarUltimaLectura(), deposito.observarPerfil(), accion) { _, lectura, perfil, estado ->
            val capacidad = perfil?.capacidad?.litros?.valor
            estado.copy(
                cargando = lectura == null && estado.error == null,
                porcentaje = lectura?.porcentaje?.roundToInt(),
                litros = lectura?.let { l -> capacidad?.let { l.litros(it).roundToInt() } },
                antiguedad = lectura?.let(::describirAntiguedad)
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SensorUiState())

    fun onUsarMedicion() {
        val litros = uiState.value.litros ?: return
        if (accion.value.guardando) return
        viewModelScope.launch { guardarNivel(ahora(), litros.toDouble()) }
    }

    @OptIn(ExperimentalTime::class)
    private suspend fun aplicarSiEsNueva(lectura: LecturaSensor) {
        val capacidad = deposito.observarPerfil().first()?.capacidad?.litros?.valor ?: return
        val medidaEn = minOf(lectura.medidaEn.toLocalDateTime(TimeZone.currentSystemDefault()), ahora())
        val ultimoNivel = deposito.observarDeposito().first()?.llenado?.momento
        if (ultimoNivel != null && medidaEn <= ultimoNivel) return
        guardarNivel(medidaEn, lectura.litros(capacidad).roundToInt().toDouble())
    }

    private suspend fun guardarNivel(momento: LocalDateTime, litros: Double) {
        if (litros < 1) return
        accion.update { it.copy(guardando = true, mensaje = null) }
        val resultado = deposito.registrarLlenado(momento, Litros(litros))
        accion.update {
            it.copy(
                guardando = false,
                mensaje = if (resultado.isSuccess) "Nivel actualizado con el sensor" else resultado.exceptionOrNull()?.message
            )
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun describirAntiguedad(lectura: LecturaSensor): String {
        val segundos = (Clock.System.now() - lectura.medidaEn).inWholeSeconds.coerceAtLeast(0)
        return when {
            segundos < 60 -> "hace $segundos s"
            segundos < 3_600 -> "hace ${segundos / 60} min"
            segundos < 86_400 -> "hace ${segundos / 3_600} h"
            else -> "hace ${segundos / 86_400} días"
        }
    }
}
