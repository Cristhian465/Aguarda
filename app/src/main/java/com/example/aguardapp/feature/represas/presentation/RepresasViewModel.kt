package com.example.aguardapp.feature.represas.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.example.aguardapp.feature.represas.domain.model.Presa
import com.example.aguardapp.feature.represas.domain.repository.RepresasRepository

class RepresasViewModel(private val repositorio: RepresasRepository) : ViewModel() {

    /** Lo que hay en Room. Cuando llegan datos nuevos de la ANA, la lista cambia sola. */
    val presas: StateFlow<List<Presa>> =
        repositorio.presas.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    var cargando by mutableStateOf(false)
        private set
    var sinConexion by mutableStateOf(false)
        private set

    init { actualizar() }

    fun actualizar() {
        viewModelScope.launch {
            cargando = true
            sinConexion = !repositorio.actualizar()
            cargando = false
        }
    }
}
