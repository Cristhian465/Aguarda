package com.example.aguardapp.feature.liga.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import org.koin.compose.viewmodel.koinViewModel
import com.example.aguardapp.core.ui.theme.Coral
import com.example.aguardapp.core.ui.theme.FuenteTexto
import com.example.aguardapp.core.ui.theme.Tinta
import com.example.aguardapp.core.ui.theme.TintaSuave
import com.example.aguardapp.feature.deposito.presentation.componentes.BotonPrincipal
import com.example.aguardapp.feature.deposito.presentation.componentes.BotonSecundario
import com.example.aguardapp.feature.deposito.presentation.componentes.TarjetaBlanca
import com.example.aguardapp.feature.liga.domain.FilaRanking

private const val REFRESCO_MS = 30_000L

@Composable
fun SeccionRanking(viewModel: RankingLigaViewModel = koinViewModel()) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val ciclo = LocalLifecycleOwner.current
    LaunchedEffect(ciclo) {
        // Se actualiza solo mientras la pantalla está visible.
        ciclo.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                delay(REFRESCO_MS)
                viewModel.actualizar()
            }
        }
    }
    when {
        !estado.conectado -> TarjetaBlanca {
            Titulo("Ranking de distritos")
            Detalle("El ranking todavía no está conectado. Tu medición y tu puntaje ya funcionan en este teléfono.")
        }
        !estado.participa -> TarjetaConsentimiento(estado, viewModel::onElegirDistrito, viewModel::onParticipar)
        else -> Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            TarjetaRanking(estado, viewModel::onDejarDeParticipar)
            TarjetaDuelos(estado.duelos)
        }
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
                    Text(nombre, fontFamily = FuenteTexto, fontSize = 14.sp, color = Tinta)
                }
            }
        }
        BotonPrincipal("Acepto y quiero participar", onParticipar, habilitado = estado.ubigeoElegido != null)
    }
}

@Composable
private fun TarjetaRanking(estado: RankingLigaUiState, onSalir: () -> Unit) {
    TarjetaBlanca {
        Titulo("Ranking de distritos")
        Detalle("Cada distrito necesita al menos 5 hogares para competir.")
        estado.filas.forEach { FilaDeRanking(it, it.ubigeo == estado.ubigeoElegido) }
        estado.error?.let { Text(it, fontFamily = FuenteTexto, fontSize = 13.sp, color = Coral) }
        BotonSecundario("Dejar de participar y borrar mis datos", onSalir, Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun FilaDeRanking(fila: FilaRanking, esMiDistrito: Boolean) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(fila.puesto?.let { "$it." } ?: "–", fontFamily = FuenteTexto, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Tinta)
        Column(Modifier.weight(1f)) {
            Text(
                fila.nombre + if (esMiDistrito) " (tu distrito)" else "",
                fontFamily = FuenteTexto, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Tinta
            )
            Text(textoHogares(fila.hogares, fila.verificados), fontFamily = FuenteTexto, fontSize = 12.sp, color = TintaSuave)
        }
        Text(
            fila.puntaje?.let { "${it.roundToInt()} pts" } ?: "Sin datos",
            fontFamily = FuenteTexto, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TintaSuave
        )
    }
}

internal fun textoHogares(hogares: Int, verificados: Int): String {
    val total = if (hogares == 1) "1 hogar" else "$hogares hogares"
    return if (verificados > 0) "$total · $verificados verificados" else total
}
