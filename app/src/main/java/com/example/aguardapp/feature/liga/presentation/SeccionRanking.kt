package com.example.aguardapp.feature.liga.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import com.example.aguardapp.core.ui.theme.AguaClara
import com.example.aguardapp.core.ui.theme.AguaMedia
import com.example.aguardapp.core.ui.theme.AguaProfunda
import com.example.aguardapp.core.ui.theme.Blanco
import com.example.aguardapp.core.ui.theme.Coral
import com.example.aguardapp.core.ui.theme.FuenteTexto
import com.example.aguardapp.core.ui.theme.Tinta
import com.example.aguardapp.core.ui.theme.TintaSuave
import com.example.aguardapp.feature.deposito.presentation.componentes.BotonPrincipal
import com.example.aguardapp.feature.deposito.presentation.componentes.BotonSecundario
import com.example.aguardapp.feature.deposito.presentation.componentes.TarjetaBlanca
import com.example.aguardapp.feature.liga.domain.FilaRanking
import com.example.aguardapp.feature.liga.domain.HOGARES_MINIMOS
import com.example.aguardapp.feature.liga.domain.hogaresQueFaltan
import com.example.aguardapp.feature.mapa.presentation.colorDelDistrito

private const val REFRESCO_MS = 30_000L

@Composable
fun ActualizarRankingCadaTanto(viewModel: RankingLigaViewModel) {
    val ciclo = LocalLifecycleOwner.current
    LaunchedEffect(ciclo) {
        ciclo.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                delay(REFRESCO_MS)
                viewModel.actualizar()
            }
        }
    }
}

@Composable
fun SeccionRanking(viewModel: RankingLigaViewModel) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    if (!estado.participa) {
        TarjetaConsentimiento(estado, viewModel::onElegirDistrito, viewModel::onParticipar)
    } else {
        ContenidoDelRanking(estado, viewModel::onDejarDeParticipar)
    }
}

@Composable
fun SeccionDuelos(viewModel: RankingLigaViewModel) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    if (!estado.participa) {
        TarjetaConsentimiento(estado, viewModel::onElegirDistrito, viewModel::onParticipar)
    } else {
        TarjetaDuelos(estado.duelos, estado.ubigeoElegido)
    }
}

@Composable
private fun TarjetaConsentimiento(estado: RankingLigaUiState, onElegir: (String) -> Unit, onParticipar: () -> Unit) {
    TarjetaBlanca {
        Titulo("Representa a tu distrito")
        Detalle("Si participas, cada semana se envía: tu distrito, tus litros por persona al día y tu puntaje, con un código anónimo.")
        Detalle("Nunca se envía tu nombre, tu ubicación exacta ni tus fotos. Puedes salir cuando quieras y se borran tus datos.")
        Column(Modifier.padding(top = 6.dp)) {
            estado.distritos.forEach { (ubigeo, nombre) ->
                Row(
                    Modifier.fillMaxWidth().clickable { onElegir(ubigeo) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = ubigeo == estado.ubigeoElegido, onClick = { onElegir(ubigeo) })
                    Box(Modifier.size(12.dp).clip(CircleShape).background(colorDelDistrito(ubigeo)))
                    Text(nombre, Modifier.padding(start = 8.dp), fontFamily = FuenteTexto, fontSize = 14.sp, color = Tinta)
                }
            }
        }
        BotonPrincipal("Acepto y quiero participar", onParticipar, habilitado = estado.ubigeoElegido != null)
    }
}

@Composable
private fun ContenidoDelRanking(estado: RankingLigaUiState, onSalir: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ResumenDeMiDistrito(estado)
        if (estado.filas.isEmpty()) {
            Detalle("Cargando el ranking…")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                estado.filas.forEach { FilaDeRanking(it, it.ubigeo == estado.ubigeoElegido) }
            }
        }
        Detalle(
            "El puntaje de un distrito es la mediana de sus hogares. Cada distrito necesita al menos " +
                "$HOGARES_MINIMOS hogares para competir."
        )
        estado.error?.let { Text(it, fontFamily = FuenteTexto, fontSize = 13.sp, color = Coral) }
        BotonSecundario("Salir y borrar mis datos", onSalir, Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun ResumenDeMiDistrito(estado: RankingLigaUiState) {
    val fila = filaDeMiDistrito(estado.filas, estado.ubigeoElegido) ?: return
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(AguaProfunda).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("Tu distrito", fontFamily = FuenteTexto, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AguaClara)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(14.dp).clip(CircleShape).background(colorDelDistrito(fila.ubigeo)))
            Text(fila.nombre, fontFamily = FuenteTexto, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Blanco)
        }
        val puesto = fila.puesto
        val puntaje = fila.puntaje
        Text(
            if (puesto != null && puntaje != null) {
                "Puesto $puesto de ${estado.filas.count { it.puesto != null }} · ${puntaje.roundToInt()} puntos"
            } else {
                textoHogaresQueFaltan(fila.hogaresQueFaltan) + " para competir"
            },
            fontFamily = FuenteTexto, fontSize = 14.sp, color = AguaClara
        )
    }
}

@Composable
private fun FilaDeRanking(fila: FilaRanking, esMiDistrito: Boolean) {
    val color = colorDelDistrito(fila.ubigeo)
    val puntaje = fila.puntaje
    val forma = RoundedCornerShape(16.dp)
    Row(
        Modifier.fillMaxWidth().height(IntrinsicSize.Min).clip(forma)
            .background(if (esMiDistrito) AguaClara.copy(alpha = 0.4f) else Blanco)
            .then(if (esMiDistrito) Modifier.border(2.dp, AguaMedia, forma) else Modifier)
    ) {
        Box(Modifier.width(6.dp).fillMaxHeight().background(if (puntaje != null) color else color.copy(alpha = 0.4f)))
        Row(
            Modifier.weight(1f).padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            InsigniaDePuesto(fila.puesto)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(fila.nombre, fontFamily = FuenteTexto, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Tinta)
                if (esMiDistrito) {
                    Text("Tu distrito", fontFamily = FuenteTexto, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = AguaMedia)
                }
                Text(textoHogares(fila.hogares, fila.verificados), fontFamily = FuenteTexto, fontSize = 12.sp, color = TintaSuave)
                BarraDeProgreso(if (puntaje != null) fraccionDelPuntaje(puntaje) else fraccionDeHogares(fila.hogares), color)
            }
            if (puntaje != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${puntaje.roundToInt()}", fontFamily = FuenteTexto, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Tinta)
                    Text("puntos", fontFamily = FuenteTexto, fontSize = 11.sp, color = TintaSuave)
                }
            } else {
                Text(
                    textoHogaresQueFaltan(fila.hogaresQueFaltan),
                    fontFamily = FuenteTexto, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TintaSuave
                )
            }
        }
    }
}

@Composable
private fun InsigniaDePuesto(puesto: Int?) {
    Box(Modifier.size(38.dp).clip(CircleShape).background(colorDeMedalla(puesto)), contentAlignment = Alignment.Center) {
        Text(puesto?.toString() ?: "–", fontFamily = FuenteTexto, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Tinta)
    }
}

@Composable
fun BarraDeProgreso(fraccion: Float, color: Color, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(7.dp).clip(CircleShape).background(GrisLiga)) {
        Box(Modifier.fillMaxWidth(fraccion).fillMaxHeight().clip(CircleShape).background(color))
    }
}

internal fun textoHogares(hogares: Int, verificados: Int): String {
    val total = when (hogares) {
        0 -> "Aún sin hogares"
        1 -> "1 hogar"
        else -> "$hogares hogares"
    }
    return when (verificados) {
        0 -> total
        1 -> "$total · 1 verificado"
        else -> "$total · $verificados verificados"
    }
}

internal fun textoHogaresQueFaltan(faltan: Int): String = when (faltan) {
    0 -> "Calculando"
    1 -> "Falta 1 hogar"
    else -> "Faltan $faltan hogares"
}
