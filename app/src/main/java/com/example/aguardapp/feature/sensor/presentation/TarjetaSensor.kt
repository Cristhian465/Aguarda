package com.example.aguardapp.feature.sensor.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import com.example.aguardapp.core.ui.theme.AguaMedia
import com.example.aguardapp.core.ui.theme.Coral
import com.example.aguardapp.core.ui.theme.FuenteTexto
import com.example.aguardapp.core.ui.theme.Tinta
import com.example.aguardapp.core.ui.theme.TintaSuave
import com.example.aguardapp.core.ui.theme.TintaTenue
import com.example.aguardapp.feature.deposito.presentation.componentes.TarjetaBlanca
import com.example.aguardapp.feature.deposito.presentation.formatearMiles

@Composable
fun TarjetaSensor(modifier: Modifier = Modifier, viewModel: SensorViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    if (!uiState.configurado) return
    TarjetaBlanca(modifier) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Sensor del tanque", fontFamily = FuenteTexto, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Tinta)
            uiState.antiguedad?.let { Text(it, fontFamily = FuenteTexto, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TintaTenue) }
        }
        val porcentaje = uiState.porcentaje
        when {
            uiState.cargando -> TextoSecundario("Consultando el sensor…", TintaSuave)
            porcentaje == null -> TextoSecundario(uiState.error ?: "Sin lecturas del sensor", Coral)
            else -> {
                val litros = uiState.litros?.let { " · ${formatearMiles(it)} L" } ?: ""
                Text("$porcentaje %$litros", Modifier.padding(top = 4.dp), fontFamily = FuenteTexto, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = AguaMedia)
                uiState.error?.let { TextoSecundario("Última lectura conocida · $it", Coral) }
                if (uiState.litros != null) {
                    TextButton(viewModel::onUsarMedicion, enabled = !uiState.guardando) {
                        Text(
                            if (uiState.guardando) "Actualizando…" else "Usar esta medición como nivel actual",
                            fontFamily = FuenteTexto, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AguaMedia
                        )
                    }
                }
            }
        }
        uiState.mensaje?.let { TextoSecundario(it, TintaSuave) }
    }
}

@Composable
private fun TextoSecundario(texto: String, color: androidx.compose.ui.graphics.Color) {
    Text(texto, fontFamily = FuenteTexto, fontSize = 12.5.sp, fontWeight = FontWeight.Medium, color = color)
}
