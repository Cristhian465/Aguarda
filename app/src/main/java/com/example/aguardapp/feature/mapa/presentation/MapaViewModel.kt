package com.example.aguardapp.feature.mapa.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.example.aguardapp.feature.deposito.presentation.formatearHora
import com.example.aguardapp.feature.mapa.domain.model.Coordenada
import com.example.aguardapp.feature.mapa.domain.model.EstadoCisterna
import com.example.aguardapp.feature.mapa.domain.model.PuntoCisterna
import com.example.aguardapp.feature.mapa.domain.repository.MapaRepository

data class MapaUiState(
    val cargando: Boolean = true,
    val centro: Coordenada? = null,
    val puntos: List<PuntoCisterna> = emptyList()
)

class MapaViewModel(private val repositorio: MapaRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(MapaUiState())
    val uiState: StateFlow<MapaUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val puntos = repositorio.obtenerPuntosCisterna()
            _uiState.update { MapaUiState(cargando = false, centro = repositorio.centroDelMapa(), puntos = puntos) }
        }
    }
}

/** Texto bajo el nombre del marcador: "07:00 – 13:00 · Activo". */
fun detalleDe(punto: PuntoCisterna): String =
    "${formatearHora(punto.horarioInicio)} – ${formatearHora(punto.horarioFin)} · ${nombreDelEstado(punto.estado)}"

fun nombreDelEstado(estado: EstadoCisterna): String = when (estado) {
    EstadoCisterna.ACTIVO -> "Activo"
    EstadoCisterna.EN_RUTA -> "En ruta"
    EstadoCisterna.TERMINADO -> "Terminado"
}
