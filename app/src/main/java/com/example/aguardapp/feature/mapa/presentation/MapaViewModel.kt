package com.example.aguardapp.feature.mapa.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.example.aguardapp.feature.deposito.presentation.formatearHora
import com.example.aguardapp.feature.mapa.domain.model.CisternaCercana
import com.example.aguardapp.feature.mapa.domain.model.Coordenada
import com.example.aguardapp.feature.mapa.domain.model.EstadoCisterna
import com.example.aguardapp.feature.mapa.domain.model.PuntoCisterna
import com.example.aguardapp.feature.mapa.domain.repository.MapaRepository
import com.example.aguardapp.feature.mapa.domain.repository.UbicacionRepository
import com.example.aguardapp.feature.mapa.domain.usecase.BuscarCisternasCercanas

/** Cómo va la búsqueda de la ubicación del usuario. */
enum class EstadoUbicacion { SIN_PERMISO, BUSCANDO, ENCONTRADA, NO_DISPONIBLE }

data class MapaUiState(
    val cargando: Boolean = true,
    val centro: Coordenada? = null,
    val puntos: List<PuntoCisterna> = emptyList(),
    val ubicacion: EstadoUbicacion = EstadoUbicacion.SIN_PERMISO,
    val cercanas: List<CisternaCercana> = emptyList()
)

class MapaViewModel(
    private val repositorio: MapaRepository,
    private val ubicacion: UbicacionRepository,
    private val buscarCercanas: BuscarCisternasCercanas = BuscarCisternasCercanas()
) : ViewModel() {
    private val _uiState = MutableStateFlow(MapaUiState())
    val uiState: StateFlow<MapaUiState> = _uiState.asStateFlow()

    private val carga: Job = viewModelScope.launch {
        val puntos = repositorio.obtenerPuntosCisterna()
        _uiState.update { it.copy(cargando = false, centro = repositorio.centroDelMapa(), puntos = puntos) }
    }

    /** La pantalla avisa si el usuario dio (o ya tenía) el permiso de ubicación. */
    fun onPermisoDeUbicacion(concedido: Boolean) {
        if (!concedido) {
            _uiState.update { it.copy(ubicacion = EstadoUbicacion.SIN_PERMISO, cercanas = emptyList()) }
            return
        }
        if (_uiState.value.ubicacion == EstadoUbicacion.BUSCANDO) return
        _uiState.update { it.copy(ubicacion = EstadoUbicacion.BUSCANDO) }
        viewModelScope.launch {
            carga.join()
            val actual = runCatching { ubicacion.ubicacionActual() }.getOrNull()
            _uiState.update { estado ->
                if (actual == null) {
                    estado.copy(ubicacion = EstadoUbicacion.NO_DISPONIBLE, cercanas = emptyList())
                } else {
                    estado.copy(ubicacion = EstadoUbicacion.ENCONTRADA, cercanas = buscarCercanas.buscar(estado.puntos, actual))
                }
            }
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
