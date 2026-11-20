package com.example.aguardapp.feature.liga.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import kotlinx.datetime.LocalDate
import com.example.aguardapp.core.ui.theme.Agua
import com.example.aguardapp.core.ui.theme.AguaMedia
import com.example.aguardapp.core.ui.theme.Coral
import com.example.aguardapp.core.ui.theme.FuenteTexto
import com.example.aguardapp.core.ui.theme.Tinta
import com.example.aguardapp.core.ui.theme.TintaSuave
import com.example.aguardapp.feature.deposito.presentation.componentes.TarjetaBlanca
import com.example.aguardapp.feature.liga.domain.LitrosSemana

private val ALTO_DE_LAS_BARRAS = 120.dp
private val ALTO_DE_LA_CIFRA = 20.dp
private val MESES = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")

@Composable
fun TarjetaHistorial(historial: List<LitrosSemana>, lineaBase: Double?) {
    TarjetaBlanca {
        Titulo("Tus litros por semana")
        if (historial.none { it.litrosPorPersonaDia != null }) {
            Detalle("Aquí verás cuántos litros por persona gastas cada semana, apenas tengas tu primera medición.")
        } else {
            Detalle(
                if (lineaBase != null) "Litros por persona al día. La línea roja es tu consumo habitual."
                else "Litros por persona al día."
            )
            BarrasSemanales(historial, lineaBase)
        }
    }
}

@Composable
private fun BarrasSemanales(historial: List<LitrosSemana>, lineaBase: Double?) {
    val maximo = maximoDelGrafico(historial, lineaBase)
    Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.fillMaxWidth().height(ALTO_DE_LAS_BARRAS + ALTO_DE_LA_CIFRA)) {
            Row(
                Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                historial.forEachIndexed { indice, semana ->
                    BarraDeLaSemana(semana.litrosPorPersonaDia, maximo, indice == historial.lastIndex, Modifier.weight(1f))
                }
            }
            lineaBase?.let {
                Box(
                    Modifier.align(Alignment.BottomStart)
                        .padding(bottom = ALTO_DE_LAS_BARRAS * alturaRelativa(it, maximo))
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(Coral)
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            historial.forEachIndexed { indice, semana ->
                Text(
                    etiquetaDeSemana(semana.lunes, indice == historial.lastIndex),
                    Modifier.weight(1f),
                    fontFamily = FuenteTexto, fontSize = 11.sp, color = TintaSuave, textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun BarraDeLaSemana(litros: Double?, maximo: Double, esActual: Boolean, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
        Text(
            litros?.let { "${it.roundToInt()}" } ?: "–",
            fontFamily = FuenteTexto, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (esActual) Tinta else TintaSuave
        )
        if (litros != null) {
            Box(
                Modifier.fillMaxWidth()
                    .height(ALTO_DE_LAS_BARRAS * alturaRelativa(litros, maximo))
                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                    .background(if (esActual) AguaMedia else Agua)
            )
        }
    }
}

internal fun maximoDelGrafico(historial: List<LitrosSemana>, lineaBase: Double?): Double =
    (historial.mapNotNull { it.litrosPorPersonaDia } + listOfNotNull(lineaBase)).maxOrNull()?.coerceAtLeast(1.0) ?: 1.0

internal fun alturaRelativa(litros: Double, maximo: Double): Float =
    (litros / maximo).coerceIn(0.0, 1.0).toFloat()

internal fun etiquetaDeSemana(lunes: LocalDate, esActual: Boolean): String =
    if (esActual) "Esta" else "${lunes.dayOfMonth} ${MESES[lunes.monthNumber - 1]}"
