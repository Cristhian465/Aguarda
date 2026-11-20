package com.example.aguardapp.feature.liga.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aguardapp.R
import com.example.aguardapp.core.ui.theme.AguaClara
import com.example.aguardapp.core.ui.theme.AguaMedia
import com.example.aguardapp.core.ui.theme.AguaProfunda
import com.example.aguardapp.core.ui.theme.Blanco
import com.example.aguardapp.core.ui.theme.FuenteTexto
import com.example.aguardapp.core.ui.theme.TintaSuave
import com.example.aguardapp.feature.mapa.presentation.colorDelDistrito

enum class SeccionLiga(val titulo: String, val subtitulo: String, val icono: Int) {
    TANQUE("Mi tanque", "Marca cuánta agua tienes", R.drawable.ic_liga_tanque),
    CONSUMO("Mi consumo", "Litros por persona", R.drawable.ic_liga_consumo),
    PUNTAJE("Mi puntaje", "Cómo vas esta semana", R.drawable.ic_liga_puntaje),
    RANKING("Ranking", "Distritos de Tacna", R.drawable.ic_liga_ranking),
    DUELOS("Duelos", "Distrito contra distrito", R.drawable.ic_liga_duelos)
}

@Composable
fun MenuLiga(estado: LigaUiState, ranking: RankingLigaUiState, onAbrir: (SeccionLiga) -> Unit) {
    ResumenDeLaSemana(estado, ranking)
    Text("Explorar la Liga", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    SeccionLiga.entries.chunked(2).forEach { fila ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            fila.forEach { seccion ->
                TarjetaDeSeccion(seccion, detalleDe(seccion, estado, ranking), { onAbrir(seccion) }, Modifier.weight(1f))
            }
            if (fila.size == 1) Box(Modifier.weight(1f))
        }
    }
}

internal fun detalleDe(seccion: SeccionLiga, estado: LigaUiState, ranking: RankingLigaUiState): String = when (seccion) {
    SeccionLiga.TANQUE -> seccion.subtitulo
    SeccionLiga.CONSUMO -> detalleDeConsumo(estado.resumen)
    SeccionLiga.PUNTAJE -> detalleDePuntaje(estado.resumen)
    SeccionLiga.RANKING -> if (ranking.participa) detalleDeMiDistrito(ranking.filas, ranking.ubigeoElegido) else "Representa a tu distrito"
    SeccionLiga.DUELOS -> if (ranking.participa) detalleDeDuelos(ranking.duelos) else "Participa para verlos"
}

@Composable
private fun TarjetaDeSeccion(seccion: SeccionLiga, detalle: String, onClick: () -> Unit, modifier: Modifier) {
    OutlinedCard(onClick = onClick, modifier = modifier.aspectRatio(1f)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(AguaClara), contentAlignment = Alignment.Center) {
                Icon(painterResource(seccion.icono), null, tint = AguaMedia)
            }
            Text(seccion.titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(detalle, style = MaterialTheme.typography.bodyMedium, color = TintaSuave)
        }
    }
}

@Composable
private fun ResumenDeLaSemana(estado: LigaUiState, ranking: RankingLigaUiState) {
    val puntaje = estado.resumen?.puntaje
    val miDistrito = filaDeMiDistrito(ranking.filas, ranking.ubigeoElegido)
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(AguaProfunda).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text("Tu semana", fontFamily = FuenteTexto, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AguaClara)
        Text(
            puntaje?.let { "${it.total} / 100" } ?: "Sin puntaje aún",
            fontFamily = FuenteTexto, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, color = Blanco
        )
        Text(detalleDeConsumo(estado.resumen), fontFamily = FuenteTexto, fontSize = 14.sp, color = AguaClara)
        if (ranking.participa && miDistrito != null) {
            Row(
                Modifier.padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(12.dp).clip(RoundedCornerShape(50)).background(colorDelDistrito(miDistrito.ubigeo)))
                Text(
                    "${miDistrito.nombre}: ${detalleDeMiDistrito(ranking.filas, ranking.ubigeoElegido)}",
                    fontFamily = FuenteTexto, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Blanco
                )
            }
        }
    }
}
