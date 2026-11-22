package com.example.aguardapp.feature.familia.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.example.aguardapp.feature.familia.domain.model.Invitacion
import com.example.aguardapp.feature.familia.domain.model.MiembroFamilia
import com.example.aguardapp.feature.familia.domain.repository.FamiliaRepository
import com.example.aguardapp.feature.familia.domain.usecase.formatearCodigo
import com.example.aguardapp.feature.familia.domain.usecase.normalizarCodigo

private const val LARGO_APODO = 30
private const val LARGO_CODIGO = 20
private const val MILIS_POR_MINUTO = 60_000L
private const val MINUTOS_POR_HORA = 60
private const val MINUTOS_POR_DIA = 1_440

data class FamiliaUiState(
    val enFamilia: Boolean = false,
    val apodo: String = "",
    val codigo: String = "",
    val miembros: List<MiembroFamilia> = emptyList(),
    val invitacion: Invitacion? = null,
    val ultimaSincronizacion: Long? = null,
    val trabajando: Boolean = false,
    val mensaje: String? = null,
    val error: String? = null
) {
    val puedeCrear: Boolean get() = !trabajando && apodo.isNotBlank()
    val puedeUnirse: Boolean get() = !trabajando && apodo.isNotBlank() && normalizarCodigo(codigo) != null
}

class FamiliaViewModel(private val familia: FamiliaRepository) : ViewModel() {
    private val estado = MutableStateFlow(
        FamiliaUiState(apodo = familia.apodo().orEmpty(), ultimaSincronizacion = familia.ultimaSincronizacion())
    )
    val uiState: StateFlow<FamiliaUiState> = estado.asStateFlow()

    init {
        viewModelScope.launch {
            familia.enFamilia.collect { enFamilia ->
                estado.update {
                    if (enFamilia) it.copy(enFamilia = true)
                    else it.copy(enFamilia = false, miembros = emptyList(), invitacion = null)
                }
            }
        }
        // Al abrir la pantalla se sincroniza, para ver a la familia y lo último que registraron.
        viewModelScope.launch { if (familia.enFamilia.first()) sincronizar() }
    }

    fun onApodo(texto: String) = estado.update { it.copy(apodo = texto.take(LARGO_APODO)) }

    fun onCodigo(texto: String) = estado.update { it.copy(codigo = texto.take(LARGO_CODIGO)) }

    fun onCodigoEscaneado(texto: String) {
        val codigo = normalizarCodigo(texto)
        if (codigo == null) {
            mostrarError("Ese QR no es una invitación de Aguarda")
        } else {
            estado.update { it.copy(codigo = formatearCodigo(codigo), error = null, mensaje = "Código leído. Revisa tu nombre y toca Unirme") }
        }
    }

    fun mostrarError(texto: String) = estado.update { it.copy(error = texto, mensaje = null) }

    fun crear() = ejecutar {
        familia.crear(estado.value.apodo).getOrThrow()
        actualizarMiembros()
        avisar("Familia creada. Toca Invitar a alguien para sumar los demás teléfonos")
    }

    fun unirse() = ejecutar {
        val actual = estado.value
        familia.unirse(actual.codigo, actual.apodo).getOrThrow()
        estado.update { it.copy(codigo = "") }
        actualizarMiembros()
        avisar("Listo: ya compartes el tanque con tu familia")
    }

    fun invitar() = ejecutar {
        val invitacion = familia.invitar().getOrThrow()
        estado.update { it.copy(invitacion = invitacion) }
    }

    fun sincronizar() = ejecutar { actualizarMiembros() }

    fun salir() = ejecutar {
        familia.salir().getOrThrow()
        avisar("Saliste de la familia. Los datos del tanque se quedan en este teléfono")
    }

    fun reiniciar() = ejecutar {
        familia.reiniciar().getOrThrow()
        estado.update { it.copy(invitacion = null) }
        actualizarMiembros()
        avisar("Solo tú quedas en la familia. Invita de nuevo a quien corresponda")
    }

    private suspend fun actualizarMiembros() {
        val sincronizado = familia.sincronizar().getOrThrow()
        estado.update { it.copy(miembros = sincronizado.miembros) }
    }

    private fun avisar(texto: String) = estado.update { it.copy(mensaje = texto) }

    private fun ejecutar(accion: suspend () -> Unit) {
        if (estado.value.trabajando) return
        estado.update { it.copy(trabajando = true, error = null, mensaje = null) }
        viewModelScope.launch {
            try {
                accion()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                estado.update { it.copy(error = e.message ?: "No se pudo completar. Inténtalo de nuevo") }
            } finally {
                estado.update { it.copy(trabajando = false, ultimaSincronizacion = familia.ultimaSincronizacion()) }
            }
        }
    }
}

fun describirSincronizacion(ultimaMillis: Long?, ahoraMillis: Long): String {
    if (ultimaMillis == null) return "Aún no se ha sincronizado"
    val minutos = ((ahoraMillis - ultimaMillis) / MILIS_POR_MINUTO).coerceAtLeast(0)
    return when {
        minutos < 1 -> "Sincronizado hace un momento"
        minutos < MINUTOS_POR_HORA -> "Sincronizado hace $minutos min"
        minutos < MINUTOS_POR_DIA -> "Sincronizado hace ${minutos / MINUTOS_POR_HORA} h"
        else -> "Sincronizado hace ${minutos / MINUTOS_POR_DIA} días"
    }
}
