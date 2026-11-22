package com.example.aguardapp.feature.deposito.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aguardapp.core.ui.theme.AguaMedia
import com.example.aguardapp.core.ui.theme.Blanco
import com.example.aguardapp.core.ui.theme.FuenteTexto
import com.example.aguardapp.core.ui.theme.IconosClarosEnBarraDeEstado

@Composable
internal fun CabeceraAzul(
    titulo: String,
    contexto: String,
    onVolver: () -> Unit,
    espacioInferior: Dp = 24.dp,
    accion: @Composable () -> Unit = {},
    contenido: @Composable ColumnScope.() -> Unit = {}
) {
    IconosClarosEnBarraDeEstado(claros = true)
    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp))
            .background(AguaMedia)
            .statusBarsPadding()
            .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = espacioInferior)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            VolverAlInicio(onVolver)
            Column(Modifier.weight(1f).padding(end = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(contexto, fontFamily = FuenteTexto, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Blanco)
                Text(
                    titulo,
                    fontFamily = FuenteTexto,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Blanco,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            accion()
        }
        contenido()
    }
}
