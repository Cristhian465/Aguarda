package com.example.aguardapp.feature.liga.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import com.example.aguardapp.core.ui.theme.AguaMedia
import com.example.aguardapp.core.ui.theme.FuenteTexto
import com.example.aguardapp.core.ui.theme.Tinta
import com.example.aguardapp.feature.deposito.presentation.componentes.TarjetaBlanca
import com.example.aguardapp.feature.liga.domain.Duelo

@Composable
fun TarjetaDuelos(duelos: List<Duelo>) {
    TarjetaBlanca {
        Titulo("Duelos de la semana")
        if (duelos.isEmpty()) {
            Detalle("Los duelos empiezan cuando dos distritos lleguen a 5 hogares.")
        }
        duelos.forEach { duelo -> FilaDuelo(duelo) }
    }
}

@Composable
private fun FilaDuelo(duelo: Duelo) {
    val puntosLocal = duelo.local.puntaje?.roundToInt()
    val puntosVisitante = duelo.visitante.puntaje?.roundToInt()
    Column(Modifier.padding(top = 8.dp)) {
        Text(
            "${duelo.local.nombre} $puntosLocal – $puntosVisitante ${duelo.visitante.nombre}",
            fontFamily = FuenteTexto, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Tinta
        )
        Text(
            duelo.ganador?.let { "Va ganando ${it.nombre}" } ?: "Van empatados",
            fontFamily = FuenteTexto, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = AguaMedia
        )
    }
}
