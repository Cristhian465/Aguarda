package com.example.aguardapp.feature.represas.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import com.example.aguardapp.feature.deposito.domain.repository.DepositoRepository
import com.example.aguardapp.feature.represas.domain.model.Presa
import com.example.aguardapp.feature.represas.domain.model.RecorridosDeTacna
import com.example.aguardapp.feature.represas.domain.repository.RepresasRepository

data class DetallePresaUiState(
    val cargando: Boolean = true,
    val presa: Presa? = null,
    /** "Abastece a Ciudad Nueva", si esta represa está en el recorrido del distrito del hogar. */
    val abasteceA: String? = null,
    /** Aclaración cuando el distrito del hogar no recibe agua de represas. */
    val nota: String? = null,
    val capacidadTanqueLitros: Double? = null
)

class DetallePresaViewModel(
    represas: RepresasRepository,
    deposito: DepositoRepository,
    nombre: String
) : ViewModel() {

    val uiState: StateFlow<DetallePresaUiState> = combine(
        represas.observarPresas(),
        represas.observarDistrito(),
        deposito.observarPerfil()
    ) { presas, distrito, perfil ->
        val recorrido = distrito?.let { RecorridosDeTacna.de(it.distrito) }
        val abastece = recorrido?.abasteceDesde(nombre) == true
        DetallePresaUiState(
            cargando = false,
            presa = presas.firstOrNull { it.nombre == nombre },
            abasteceA = distrito?.takeIf { abastece }?.let { "Abastece a ${it.distrito.nombre}" },
            nota = "Tu distrito se abastece de pozos, no de esta represa."
                .takeIf { recorrido != null && recorrido.confirmado && recorrido.presas.isEmpty() },
            capacidadTanqueLitros = perfil?.capacidad?.litros?.valor
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetallePresaUiState())
}
