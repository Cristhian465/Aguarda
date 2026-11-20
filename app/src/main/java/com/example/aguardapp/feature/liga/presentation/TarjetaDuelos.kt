package com.example.aguardapp.feature.liga.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import com.example.aguardapp.core.ui.theme.AguaMedia
import com.example.aguardapp.core.ui.theme.AguaProfunda
import com.example.aguardapp.core.ui.theme.Blanco
import com.example.aguardapp.core.ui.theme.FuenteTexto
import com.example.aguardapp.core.ui.theme.Tinta
import com.example.aguardapp.core.ui.theme.TintaSuave
import com.example.aguardapp.feature.deposito.presentation.componentes.TarjetaBlanca
import com.example.aguardapp.feature.liga.domain.Duelo
import com.example.aguardapp.feature.liga.domain.FilaRanking
import com.example.aguardapp.feature.liga.domain.HOGARES_MINIMOS
import com.example.aguardapp.feature.mapa.presentation.colorDelDistrito
import com.example.aguardapp.feature.mapa.presentation.nombreCorto

@Composable
fun TarjetaDuelos(duelos: List<Duelo>, miUbigeo: String?) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (duelos.isEmpty()) {
            TarjetaBlanca {
                Titulo("Duelos de la semana")
                Detalle("Los duelos empiezan cuando dos distritos lleguen a $HOGARES_MINIMOS hogares.")
            }
        } else {
            Detalle("Cada semana los distritos que ya compiten se enfrentan; los rivales cambian.")
            duelos.forEach { TarjetaDeDuelo(it, miUbigeo) }
        }
    }
}

@Composable
private fun TarjetaDeDuelo(duelo: Duelo, miUbigeo: String?) {
    val puntosLocal = duelo.local.puntaje ?: 0.0
    val puntosVisitante = duelo.visitante.puntaje ?: 0.0
    val colorLocal = colorDelDistrito(duelo.local.ubigeo)
    val colorVisitante = colorDelDistrito(duelo.visitante.ubigeo)
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Blanco).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(Modifier.fillMaxWidth()) {
            LadoDelDuelo(duelo.local, puntosLocal, miUbigeo, Modifier.weight(1f))
            Text(
                "VS",
                Modifier.align(Alignment.CenterVertically).clip(CircleShape).background(AguaProfunda).padding(horizontal = 10.dp, vertical = 6.dp),
                fontFamily = FuenteTexto, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Blanco
            )
            LadoDelDuelo(duelo.visitante, puntosVisitante, miUbigeo, Modifier.weight(1f))
        }
        val fraccion = fraccionDelDuelo(puntosLocal, puntosVisitante).coerceIn(FRACCION_MINIMA, 1f - FRACCION_MINIMA)
        Row(Modifier.fillMaxWidth().height(10.dp).clip(CircleShape)) {
            Box(Modifier.weight(fraccion).height(10.dp).background(colorLocal))
            Box(Modifier.weight(1f - fraccion).height(10.dp).background(colorVisitante))
        }
        val ganador = duelo.ganador
        Text(
            ganador?.let { "Va ganando ${nombreCorto(it.nombre)}" } ?: "Van empatados",
            Modifier.align(Alignment.CenterHorizontally).clip(CircleShape)
                .background(ganador?.let { colorDelDistrito(it.ubigeo).copy(alpha = 0.35f) } ?: GrisLiga)
                .padding(horizontal = 14.dp, vertical = 6.dp),
            fontFamily = FuenteTexto, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Tinta
        )
    }
}

@Composable
private fun LadoDelDuelo(fila: FilaRanking, puntos: Double, miUbigeo: String?, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(14.dp).clip(CircleShape).background(colorDelDistrito(fila.ubigeo)))
        Text(
            nombreCorto(fila.nombre),
            fontFamily = FuenteTexto, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Tinta, textAlign = TextAlign.Center
        )
        if (fila.ubigeo == miUbigeo) {
            Text("Tu distrito", fontFamily = FuenteTexto, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = AguaMedia)
        }
        Text("${puntos.roundToInt()}", fontFamily = FuenteTexto, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = Tinta)
        Text("puntos", fontFamily = FuenteTexto, fontSize = 11.sp, color = TintaSuave)
    }
}

private const val FRACCION_MINIMA = 0.08f
