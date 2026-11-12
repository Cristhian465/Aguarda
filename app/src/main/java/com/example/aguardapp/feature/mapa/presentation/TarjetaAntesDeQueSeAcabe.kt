package com.example.aguardapp.feature.mapa.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aguardapp.core.ui.theme.AguaMedia
import com.example.aguardapp.core.ui.theme.Coral
import com.example.aguardapp.core.ui.theme.CoralOscuro
import com.example.aguardapp.core.ui.theme.FuenteTexto
import com.example.aguardapp.core.ui.theme.Tenue
import com.example.aguardapp.core.ui.theme.Tinta

/** Tarjeta sobre la lista de cisternas: en coral si el agua no alcanza, en celeste si alcanza. */
@Composable
fun TarjetaAntesDeQueSeAcabe(aviso: AvisoDeAgua, modifier: Modifier = Modifier) {
    val fondo = if (aviso.faltaAgua) Coral.copy(alpha = 0.12f) else Tenue
    val colorTitulo = if (aviso.faltaAgua) CoralOscuro else AguaMedia
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(fondo).padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(aviso.titulo, fontFamily = FuenteTexto, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = colorTitulo)
        Text(
            aviso.detalle,
            modifier = Modifier.padding(top = 2.dp),
            fontFamily = FuenteTexto,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Medium,
            color = Tinta
        )
    }
}
