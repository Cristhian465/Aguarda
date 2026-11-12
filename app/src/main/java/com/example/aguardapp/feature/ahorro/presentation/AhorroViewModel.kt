package com.example.aguardapp.feature.ahorro.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aguardapp.core.util.Reloj
import com.example.aguardapp.feature.ahorro.data.AhorroLocal
import com.example.aguardapp.feature.ahorro.domain.calcularRacha
import com.example.aguardapp.feature.ahorro.domain.retosDeAhorro
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

data class AhorroUiState(
    val hoy: LocalDate,
    val diasCumplidos: Set<LocalDate>,
    val retosCumplidosHoy: Set<String>,
    val diasDeRacha: Int,
    val litrosEstimados: Int
)

class AhorroViewModel(private val ahorro: AhorroLocal, private val reloj: Reloj) : ViewModel() {
    private var historial = ahorro.historial()
    private val _uiState = MutableStateFlow(armarEstado(reloj.ahora().date))
    val uiState: StateFlow<AhorroUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            while (true) {
                delay(60_000)
                actualizarFecha()
            }
        }
    }

    fun actualizarFecha() {
        val hoy = reloj.ahora().date
        if (hoy != _uiState.value.hoy) _uiState.value = armarEstado(hoy)
    }

    fun alternarReto(retoId: String) {
        val hoy = reloj.ahora().date
        ahorro.alternarReto(hoy, retoId)
        historial = ahorro.historial()
        _uiState.value = armarEstado(hoy)
    }

    private fun armarEstado(hoy: LocalDate): AhorroUiState {
        val hastaHoy = historial.filterKeys { it <= hoy }
        return AhorroUiState(
            hoy = hoy,
            diasCumplidos = hastaHoy.keys,
            retosCumplidosHoy = hastaHoy[hoy].orEmpty(),
            diasDeRacha = calcularRacha(hastaHoy.keys, hoy),
            litrosEstimados = hastaHoy.values.sumOf { cumplidos ->
                retosDeAhorro.filter { it.id in cumplidos }.sumOf { it.litrosEstimados }
            }
        )
    }
}
