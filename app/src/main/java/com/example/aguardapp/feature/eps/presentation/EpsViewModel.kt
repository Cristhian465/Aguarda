package com.example.aguardapp.feature.eps.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aguardapp.feature.eps.domain.ConsultaEps
import com.example.aguardapp.feature.eps.domain.CuentaEps
import com.example.aguardapp.feature.eps.domain.ErrorEps
import com.example.aguardapp.feature.eps.data.RecibosLocales
import com.example.aguardapp.feature.eps.domain.ReciboGuardado
import com.example.aguardapp.feature.eps.domain.ReciboEps
import com.example.aguardapp.feature.eps.domain.periodosImportables
import java.io.IOException
import java.io.OutputStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime

enum class AccionRecibos(val boton: String, val alReconectar: String) {
    ACTUALIZAR("Actualizar último recibo", "Vincular y actualizar"),
    IMPORTAR("Importar historial completo", "Vincular e importar"),
    PDF("Descargar PDF", "Vincular y descargar PDF"),
    VER_PDF("Ver PDF", "Vincular y ver PDF")
}

data class EpsUiState(
    val cuenta: CuentaEps? = null,
    val trabajando: Boolean = false,
    val elegirDestinoPdf: Boolean = false,
    val pdfVisible: ByteArray? = null,
    val formulario: Boolean = false,
    val reconectar: CuentaEps? = null,
    val accionPendiente: AccionRecibos? = null,
    val usuarioRecordado: String = "",
    val mensaje: String? = null,
    val error: String? = null
)

class EpsViewModel(
    private val consulta: ConsultaEps,
    private val recibos: RecibosLocales,
    private val ahora: () -> LocalDateTime
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        EpsUiState(cuenta = consulta.cuenta(), usuarioRecordado = recibos.ultimoUsuarioEps.orEmpty())
    )
    val uiState = _uiState.asStateFlow()
    private var pdfPendiente: ByteArray? = null

    init {
        // Recupera el usuario si la cuenta se vinculó antes de que se guardara este dato.
        _uiState.value.cuenta?.let { cuenta ->
            recibos.ultimoUsuarioEps = cuenta.usuario
            _uiState.update { it.copy(usuarioRecordado = cuenta.usuario) }
        }
    }

    fun mostrarFormulario(mostrar: Boolean, accionPendiente: AccionRecibos? = null) {
        _uiState.update { estado ->
            if (estado.trabajando) estado else estado.copy(
                formulario = mostrar,
                accionPendiente = if (mostrar) accionPendiente ?: estado.accionPendiente else null,
                error = null,
                mensaje = null
            )
        }
    }

    fun vincular(usuario: String, clave: String) = ejecutar(_uiState.value.accionPendiente) {
        val reanudar = _uiState.value.accionPendiente
        consulta.vincular(usuario, clave)
        val cuenta = consulta.cuenta()
        recibos.ultimoUsuarioEps = cuenta?.usuario
        _uiState.update {
            it.copy(
                cuenta = cuenta,
                usuarioRecordado = cuenta?.usuario.orEmpty(),
                formulario = false,
                reconectar = null,
                accionPendiente = null,
                mensaje = if (reanudar == null) "Cuenta vinculada. Ya puedes consultar o importar tus recibos." else null
            )
        }
        when (reanudar) {
            AccionRecibos.ACTUALIZAR -> descargarYGuardar()
            AccionRecibos.IMPORTAR -> importarYGuardar()
            AccionRecibos.PDF -> prepararPdf()
            AccionRecibos.VER_PDF -> abrirVisorPdf()
            null -> Unit
        }
    }

    fun actualizar() = ejecutar(AccionRecibos.ACTUALIZAR) { descargarYGuardar() }

    fun importar() = ejecutar(AccionRecibos.IMPORTAR) { importarYGuardar() }

    fun descargarPdf() {
        if (_uiState.value.cuenta == null) mostrarFormulario(true, AccionRecibos.PDF)
        else ejecutar(AccionRecibos.PDF) { prepararPdf() }
    }

    fun verPdf() {
        if (_uiState.value.cuenta == null) mostrarFormulario(true, AccionRecibos.VER_PDF)
        else ejecutar(AccionRecibos.VER_PDF) { abrirVisorPdf() }
    }

    private suspend fun abrirVisorPdf() {
        val pdf = consulta.descargarUltimoPdf()
        _uiState.update { it.copy(pdfVisible = pdf) }
    }

    fun cerrarVisorPdf() = _uiState.update { it.copy(pdfVisible = null) }

    private suspend fun prepararPdf() {
        pdfPendiente = consulta.descargarUltimoPdf()
        _uiState.update { it.copy(elegirDestinoPdf = true) }
    }

    fun selectorPdfAbierto() = _uiState.update { it.copy(elegirDestinoPdf = false) }

    fun descartarPdf(error: String? = null) {
        pdfPendiente = null
        _uiState.update { it.copy(elegirDestinoPdf = false, error = error) }
    }

    fun guardarPdf(abrirDestino: () -> OutputStream?) {
        val pdf = pdfPendiente
        descartarPdf()
        if (pdf == null) {
            _uiState.update { it.copy(error = "La descarga se interrumpió. Vuelve a tocar Descargar PDF.") }
            return
        }
        ejecutar {
            try {
                withContext(Dispatchers.IO) {
                    val destino = abrirDestino() ?: throw IOException()
                    destino.use { it.write(pdf) }
                }
            } catch (error: IOException) {
                throw ErrorEps("No se pudo guardar el PDF. Intenta con otra ubicación.")
            } catch (error: SecurityException) {
                throw ErrorEps("Aguarda no tiene acceso a esa ubicación. Elige otra carpeta.")
            }
            _uiState.update { it.copy(mensaje = "PDF guardado en la ubicación elegida.") }
        }
    }

    private suspend fun importarYGuardar() {
        val historialEps = consulta.consultarHistorial()
        val fecha = ahora()
        val periodos = periodosImportables(historialEps, fecha.date)
        if (periodos.isEmpty()) {
            _uiState.update { it.copy(mensaje = "EPS no mostró periodos que se puedan importar.") }
            return
        }
        recibos.guardarRecibos(periodos.map { it.paraGuardar(fecha) })
        val mensaje = if (periodos.size == 1) "Se importó 1 periodo desde EPS."
            else "Se importaron ${periodos.size} periodos desde EPS."
        _uiState.update { it.copy(mensaje = mensaje) }
    }

    private suspend fun descargarYGuardar() {
        val recibido = consulta.actualizar()
        val fecha = ahora()
        if (recibido.mes.mesesHasta(fecha.date) < 0) {
            throw ErrorEps("EPS devolvió un periodo futuro. Conservamos tus recibos guardados.")
        }
        recibos.guardarRecibo(recibido.paraGuardar(fecha))
        _uiState.update { it.copy(mensaje = "Recibo actualizado desde EPS. Los datos se guardaron en este teléfono.") }
    }

    private fun ReciboEps.paraGuardar(fecha: LocalDateTime) = ReciboGuardado(
        mes, metrosCubicos, leidoDeLaFoto = false, leidoDeEps = true, totalPagar = totalPagar,
        actualizadoEn = fecha.toString(), diasFacturados = diasFacturados, totalMes = totalMes, deuda = deuda
    )

    fun desvincular() = ejecutar {
        consulta.desvincular()
        recibos.ultimoUsuarioEps = null
        _uiState.update { EpsUiState(trabajando = true, mensaje = "Cuenta desvinculada de Aguarda. Tu historial se conserva.") }
    }

    private fun ejecutar(operacion: AccionRecibos? = null, accion: suspend () -> Unit) {
        if (_uiState.value.trabajando) return
        _uiState.update { it.copy(trabajando = true, error = null, mensaje = null) }
        viewModelScope.launch {
            try { accion() }
            catch (error: CancellationException) { throw error }
            catch (error: Exception) {
                val fallo = error as? ErrorEps
                _uiState.update { estado ->
                    val reconectar = if (fallo?.requiereVincular == true) {
                        estado.cuenta ?: estado.reconectar
                    } else estado.reconectar
                    estado.copy(
                        cuenta = consulta.cuenta(),
                        formulario = estado.formulario || reconectar != null,
                        reconectar = reconectar,
                        accionPendiente = if (reconectar != null) operacion else null,
                        error = if (fallo?.requiereVincular == true && reconectar != null) {
                            "Tu sesión de EPS ya no está activa. Ingresa tu clave para continuar."
                        } else fallo?.detalle ?: "No pudimos completar la consulta. Intenta nuevamente."
                    )
                }
            } finally { _uiState.update { it.copy(trabajando = false) } }
        }
    }
}
