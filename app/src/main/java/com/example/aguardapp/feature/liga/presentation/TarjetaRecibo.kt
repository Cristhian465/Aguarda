package com.example.aguardapp.feature.liga.presentation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import com.example.aguardapp.core.ui.theme.AguaMedia
import com.example.aguardapp.core.ui.theme.Coral
import com.example.aguardapp.core.ui.theme.FuenteTexto
import com.example.aguardapp.feature.deposito.presentation.componentes.BotonPrincipal
import com.example.aguardapp.feature.deposito.presentation.componentes.BotonSecundario
import com.example.aguardapp.feature.deposito.presentation.componentes.TarjetaBlanca

@Composable
fun TarjetaRecibo(viewModel: ReciboViewModel = koinViewModel()) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val elegirFoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { foto ->
        foto?.let(viewModel::onFotoElegida)
    }
    TarjetaBlanca {
        Titulo("Verifica tu consumo con el recibo")
        if (estado.revisando) {
            FormularioRecibo(estado, viewModel)
        } else {
            estado.guardado?.let { recibo ->
                val verificado = recibo.verificado(estado.hoy)
                Text(
                    if (verificado) "Verificado" else "Sin verificar",
                    fontFamily = FuenteTexto, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold,
                    color = if (verificado) AguaMedia else Coral
                )
                Detalle("Recibo de ${recibo.mes}: ${recibo.metrosCubicos} m³")
                recibo.totalPagar?.let { Detalle("Total a pagar: S/ ${String.format(java.util.Locale.US, "%.2f", it)}") }
                if (recibo.leidoDeEps) Detalle("Obtenido de EPS Tacna")
                recibo.actualizadoEn?.let { Detalle("Actualizado: ${it.replace('T', ' ')}") }
            } ?: Detalle("Elige una foto de tu recibo de agua. Se lee dentro del teléfono: la foto no se guarda ni se envía.")
            BotonPrincipal(
                if (estado.leyendo) "Leyendo el recibo…" else "Elegir foto del recibo",
                { elegirFoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                Modifier.padding(top = 6.dp),
                habilitado = !estado.leyendo
            )
        }
    }
}

@Composable
private fun FormularioRecibo(estado: ReciboUiState, viewModel: ReciboViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Detalle("Revisa que los datos coincidan con tu recibo.")
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
        estado.error?.let { Text(it, fontFamily = FuenteTexto, fontSize = 13.sp, color = Coral) }
        BotonPrincipal("Confirmar", viewModel::onConfirmar)
        BotonSecundario("Cancelar", viewModel::onCancelar)
    }
}
