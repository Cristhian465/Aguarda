package com.example.aguardapp.feature.represas.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import com.example.aguardapp.feature.deposito.domain.repository.DepositoRepository
import com.example.aguardapp.feature.deposito.presentation.describirMomento
import com.example.aguardapp.feature.represas.domain.model.Distrito
import com.example.aguardapp.feature.represas.domain.model.DistritoDelHogar
import com.example.aguardapp.feature.represas.domain.model.Presa
import com.example.aguardapp.feature.represas.domain.model.RecorridoDelAgua
import com.example.aguardapp.feature.represas.domain.model.RecorridosDeTacna
import com.example.aguardapp.feature.represas.domain.model.UsoPresa
import com.example.aguardapp.feature.represas.domain.repository.RepresasRepository
import com.example.aguardapp.feature.represas.domain.repository.UbicacionRepository

enum class FiltroUso(val etiqueta: String, private val uso: UsoPresa?) {
    TODAS("Todas", null),
    AGUA_POTABLE("Agua potable", UsoPresa.AGUA_POTABLE),
    RIEGO("Riego", UsoPresa.RIEGO),
    MINERIA("Minería", UsoPresa.MINERIA);

    fun incluye(presa: Presa): Boolean = uso == null || uso in presa.usos
}

data class RepresasUiState(
    val cargando: Boolean = true,
    val distrito: DistritoDelHogar? = null,
    val recorrido: RecorridoDelAgua? = null,
    val presas: List<Presa> = emptyList(),
    val filtro: FiltroUso = FiltroUso.TODAS,
    val capacidadTanqueLitros: Double? = null,
    /** Cuándo se descargaron los datos de la ANA, por ejemplo "hoy 10:52 a.m.". */
    val actualizado: String? = null,
    /** No se pudieron renovar los datos: se muestran los guardados, si los hay. */
    val errorDeDescarga: String? = null,
    val buscandoUbicacion: Boolean = false,
    val mensaje: String? = null
) {
    val presasVisibles: List<Presa> get() = presas.filter(filtro::incluye)
}

// Lo que no viene de la base de datos: lo que el usuario está haciendo en la pantalla.
private data class EstadoDePantalla(
    val filtro: FiltroUso = FiltroUso.TODAS,
    val descargando: Boolean = false,
    val errorDeDescarga: String? = null,
    val buscandoUbicacion: Boolean = false,
    val mensaje: String? = null
)

class RepresasViewModel(
    private val represas: RepresasRepository,
    private val ubicacion: UbicacionRepository,
    deposito: DepositoRepository,
    private val ahora: () -> LocalDateTime
) : ViewModel() {

    private val pantalla = MutableStateFlow(EstadoDePantalla())

    val uiState: StateFlow<RepresasUiState> = combine(
        represas.observarPresas(),
        represas.observarDistrito(),
        represas.observarDescarga(),
        deposito.observarPerfil(),
        pantalla
    ) { presas, distrito, descarga, perfil, estado ->
        RepresasUiState(
            cargando = presas.isEmpty() && estado.descargando,
            distrito = distrito,
            recorrido = distrito?.let { RecorridosDeTacna.de(it.distrito) },
            presas = presas,
            filtro = estado.filtro,
            capacidadTanqueLitros = perfil?.capacidad?.litros?.valor,
            actualizado = descarga?.let { describirMomento(it, ahora()) },
            errorDeDescarga = estado.errorDeDescarga,
            buscandoUbicacion = estado.buscandoUbicacion,
            mensaje = estado.mensaje
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RepresasUiState())

    init {
        actualizar(forzar = false)
    }

    fun onReintentar() = actualizar(forzar = true)

    fun onFiltrar(filtro: FiltroUso) = pantalla.update { it.copy(filtro = filtro) }

    fun onElegirDistrito(distrito: Distrito) {
        pantalla.update { it.copy(mensaje = null) }
        viewModelScope.launch { represas.elegirDistrito(distrito) }
    }

    /**
     * Solo cuando el usuario toca "Usar mi ubicación" y concede el permiso: las coordenadas salen del teléfono
     * (hacia Nominatim) únicamente por una acción explícita, aunque el permiso ya se haya dado en otra pantalla.
     */
    fun onUsarMiUbicacion() {
        if (pantalla.value.buscandoUbicacion) return
        pantalla.update { it.copy(buscandoUbicacion = true, mensaje = null) }
        viewModelScope.launch {
            val coordenada = ubicacion.ubicacionActual()
            val error = if (coordenada == null) {
                "No pudimos leer tu ubicación. Revisa que esté activada o elige tu distrito de la lista."
            } else {
                represas.detectarDistrito(coordenada).exceptionOrNull()?.message
            }
            pantalla.update { it.copy(buscandoUbicacion = false, mensaje = error) }
        }
    }

    fun onPermisoDenegado() = pantalla.update { it.copy(mensaje = "Sin permiso de ubicación. Elige tu distrito de la lista.") }

    fun onCerrarMensaje() = pantalla.update { it.copy(mensaje = null) }

    private fun actualizar(forzar: Boolean) {
        if (pantalla.value.descargando) return
        pantalla.update { it.copy(descargando = true, errorDeDescarga = null) }
        viewModelScope.launch {
            val resultado = represas.actualizarPresas(forzar)
            pantalla.update { it.copy(descargando = false, errorDeDescarga = resultado.exceptionOrNull()?.message) }
        }
    }
}
