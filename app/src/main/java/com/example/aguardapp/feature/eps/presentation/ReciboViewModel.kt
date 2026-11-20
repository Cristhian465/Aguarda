package com.example.aguardapp.feature.eps.presentation

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlin.math.abs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import com.example.aguardapp.feature.eps.data.LectorDeRecibo
import com.example.aguardapp.feature.eps.data.RecibosLocales
import com.example.aguardapp.feature.eps.domain.MesRecibo
import com.example.aguardapp.feature.eps.domain.ReciboGuardado
import com.example.aguardapp.feature.eps.domain.leerConsumoDelRecibo
import com.example.aguardapp.feature.eps.domain.leerMesDelRecibo

data class ReciboUiState(
    val leyendo: Boolean = false,
    val revisando: Boolean = false,
    val metrosTexto: String = "",
    val mesTexto: String = "",
    val metrosLeidos: Double? = null,
    val error: String? = null,
    val guardado: ReciboGuardado? = null,
    val hoy: LocalDate
)

class ReciboViewModel(
    private val lector: LectorDeRecibo,
    private val recibos: RecibosLocales,
    ahora: () -> LocalDateTime
) : ViewModel() {
    private val _uiState = MutableStateFlow(ReciboUiState(guardado = recibos.recibo.value, hoy = ahora().date))
    val uiState: StateFlow<ReciboUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            recibos.recibo.collect { recibo -> _uiState.update { it.copy(guardado = recibo) } }
        }
    }

    fun onFotoElegida(foto: Uri) {
        _uiState.update { it.copy(leyendo = true, error = null) }
        viewModelScope.launch {
            val texto = lector.leerTexto(foto).getOrNull().orEmpty()
            val metros = leerConsumoDelRecibo(texto)
            _uiState.update {
                it.copy(
                    leyendo = false,
                    revisando = true,
                    metrosLeidos = metros,
                    metrosTexto = metros?.toString().orEmpty(),
                    mesTexto = leerMesDelRecibo(texto)?.toString().orEmpty(),
                    error = if (metros == null) "No pudimos leer el consumo. Escríbelo tú; quedará sin verificar." else null
                )
            }
        }
    }

    fun onCambiarMetros(texto: String) = _uiState.update { it.copy(metrosTexto = texto, error = null) }

    fun onCambiarMes(texto: String) = _uiState.update { it.copy(mesTexto = texto, error = null) }

    fun onCancelar() = _uiState.update { it.copy(revisando = false, error = null) }

    fun onConfirmar() {
        val estado = _uiState.value
        val metros = estado.metrosTexto.replace(',', '.').toDoubleOrNull()?.takeIf { it > 0 && it <= 500 }
        val mes = MesRecibo.desdeTexto(estado.mesTexto)
        if (metros == null || mes == null) {
            _uiState.update { it.copy(error = "Revisa los datos: consumo en m³ y mes como 11/2026.") }
            return
        }
        // Si el usuario cambió el número leído, el recibo ya no cuenta como verificado.
        val igualALaFoto = estado.metrosLeidos?.let { abs(it - metros) < 0.001 } == true
        val recibo = ReciboGuardado(mes, metros, igualALaFoto)
        recibos.guardarRecibo(recibo)
        _uiState.update { it.copy(revisando = false, guardado = recibo, error = null) }
    }
}
