package com.example.aguardapp.feature.liga.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import com.example.aguardapp.feature.liga.data.PreferenciasLiga
import com.example.aguardapp.feature.liga.data.ResumenDelHogar
import com.example.aguardapp.feature.liga.domain.Duelo
import com.example.aguardapp.feature.liga.domain.EnvioSemanal
import com.example.aguardapp.feature.liga.domain.FilaRanking
import com.example.aguardapp.feature.liga.domain.RankingRepository
import com.example.aguardapp.feature.liga.domain.armarRanking
import com.example.aguardapp.feature.liga.domain.duelosDeLaSemana
import com.example.aguardapp.feature.liga.domain.lunesDe
import com.example.aguardapp.feature.mapa.domain.model.distritoDe
import com.example.aguardapp.feature.mapa.domain.repository.DistritosRepository
import com.example.aguardapp.feature.mapa.domain.repository.UbicacionRepository

data class RankingLigaUiState(
    val conectado: Boolean = false,
    val participa: Boolean = false,
    val distritos: List<Pair<String, String>> = emptyList(),
    val ubigeoElegido: String? = null,
    val filas: List<FilaRanking> = emptyList(),
    val duelos: List<Duelo> = emptyList(),
    val error: String? = null
)

class RankingLigaViewModel(
    private val ranking: RankingRepository,
    private val preferencias: PreferenciasLiga,
    private val resumen: ResumenDelHogar,
    private val distritos: DistritosRepository,
    private val ubicacion: UbicacionRepository,
    private val ahora: () -> LocalDateTime
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        RankingLigaUiState(ranking.conectado, preferencias.participa, ubigeoElegido = preferencias.ubigeo)
    )
    val uiState: StateFlow<RankingLigaUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val lista = runCatching { distritos.obtenerDistritos() }.getOrDefault(emptyList())
            _uiState.update { estado -> estado.copy(distritos = lista.map { it.ubigeo to it.nombre }) }
            actualizar()
            // Propone el distrito del usuario; la ubicación se usa aquí y no se envía.
            val propuesto = runCatching { ubicacion.ubicacionActual() }.getOrNull()?.let { lista.distritoDe(it)?.ubigeo }
            _uiState.update { estado -> estado.copy(ubigeoElegido = estado.ubigeoElegido ?: propuesto) }
        }
    }

    fun onElegirDistrito(ubigeo: String) {
        _uiState.update { it.copy(ubigeoElegido = ubigeo) }
    }

    fun onParticipar() {
        val ubigeo = _uiState.value.ubigeoElegido ?: return
        preferencias.aceptar(ubigeo)
        _uiState.update { it.copy(participa = true) }
        actualizar()
    }

    fun onDejarDeParticipar() {
        viewModelScope.launch {
            val borrado = ranking.borrarMisDatos(preferencias.token())
            if (borrado.isSuccess) {
                preferencias.salir()
                _uiState.update { it.copy(participa = false, error = null) }
                actualizar()
            } else {
                _uiState.update { it.copy(error = "No se pudieron borrar tus datos. Revisa tu conexión e inténtalo de nuevo.") }
            }
        }
    }

    /** Envía el resumen del hogar (si aceptó participar) y trae el ranking de la semana. */
    fun actualizar() {
        if (!ranking.conectado) return
        viewModelScope.launch {
            val lunes = lunesDe(ahora().date)
            val envio = if (preferencias.participa) enviarResumen() else Result.success(Unit)
            val posiciones = ranking.ranking(lunes)
            _uiState.update { estado ->
                val filas = posiciones.getOrNull()?.let { armarRanking(estado.distritos, it) } ?: estado.filas
                estado.copy(
                    filas = filas,
                    duelos = duelosDeLaSemana(filas, lunes),
                    error = (posiciones.exceptionOrNull() ?: envio.exceptionOrNull())?.message
                )
            }
        }
    }

    private suspend fun enviarResumen(): Result<Unit> {
        val datos = resumen.observar().first() ?: return Result.success(Unit)
        val semana = datos.resumen
        val litros = semana.litrosPorPersonaDia ?: return Result.success(Unit)
        val puntaje = semana.puntaje ?: return Result.success(Unit)
        val ubigeo = preferencias.ubigeo ?: return Result.success(Unit)
        return ranking.enviar(EnvioSemanal(preferencias.token(), semana.lunes, ubigeo, litros, puntaje.total, datos.verificado))
    }
}
