package com.example.aguardapp.feature.mapa.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import com.example.aguardapp.feature.deposito.domain.repository.DepositoRepository
import com.example.aguardapp.feature.deposito.domain.usecase.CalcularDeficit
import com.example.aguardapp.feature.deposito.presentation.formatearHora
import com.example.aguardapp.feature.mapa.domain.model.CisternaCercana
import com.example.aguardapp.feature.mapa.domain.model.Coordenada
import com.example.aguardapp.feature.mapa.domain.model.EstadoCisterna
import com.example.aguardapp.feature.mapa.domain.model.PuntoCisterna
import com.example.aguardapp.feature.mapa.domain.repository.MapaRepository
import com.example.aguardapp.feature.mapa.domain.repository.UbicacionRepository
import com.example.aguardapp.feature.mapa.domain.usecase.BuscarCisternasCercanas
import com.example.aguardapp.feature.mapa.domain.usecase.CandidataCisterna

/** Cómo va la búsqueda de la ubicación del usuario. */
enum class EstadoUbicacion { SIN_PERMISO, BUSCANDO, ENCONTRADA, NO_DISPONIBLE }

data class MapaUiState(
    val cargando: Boolean = true,
    val centro: Coordenada? = null,
    val puntos: List<PuntoCisterna> = emptyList(),
    val ubicacion: EstadoUbicacion = EstadoUbicacion.SIN_PERMISO,
    val cercanas: List<CisternaCercana> = emptyList(),
    val aviso: AvisoDeAgua? = null
) {
    /** Lo que se lista: las cercanas con distancia o, si no hay ninguna, todos los puntos sin distancia. */
    val candidatas: List<CandidataCisterna>
        get() = if (ubicacion == EstadoUbicacion.ENCONTRADA && cercanas.isNotEmpty()) {
            cercanas.map { CandidataCisterna(it.punto, it.distanciaKm) }
        } else {
            puntos.map { CandidataCisterna(it, null) }
        }
}

class MapaViewModel(
    private val repositorio: MapaRepository,
    private val ubicacion: UbicacionRepository,
    private val deposito: DepositoRepository,
    private val ahora: () -> LocalDateTime,
    private val buscarCercanas: BuscarCisternasCercanas = BuscarCisternasCercanas()
) : ViewModel() {
    private val _uiState = MutableStateFlow(MapaUiState())
    val uiState: StateFlow<MapaUiState> = _uiState.asStateFlow()

    private var agotamiento: LocalDateTime? = null
    private var proximoLlenado: LocalDateTime? = null

    private val carga: Job = viewModelScope.launch {
        val puntos = repositorio.obtenerPuntosCisterna()
        actualizar { it.copy(cargando = false, centro = repositorio.centroDelMapa(), puntos = puntos) }
    }

    init {
        // Cada cambio del depósito (un llenado, el sensor, "me quedé sin agua") rehace el aviso.
        viewModelScope.launch {
            combine(deposito.observarPerfil(), deposito.observarDeposito()) { perfil, actual -> perfil to actual }
                .collect { (perfil, actual) ->
                    val momento = ahora()
                    agotamiento = actual?.agotamientoProyectado()
                    proximoLlenado = perfil?.let { CalcularDeficit().proximoLlenado(it.configuracion.horaProximoLlenado, momento) }
                    actualizar { it }
                }
        }
    }

    /** La pantalla avisa si el usuario dio (o ya tenía) el permiso de ubicación. */
    fun onPermisoDeUbicacion(concedido: Boolean) {
        if (!concedido) {
            actualizar { it.copy(ubicacion = EstadoUbicacion.SIN_PERMISO, cercanas = emptyList()) }
            return
        }
        if (_uiState.value.ubicacion == EstadoUbicacion.BUSCANDO) return
        actualizar { it.copy(ubicacion = EstadoUbicacion.BUSCANDO) }
        viewModelScope.launch {
            carga.join()
            val actual = runCatching { ubicacion.ubicacionActual() }.getOrNull()
            actualizar { estado ->
                if (actual == null) {
                    estado.copy(ubicacion = EstadoUbicacion.NO_DISPONIBLE, cercanas = emptyList())
                } else {
                    estado.copy(ubicacion = EstadoUbicacion.ENCONTRADA, cercanas = buscarCercanas.buscar(estado.puntos, actual))
                }
            }
        }
    }

    private fun actualizar(cambio: (MapaUiState) -> MapaUiState) {
        _uiState.update { anterior ->
            val nuevo = cambio(anterior)
            nuevo.copy(aviso = armarAvisoDeAgua(agotamiento, proximoLlenado, ahora(), nuevo.candidatas))
        }
    }
}

fun detalleDe(punto: PuntoCisterna): String =
    "${formatearHora(punto.horarioInicio)} – ${formatearHora(punto.horarioFin)} · ${nombreDelEstado(punto.estado)}"

fun nombreDelEstado(estado: EstadoCisterna): String = when (estado) {
    EstadoCisterna.ACTIVO -> "Activo"
    EstadoCisterna.EN_RUTA -> "En ruta"
    EstadoCisterna.TERMINADO -> "Terminado"
}

/** "a 850 m" o "a 1.2 km". Por debajo de 1 km se redondea a decenas de metros. */
fun formatearDistancia(km: Double): String {
    val metros = (Math.round(km * 100) * 10).coerceAtLeast(10)
    if (metros < 1000) return "a $metros m"
    val decimas = Math.round(km * 10)
    return "a ${decimas / 10}.${decimas % 10} km"
}
