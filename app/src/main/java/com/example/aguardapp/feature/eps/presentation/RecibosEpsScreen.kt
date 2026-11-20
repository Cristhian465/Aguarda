package com.example.aguardapp.feature.eps.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.aguardapp.core.ui.theme.IconosClarosEnBarraDeEstado
import com.example.aguardapp.feature.deposito.presentation.componentes.BarraSuperior

@Composable
fun RecibosEpsScreen(onVolver: () -> Unit) {
    IconosClarosEnBarraDeEstado(claros = false)
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        BarraSuperior("Recibos EPS", "Consulta tu recibo de agua", onVolver)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            TarjetaEps()
            TarjetaRecibo()
        }
    }
}
