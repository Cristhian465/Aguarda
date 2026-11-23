package com.example.aguardapp.feature.cortes.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aguardapp.BuildConfig
import com.example.aguardapp.feature.cortes.data.PreferenciasCortes
import com.example.aguardapp.feature.cortes.domain.AVISOS_POR_PAGINA
import com.example.aguardapp.feature.cortes.domain.CatalogoSectores
import com.example.aguardapp.feature.cortes.domain.CategoriaAviso
import com.example.aguardapp.feature.cortes.domain.CorteAgua
import com.example.aguardapp.feature.cortes.domain.CortesRepository
import com.example.aguardapp.feature.cortes.domain.FiltrosAvisos
import com.example.aguardapp.feature.cortes.domain.PeriodoAviso
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException

data class CortesUiState(
    val catalogo: CatalogoSectores,
    val filtros: FiltrosAvisos,
    val cortes: List<CorteAgua> = emptyList(),
    val siguienteDesde: Int? = null,
    val cargando: Boolean = false,
    val error: String? = null,
    val apiConfigurada: Boolean = false
)

class CortesViewModel(
    private val repositorio: CortesRepository,
    private val preferencias: PreferenciasCortes,
    catalogo: CatalogoSectores
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        CortesUiState(
            catalogo = catalogo,
            filtros = FiltrosAvisos(sector = preferencias.sectorEps?.takeIf { id -> catalogo.sectores.any { it.id == id } }),
            apiConfigurada = repositorio.configurado
        )
    )
    val uiState = _uiState.asStateFlow()
    private var consulta: Job? = null
    init { actualizar() }

    fun actualizar() = cargarAvisos(0)

    fun elegirSector(sector: String?) {
        preferencias.sectorEps = sector
        actualizarFiltros(_uiState.value.filtros.copy(sector = sector))
    }

    fun elegirCategoria(categoria: CategoriaAviso?) = actualizarFiltros(_uiState.value.filtros.copy(categoria = categoria))

    fun elegirPeriodo(periodo: PeriodoAviso) = actualizarFiltros(_uiState.value.filtros.copy(periodo = periodo))

    fun escribirBusqueda(buscar: String) = actualizarFiltros(_uiState.value.filtros.copy(buscar = buscar.take(120)), 350)

    fun cargarMas() {
        val estado = _uiState.value
        if (!estado.cargando) estado.siguienteDesde?.let { cargarAvisos(it) }
    }

    private fun actualizarFiltros(filtros: FiltrosAvisos, espera: Long = 0) {
        if (filtros == _uiState.value.filtros) return
        _uiState.update { it.copy(filtros = filtros, cortes = emptyList(), siguienteDesde = null) }
        cargarAvisos(0, espera)
    }

    private fun cargarAvisos(desde: Int, espera: Long = 0) {
        if (!repositorio.configurado) return
        consulta?.cancel()
        val filtros = _uiState.value.filtros
        _uiState.update { it.copy(cargando = true, error = null) }
        consulta = viewModelScope.launch {
            try {
                delay(espera)
                val nuevos = repositorio.obtenerAvisos(filtros, desde)
                val siguienteDesde = (desde + nuevos.size).takeIf { nuevos.size == AVISOS_POR_PAGINA }
                _uiState.update {
                    val avisos = if (desde == 0) nuevos else (it.cortes + nuevos).distinctBy(CorteAgua::id)
                    it.copy(cargando = false, cortes = avisos, siguienteDesde = siguienteDesde)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                val mensaje = when {
                    error is IOException && BuildConfig.DEBUG && BuildConfig.CORTES_API_URL.isBlank() ->
                        "No se pudo acceder a la API local. Inicia la API de cortes en la PC. Si usas un celular, verifica la conexión USB."
                    error is IOException -> "No se pudo acceder al servicio de cortes. Comprueba tu conexión e inténtalo de nuevo."
                    else -> "El servicio de cortes no pudo devolver los avisos. Inténtalo de nuevo."
                }
                _uiState.update { it.copy(cargando = false, error = mensaje) }
            }
        }
    }
}
