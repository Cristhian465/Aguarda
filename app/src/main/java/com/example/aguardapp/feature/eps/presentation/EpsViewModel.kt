package com.example.aguardapp.feature.eps.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aguardapp.feature.eps.domain.ConsultaEps
import com.example.aguardapp.feature.eps.domain.CuentaEps
import com.example.aguardapp.feature.eps.domain.ErrorEps
import com.example.aguardapp.feature.eps.data.RecibosLocales
import com.example.aguardapp.feature.eps.domain.ReciboGuardado
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime

data class EpsUiState(
    val cuenta: CuentaEps? = null,
    val trabajando: Boolean = false,
    val formulario: Boolean = false,
    val mensaje: String? = null,
    val error: String? = null
)

class EpsViewModel(
    private val consulta: ConsultaEps,
    private val recibos: RecibosLocales,
    private val ahora: () -> LocalDateTime
) : ViewModel() {
    private val _uiState = MutableStateFlow(EpsUiState(cuenta = consulta.cuenta()))
    val uiState = _uiState.asStateFlow()

    fun mostrarFormulario(mostrar: Boolean) {
        if (!_uiState.value.trabajando) _uiState.update { it.copy(formulario = mostrar, error = null, mensaje = null) }
    }

    fun vincular(usuario: String, clave: String) = ejecutar {
        consulta.vincular(usuario, clave)
        _uiState.update { it.copy(cuenta = consulta.cuenta(), formulario = false,
            mensaje = "Cuenta vinculada. Toca Actualizar recibo para consultar tu documento.") }
    }

    fun actualizar() = ejecutar {
        val recibido = consulta.actualizar()
        val fecha = ahora()
        val anterior = recibos.recibo.value
        if (recibido.mes.mesesHasta(fecha.date) < 0 ||
            (anterior != null && recibido.mes.anio * 12 + recibido.mes.mes < anterior.mes.anio * 12 + anterior.mes.mes)) {
            throw ErrorEps("EPS devolvió un periodo futuro o anterior al que ya guardaste. Conservamos tu recibo actual.")
        }
        recibos.guardarRecibo(ReciboGuardado(recibido.mes, recibido.metrosCubicos,
            leidoDeLaFoto = false, leidoDeEps = true, totalPagar = recibido.totalPagar, actualizadoEn = fecha.toString()))
        _uiState.update { it.copy(mensaje = "Recibo actualizado desde EPS. Los datos se guardaron en este teléfono.") }
    }

    fun desvincular() = ejecutar {
        consulta.desvincular()
        _uiState.update { EpsUiState(trabajando = true, mensaje = "Cuenta desvinculada de Aguarda. Tu último recibo se conserva.") }
    }

    private fun ejecutar(accion: suspend () -> Unit) {
        if (_uiState.value.trabajando) return
        _uiState.update { it.copy(trabajando = true, error = null, mensaje = null) }
        viewModelScope.launch {
            try { accion() }
            catch (error: CancellationException) { throw error }
            catch (error: Exception) {
                _uiState.update { it.copy(cuenta = consulta.cuenta(), error =
                    (error as? ErrorEps)?.detalle ?: "No pudimos completar la consulta. Intenta nuevamente.") }
            } finally { _uiState.update { it.copy(trabajando = false) } }
        }
    }
}
