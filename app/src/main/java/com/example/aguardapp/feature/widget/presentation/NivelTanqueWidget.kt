package com.example.aguardapp.feature.widget.presentation

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
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
import kotlin.math.roundToInt
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.flow.first
import org.koin.mp.KoinPlatform
import com.example.aguardapp.MainActivity
import com.example.aguardapp.R
import com.example.aguardapp.core.ui.theme.AguaClara
import com.example.aguardapp.core.ui.theme.Blanco
import com.example.aguardapp.feature.deposito.domain.repository.DepositoRepository
import com.example.aguardapp.feature.deposito.presentation.componentes.dibujarIndicadorNivel
import com.example.aguardapp.feature.deposito.presentation.formatearMiles
import com.example.aguardapp.feature.sensor.domain.repository.SensorRepository

private val MAXIMO_SIN_SENAL = 10.minutes
private const val PROPORCION = 2.1f

class NivelTanqueReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NivelTanqueWidget()
}

data class EstadoTanque(val porcentaje: Int, val litros: Int, val capacidad: Int, val conectado: Boolean)

class NivelTanqueWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val estado = leerEstado()
        provideContent { Contenido(estado) }
    }

    @OptIn(ExperimentalTime::class)
    private suspend fun leerEstado(): EstadoTanque {
        val koin = KoinPlatform.getKoin()
        val sensor = koin.get<SensorRepository>()
        val capacidad = koin.get<DepositoRepository>().observarPerfil().first()?.capacidad?.litros?.valor ?: 0.0
        val nueva = sensor.actualizarDesdeApi().getOrNull()
        val lectura = nueva ?: sensor.observarUltimaLectura().first()
        val conectado = nueva != null && Clock.System.now() - nueva.medidaEn < MAXIMO_SIN_SENAL
        return EstadoTanque(
            porcentaje = lectura?.porcentaje?.roundToInt() ?: 0,
            litros = lectura?.litros(capacidad)?.roundToInt() ?: 0,
            capacidad = capacidad.roundToInt(),
            conectado = conectado
        )
    }
}

@Composable
private fun Contenido(estado: EstadoTanque) {
    val context = LocalContext.current
    val tamano = LocalSize.current
    val base = minOf(tamano.height, tamano.width / PROPORCION)
    val escalaLetra = context.resources.configuration.fontScale
    fun letra(proporcion: Float) = (base.value * proporcion / escalaLetra).sp
    val margen = base * 0.1f
    val altoTanque = base * 0.8f
    val anchoTanque = altoTanque * 0.56f
    val blanco = ColorProvider(Blanco)
    Box(
        GlanceModifier.fillMaxSize()
            .background(ImageProvider(R.drawable.widget_fondo))
            .cornerRadius(base * 0.24f)
            .clickable(actionStartActivity<MainActivity>()),
        contentAlignment = Alignment.Center
    ) {
        Box(GlanceModifier.size(base * PROPORCION, base).padding(margen)) {
            Row(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Image(
                    ImageProvider(dibujarTanque(context, estado.porcentaje / 100f, anchoTanque, altoTanque)),
                    contentDescription = null,
                    modifier = GlanceModifier.size(anchoTanque, altoTanque)
                )
                Spacer(GlanceModifier.width(margen))
                Column {
                    Text(
                        "${estado.porcentaje} %",
                        maxLines = 1,
                        style = TextStyle(color = blanco, fontSize = letra(0.3f), fontWeight = FontWeight.Bold)
                    )
                    Text(
                        "del tanque disponible",
                        maxLines = 1,
                        style = TextStyle(color = blanco, fontSize = letra(0.1f), fontWeight = FontWeight.Medium)
                    )
                    Spacer(GlanceModifier.height(base * 0.04f))
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
                        Text(
                            "${formatearMiles(estado.litros)} L de ${formatearMiles(estado.capacidad)} L",
                            maxLines = 1,
                            style = TextStyle(color = blanco, fontSize = letra(0.085f), fontWeight = FontWeight.Medium)
                        )
                    }
                }
            }
            Box(GlanceModifier.fillMaxSize(), contentAlignment = Alignment.TopEnd) {
                Box(
                    GlanceModifier.size(base * 0.24f).background(ImageProvider(R.drawable.widget_circulo)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        ImageProvider(if (estado.conectado) R.drawable.ic_wifi else R.drawable.ic_wifi_off),
                        contentDescription = if (estado.conectado) "Tanque conectado" else "Tanque sin conexión",
                        modifier = GlanceModifier.size(base * 0.13f),
                        colorFilter = ColorFilter.tint(ColorProvider(if (estado.conectado) Blanco else Blanco.copy(alpha = 0.5f)))
                    )
                }
            }
        }
    }
}

private fun dibujarTanque(context: Context, fraccion: Float, ancho: Dp, alto: Dp): Bitmap {
    val densidad = context.resources.displayMetrics.density
    val anchoPx = (ancho.value * densidad).roundToInt().coerceAtLeast(1)
    val altoPx = (alto.value * densidad).roundToInt().coerceAtLeast(1)
    val imagen = ImageBitmap(anchoPx, altoPx)
    CanvasDrawScope().draw(Density(densidad), LayoutDirection.Ltr, Canvas(imagen), Size(anchoPx.toFloat(), altoPx.toFloat())) {
        dibujarIndicadorNivel(fraccion, conOla = true)
    }
    return imagen.asAndroidBitmap()
}
