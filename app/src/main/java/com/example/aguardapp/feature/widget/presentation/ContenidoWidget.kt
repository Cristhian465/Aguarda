package com.example.aguardapp.feature.widget.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.aguardapp.MainActivity
import com.example.aguardapp.R
import com.example.aguardapp.core.ui.theme.AguaClara
import com.example.aguardapp.core.ui.theme.Blanco
import com.example.aguardapp.feature.deposito.presentation.formatearMiles
import com.example.aguardapp.feature.widget.domain.model.EstadoTanque

private const val PROPORCION = 2.1f

@Composable
fun ContenidoWidget(estado: EstadoTanque) {
    val context = LocalContext.current
    val tamano = LocalSize.current
    val base = minOf(tamano.height, tamano.width / PROPORCION)
    val escalaLetra = context.resources.configuration.fontScale
    fun letra(proporcion: Float): TextUnit = (base.value * proporcion / escalaLetra).sp
    val altoTanque = base * 0.8f
    val anchoTanque = altoTanque * 0.56f

    Box(
        GlanceModifier.fillMaxSize()
            .background(ImageProvider(R.drawable.widget_fondo))
            .clickable(actionStartActivity<MainActivity>()),
        contentAlignment = Alignment.Center
    ) {
        Box(GlanceModifier.size(base * PROPORCION, base).padding(base * 0.1f)) {
            Row(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Image(
                    ImageProvider(imagenTanque(context, estado.porcentaje, anchoTanque, altoTanque)),
                    contentDescription = null,
                    modifier = GlanceModifier.size(anchoTanque, altoTanque)
                )
                Spacer(GlanceModifier.width(base * 0.1f))
                Column {
                    Texto("${estado.porcentaje} %", letra(0.3f), FontWeight.Bold)
                    Texto("del tanque disponible", letra(0.1f), FontWeight.Medium)
                    Spacer(GlanceModifier.height(base * 0.04f))
                    Litros(estado, base, letra(0.085f))
                }
            }
            IconoConexion(estado.conectado, base)
        }
    }
}

@Composable
private fun Texto(texto: String, tamano: TextUnit, peso: FontWeight) {
    Text(texto, maxLines = 1, style = TextStyle(color = ColorProvider(Blanco), fontSize = tamano, fontWeight = peso))
}

@Composable
private fun Litros(estado: EstadoTanque, base: Dp, tamano: TextUnit) {
    Row(
        GlanceModifier.background(ImageProvider(R.drawable.widget_pastilla))
            .padding(horizontal = base * 0.06f, vertical = base * 0.03f),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            ImageProvider(R.drawable.ic_gota),
            contentDescription = null,
            modifier = GlanceModifier.size(base * 0.1f),
            colorFilter = ColorFilter.tint(ColorProvider(AguaClara))
        )
        Spacer(GlanceModifier.width(base * 0.04f))
        Texto("${formatearMiles(estado.litros)} L de ${formatearMiles(estado.capacidad)} L", tamano, FontWeight.Medium)
    }
}

@Composable
private fun IconoConexion(conectado: Boolean, base: Dp) {
    Box(GlanceModifier.fillMaxSize(), contentAlignment = Alignment.TopEnd) {
        Box(
            GlanceModifier.size(base * 0.24f).background(ImageProvider(R.drawable.widget_circulo)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                ImageProvider(if (conectado) R.drawable.ic_wifi else R.drawable.ic_wifi_off),
                contentDescription = if (conectado) "Tanque conectado" else "Tanque sin conexión",
                modifier = GlanceModifier.size(base * 0.13f),
                colorFilter = ColorFilter.tint(ColorProvider(if (conectado) Blanco else Blanco.copy(alpha = 0.5f)))
            )
        }
    }
}
