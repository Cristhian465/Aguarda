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
import com.example.aguardapp.feature.liga.domain.FilaRanking
import com.example.aguardapp.feature.liga.domain.RankingRepository
import com.example.aguardapp.feature.liga.domain.armarRanking
import com.example.aguardapp.feature.liga.domain.lunesDe
import com.example.aguardapp.feature.mapa.domain.model.CisternaCercana
import com.example.aguardapp.feature.mapa.domain.model.Coordenada
import com.example.aguardapp.feature.mapa.domain.model.Distrito
import com.example.aguardapp.feature.mapa.domain.model.PuntoCisterna
import com.example.aguardapp.feature.mapa.domain.model.distritoDe
import com.example.aguardapp.feature.mapa.domain.repository.DistritosRepository
import com.example.aguardapp.feature.mapa.domain.repository.MapaRepository
import com.example.aguardapp.feature.mapa.domain.repository.UbicacionRepository
import com.example.aguardapp.feature.mapa.domain.usecase.BuscarCisternasCercanas
import com.example.aguardapp.feature.mapa.domain.usecase.CandidataCisterna

enum class EstadoUbicacion { SIN_PERMISO, BUSCANDO, ENCONTRADA, NO_DISPONIBLE }

data class MapaUiState(
    val cargando: Boolean = true,
    val centro: Coordenada? = null,
    val puntos: List<PuntoCisterna> = emptyList(),
    val ubicacion: EstadoUbicacion = EstadoUbicacion.SIN_PERMISO,
    val cercanas: List<CisternaCercana> = emptyList(),
    val aviso: AvisoDeAgua? = null,
    val distritos: List<Distrito> = emptyList(),
    val miDistrito: Distrito? = null,
    val distritoElegido: Distrito? = null,
    val liga: List<FilaRanking> = emptyList()
) {
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
    private val distritos: DistritosRepository,
    private val ranking: RankingRepository,
    private val ahora: () -> LocalDateTime,
    private val buscarCercanas: BuscarCisternasCercanas = BuscarCisternasCercanas()
) : ViewModel() {
    private val _uiState = MutableStateFlow(MapaUiState())
    val uiState: StateFlow<MapaUiState> = _uiState.asStateFlow()

    private var agotamiento: LocalDateTime? = null
    private var proximoLlenado: LocalDateTime? = null

    private val carga: Job = viewModelScope.launch {
        val puntos = repositorio.obtenerPuntosCisterna()
        val limites = runCatching { distritos.obtenerDistritos() }.getOrDefault(emptyList())
        actualizar { it.copy(cargando = false, centro = repositorio.centroDelMapa(), puntos = puntos, distritos = limites) }
    }

    init {
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
                    estado.copy(
                        ubicacion = EstadoUbicacion.ENCONTRADA,
                        cercanas = buscarCercanas.buscar(estado.puntos, actual),
                        miDistrito = estado.distritos.distritoDe(actual)
                    )
                }
            }
        }
    }

    fun onTocarDistrito(punto: Coordenada) {
        actualizar { it.copy(distritoElegido = it.distritos.distritoDe(punto)) }
    }

    fun onRefrescarLiga() {
        viewModelScope.launch {
            carga.join()
            val posiciones = ranking.ranking(lunesDe(ahora().date)).getOrNull() ?: return@launch
            actualizar { it.copy(liga = armarRanking(it.distritos.map { d -> d.ubigeo to d.nombre }, posiciones)) }
        }
    }

    fun onCerrarDistrito() {
        actualizar { it.copy(distritoElegido = null) }
    }

    private fun actualizar(cambio: (MapaUiState) -> MapaUiState) {
        _uiState.update { anterior ->
            val nuevo = cambio(anterior)
            nuevo.copy(aviso = armarAvisoDeAgua(agotamiento, proximoLlenado, ahora(), nuevo.candidatas))
        }
    }
}

