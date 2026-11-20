package com.example.aguardapp.feature.sensor.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import com.example.aguardapp.feature.deposito.domain.repository.DepositoRepository
import com.example.aguardapp.feature.sensor.data.PreferenciasFuga
import com.example.aguardapp.feature.sensor.domain.repository.SensorRepository
import com.example.aguardapp.feature.sensor.domain.usecase.EstadoFuga
import com.example.aguardapp.feature.sensor.domain.usecase.detectarFuga
import com.example.aguardapp.feature.sensor.domain.usecase.ultimaMadrugada

data class FugaUiState(
    val estado: EstadoFuga = EstadoFuga.SIN_DATOS,
    val litrosPerdidos: Int = 0,
    val litrosAlMes: Int = 0,
    val descartada: Boolean = false
)

class FugaViewModel(
    private val sensor: SensorRepository,
    private val deposito: DepositoRepository,
    private val preferencias: PreferenciasFuga,
    private val ahora: () -> LocalDateTime
) : ViewModel() {
    private val _uiState = MutableStateFlow(FugaUiState())
    val uiState: StateFlow<FugaUiState> = _uiState.asStateFlow()

    private var noche: String? = null

    init {
        viewModelScope.launch { analizar() }
    }

    fun onFuiYo() {
        val analizada = noche ?: return
        preferencias.descartar(analizada)
        _uiState.update { it.copy(descartada = true) }
    }

    @OptIn(ExperimentalTime::class)
    private suspend fun analizar() {
        val capacidad = deposito.observarPerfil().first()?.capacidad?.litros?.valor ?: return
        val (desde, hasta) = ultimaMadrugada(ahora())
        val zona = TimeZone.currentSystemDefault()
        val lecturas = sensor.lecturasEntre(desde.toInstant(zona), hasta.toInstant(zona)).getOrNull() ?: return
        val analisis = detectarFuga(lecturas, capacidad)
        val fecha = desde.date.toString()
        noche = fecha
        _uiState.value = FugaUiState(
            estado = analisis.estado,
            litrosPerdidos = analisis.litrosPerdidos,
            litrosAlMes = analisis.litrosAlMes,
            descartada = preferencias.nocheDescartada() == fecha
        )
    }
}
