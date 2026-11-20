package com.example.aguardapp.feature.mapa.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aguardapp.core.ui.theme.AguaMedia
import com.example.aguardapp.core.ui.theme.Blanco
import com.example.aguardapp.core.ui.theme.Coral
import com.example.aguardapp.core.ui.theme.FuenteTexto
import com.example.aguardapp.core.ui.theme.Tinta
import com.example.aguardapp.core.ui.theme.TintaSuave
import com.example.aguardapp.core.ui.theme.TintaTenue
import com.example.aguardapp.feature.mapa.domain.model.TipoReporte

@Composable
fun DialogoReporte(
    estado: MapaUiState,
    panel: PanelReporte,
    onTipo: (TipoReporte) -> Unit,
    onDistrito: (String) -> Unit,
    onCisterna: (String) -> Unit,
    onEnviar: () -> Unit,
    onCerrar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCerrar,
        containerColor = Blanco,
        title = { Texto(if (panel.enviado) "Gracias por avisar" else "Avisa a tus vecinos", 18, Tinta, FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                panel.error?.let { Texto(it, 14, Coral, FontWeight.Bold) }
                Column(
                    Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (panel.enviado) {
                        Texto("Tu reporte se mostrará cuando otro vecino lo confirme.", 14, TintaSuave)
                    } else {
                        CuerpoDelReporte(estado, panel, onTipo, onDistrito, onCisterna)
                    }
                }
            }
        },
        confirmButton = {
            if (panel.enviado) {
                TextButton(onClick = onCerrar) { Texto("Cerrar", 15, AguaMedia, FontWeight.Bold) }
            } else {
                val habilitado = panel.listo && !panel.enviando
                TextButton(onClick = onEnviar, enabled = habilitado) {
                    Texto(if (panel.enviando) "Enviando…" else "Enviar", 15, if (habilitado) AguaMedia else TintaTenue, FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            if (!panel.enviado) TextButton(onClick = onCerrar) { Texto("Cancelar", 15, TintaSuave, FontWeight.Bold) }
        }
    )
}

@Composable
private fun CuerpoDelReporte(
    estado: MapaUiState,
    panel: PanelReporte,
    onTipo: (TipoReporte) -> Unit,
    onDistrito: (String) -> Unit,
    onCisterna: (String) -> Unit
) {
    Texto("Solo se comparte el distrito o la cisterna. Nunca tu ubicación, tu nombre ni tus fotos.", 13, TintaSuave)
    TipoReporte.entries.forEach { tipo -> Opcion(nombreDelReporte(tipo), panel.tipo == tipo) { onTipo(tipo) } }
    val tipo = panel.tipo
    if (tipo != null) {
        Texto(
            if (tipo.deCisterna) "¿Qué cisterna?" else "¿En qué distrito?",
            14, Tinta, FontWeight.ExtraBold, Modifier.padding(top = 10.dp)
        )
        if (tipo.deCisterna) {
            estado.puntos.forEach { punto -> Opcion(punto.nombre, panel.puntoId == punto.id) { onCisterna(punto.id) } }
        } else {
            estado.distritos.forEach { distrito ->
                Opcion(distrito.nombre, panel.ubigeo == distrito.ubigeo) { onDistrito(distrito.ubigeo) }
            }
        }
    }
}

@Composable
private fun Opcion(texto: String, elegida: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = elegida, onClick = onClick)
        Texto(texto, 14, Tinta)
    }
}

@Composable
private fun Texto(
    texto: String,
    tamano: Int,
    color: Color,
    peso: FontWeight = FontWeight.Medium,
    modifier: Modifier = Modifier
) {
    Text(texto, modifier, fontFamily = FuenteTexto, fontSize = tamano.sp, fontWeight = peso, color = color)
}
