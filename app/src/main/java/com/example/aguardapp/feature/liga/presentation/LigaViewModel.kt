package com.example.aguardapp.feature.liga.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlin.math.roundToInt
import com.example.aguardapp.feature.deposito.domain.repository.DepositoRepository
import com.example.aguardapp.feature.liga.data.ResumenDelHogar
import com.example.aguardapp.feature.liga.domain.LigaRepository
import com.example.aguardapp.feature.liga.domain.MarcaNivel
import com.example.aguardapp.feature.liga.domain.ResumenSemanal

data class LigaUiState(
    val cargando: Boolean = true,
    val capacidadLitros: Int? = null,
    val nivelElegido: Int = 0,
    val resumen: ResumenSemanal? = null,
    val guardando: Boolean = false,
    val mensaje: String? = null
)

class LigaViewModel(
    private val liga: LigaRepository,
    private val deposito: DepositoRepository,
    private val hogar: ResumenDelHogar,
    private val ahora: () -> LocalDateTime
) : ViewModel() {
    private val _uiState = MutableStateFlow(LigaUiState())
    val uiState: StateFlow<LigaUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val estimado = deposito.observarDeposito().first()?.nivelEn(ahora())?.litros?.valor?.roundToInt() ?: 0
            _uiState.update { it.copy(nivelElegido = estimado) }
            hogar.observar().collect { datos ->
                _uiState.update { it.copy(cargando = false, capacidadLitros = datos?.capacidadLitros, resumen = datos?.resumen) }
            }
        }
    }

    fun onCambiarNivel(litros: Int) {
        _uiState.update { it.copy(nivelElegido = litros, mensaje = null) }
    }

    fun onGuardarNivel() {
        val estado = _uiState.value
        if (estado.guardando || estado.capacidadLitros == null) return
        _uiState.update { it.copy(guardando = true) }
        viewModelScope.launch {
            liga.guardarMarca(MarcaNivel(ahora(), estado.nivelElegido.toDouble()))
            _uiState.update { it.copy(guardando = false, mensaje = "Nivel guardado") }
        }
    }
}
