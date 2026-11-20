package com.example.aguardapp.feature.liga.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import com.example.aguardapp.core.ui.theme.IconosClarosEnBarraDeEstado
import com.example.aguardapp.feature.deposito.presentation.componentes.BarraSuperior

@Composable
fun LigaScreen(onVolver: () -> Unit, viewModel: LigaViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    IconosClarosEnBarraDeEstado(claros = false)
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        BarraSuperior("Liga del Agua", "Tu hogar esta semana", onVolver = onVolver)
        val capacidad = uiState.capacidadLitros
        when {
            uiState.cargando -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            capacidad == null -> TarjetaSinHogar(Modifier.padding(20.dp))
            else -> Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                TarjetaMarcarNivel(uiState, capacidad, viewModel::onCambiarNivel, viewModel::onGuardarNivel)
                uiState.resumen?.let {
                    TarjetaConsumo(it)
                    TarjetaPuntaje(it)
                }
                SeccionRanking()
            }
        }
    }
}
