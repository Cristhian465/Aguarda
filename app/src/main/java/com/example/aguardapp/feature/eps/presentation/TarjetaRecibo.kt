package com.example.aguardapp.feature.eps.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aguardapp.core.ui.theme.Coral
import com.example.aguardapp.core.ui.theme.FuenteTexto
import com.example.aguardapp.feature.deposito.presentation.componentes.BotonPrincipal
import com.example.aguardapp.feature.deposito.presentation.componentes.BotonSecundario

@Composable
internal fun FormularioRecibo(estado: ReciboUiState, viewModel: ReciboViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Revisa que los datos coincidan con tu recibo.")
        OutlinedTextField(
            value = estado.metrosTexto,
            onValueChange = viewModel::onCambiarMetros,
            label = { Text("Consumo del mes (m³)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = estado.mesTexto,
            onValueChange = viewModel::onCambiarMes,
            label = { Text("Mes del recibo (11/2026)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = estado.totalTexto,
            onValueChange = viewModel::onCambiarTotal,
            label = { Text("Total a pagar (S/, opcional)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = estado.diasTexto,
            onValueChange = viewModel::onCambiarDias,
            label = { Text("Días facturados (opcional)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        estado.error?.let { Text(it, fontFamily = FuenteTexto, fontSize = 13.sp, color = Coral) }
        BotonPrincipal("Confirmar", viewModel::onConfirmar)
        BotonSecundario("Cancelar", viewModel::onCancelar)
    }
}
