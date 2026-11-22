package com.example.aguardapp.feature.eps.presentation

import android.net.Uri
import java.io.File
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
import com.example.aguardapp.feature.eps.domain.leerTotalAPagar
import com.example.aguardapp.feature.eps.domain.leerDiasFacturados

data class ReciboUiState(
    val leyendo: Boolean = false,
    val revisando: Boolean = false,
    val metrosTexto: String = "",
    val mesTexto: String = "",
    val totalTexto: String = "",
    val diasTexto: String = "",
    val metrosLeidos: Double? = null,
    val error: String? = null,
    val metaMensual: Double? = null,
    val historial: List<ReciboGuardado> = emptyList(),
    val mensaje: String? = null,
    val hoy: LocalDate
)

class ReciboViewModel(
    private val lector: LectorDeRecibo,
    private val recibos: RecibosLocales,
    ahora: () -> LocalDateTime
) : ViewModel() {
    private val _uiState = MutableStateFlow(ReciboUiState(metaMensual = recibos.metaMensual, historial = recibos.historial.value, hoy = ahora().date))
    val uiState: StateFlow<ReciboUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            recibos.historial.collect { historial -> _uiState.update { it.copy(historial = historial) } }
        }
    }

    fun guardarMeta(valor: Double?) {
        require(valor == null || (valor.isFinite() && valor > 0))
        recibos.metaMensual = valor
        _uiState.update { it.copy(metaMensual = valor) }
    }

    fun onFotoElegida(foto: Uri, capturaTemporal: File? = null) {
        _uiState.update { it.copy(leyendo = true, error = null, mensaje = null) }
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
                    totalTexto = leerTotalAPagar(texto)?.toString().orEmpty(),
                    diasTexto = leerDiasFacturados(texto)?.toString().orEmpty(),
                    error = if (metros == null) "No pudimos leer el consumo. Escríbelo tú; quedará sin verificar." else null
                )
            }
        }.invokeOnCompletion { capturaTemporal?.delete() }
    }

    fun onCambiarMetros(texto: String) = _uiState.update { it.copy(metrosTexto = texto, error = null) }

    fun onCambiarMes(texto: String) = _uiState.update { it.copy(mesTexto = texto, error = null) }

    fun onCambiarTotal(texto: String) = _uiState.update { it.copy(totalTexto = texto, error = null) }

    fun onCambiarDias(texto: String) = _uiState.update { it.copy(diasTexto = texto, error = null) }

    fun onCancelar() = _uiState.update { it.copy(revisando = false, error = null) }

    fun onConfirmar() {
        val estado = _uiState.value
        val metros = estado.metrosTexto.replace(',', '.').toDoubleOrNull()?.takeIf { it in 0.0..500.0 }
        val mes = MesRecibo.desdeTexto(estado.mesTexto)
        if (metros == null || mes == null || mes.mesesHasta(estado.hoy) < 0) {
            _uiState.update { it.copy(error = "Revisa el consumo y escribe un periodo válido, sin fechas futuras.") }
            return
        }
        val total = estado.totalTexto.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0 }
        val dias = estado.diasTexto.trim().toIntOrNull()?.takeIf { it in 1..366 }
        if ((estado.totalTexto.isNotBlank() && total == null) || (estado.diasTexto.isNotBlank() && dias == null)) {
            _uiState.update { it.copy(error = "Revisa el total y los días facturados. Puedes dejarlos en blanco.") }
            return
        }
        val existeEps = estado.historial.any { it.mes == mes && it.leidoDeEps }
        val igualALaFoto = estado.metrosLeidos?.let { abs(it - metros) < 0.001 } == true
        recibos.guardarRecibo(ReciboGuardado(mes, metros, igualALaFoto, totalPagar = total, diasFacturados = dias))
        val mensaje = if (existeEps) "Conservamos los datos de consumo y total que llegaron de EPS para $mes."
            else "Recibo de $mes guardado en el historial."
        _uiState.update { it.copy(revisando = false, historial = recibos.historial.value, error = null, mensaje = mensaje) }
    }
}
