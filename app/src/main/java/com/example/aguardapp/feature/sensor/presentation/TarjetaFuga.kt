package com.example.aguardapp.feature.sensor.presentation

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import com.example.aguardapp.core.ui.theme.AguaMedia
import com.example.aguardapp.core.ui.theme.Coral
import com.example.aguardapp.core.ui.theme.FuenteTexto
import com.example.aguardapp.core.ui.theme.Tinta
import com.example.aguardapp.core.ui.theme.TintaSuave
import com.example.aguardapp.feature.deposito.presentation.componentes.TarjetaBlanca
import com.example.aguardapp.feature.deposito.presentation.formatearMiles
import com.example.aguardapp.feature.sensor.domain.usecase.EstadoFuga

@Composable
fun TarjetaFuga(modifier: Modifier = Modifier, viewModel: FugaViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    when {
        uiState.estado == EstadoFuga.POSIBLE_FUGA && !uiState.descartada -> TarjetaBlanca(modifier) {
            Titulo("Posible fuga en tu tanque", Coral)
            Detalle(
                "Anoche, entre la 1 y las 5 a. m., el nivel bajó ${formatearMiles(uiState.litrosPerdidos)} L poco a poco, " +
                    "sin señales de que alguien usara agua. A ese ritmo se pierden unos ${formatearMiles(uiState.litrosAlMes)} L al mes."
            )
            Detalle("Revisa el inodoro, la boya del tanque y las llaves que gotean.")
            TextButton(viewModel::onFuiYo) {
                Text("Fui yo: usé agua de madrugada", fontFamily = FuenteTexto, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AguaMedia)
            }
        }
        uiState.estado == EstadoFuga.SIN_FUGA -> TarjetaBlanca(modifier) {
            Titulo("Sin fugas anoche", Tinta)
            Detalle("El nivel del tanque se mantuvo estable entre la 1 y las 5 a. m.")
        }
    }
}

@Composable
private fun Titulo(texto: String, color: Color) {
    Text(texto, fontFamily = FuenteTexto, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = color)
}

@Composable
private fun Detalle(texto: String) {
    Text(texto, fontFamily = FuenteTexto, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TintaSuave)
}
