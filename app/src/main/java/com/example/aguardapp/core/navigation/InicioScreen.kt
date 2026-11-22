package com.example.aguardapp.core.navigation

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.aguardapp.R
import com.example.aguardapp.core.ui.theme.AguaOla
import com.example.aguardapp.core.ui.theme.AguaSuave
import com.example.aguardapp.core.ui.theme.AguaClara
import com.example.aguardapp.core.ui.theme.AguaMedia
import com.example.aguardapp.core.ui.theme.AguaProfunda
import com.example.aguardapp.core.ui.theme.Blanco
import com.example.aguardapp.core.ui.theme.Fondo
import com.example.aguardapp.core.ui.theme.IconosClarosEnBarraDeEstado
import com.example.aguardapp.core.ui.theme.TintaSuave
import com.example.aguardapp.feature.deposito.presentation.DepositoViewModel
import com.example.aguardapp.feature.deposito.presentation.TipoLlenado
import com.example.aguardapp.feature.deposito.presentation.formatearMiles
import org.koin.compose.viewmodel.koinViewModel

private data class Acceso(val ruta: String, val titulo: String, val detalle: String, @param:DrawableRes val icono: Int)

private val accesos = listOf(
    Acceso(Rutas.MAPA, "Mapa", "Puntos de agua cercanos", R.drawable.ic_mapa),
    Acceso(Rutas.REPRESAS, "Represas", "Nivel de los embalses", R.drawable.ic_represa),
    Acceso(Rutas.AHORRO, "Ahorro", "Retos y hábitos diarios", R.drawable.ic_ahorro),
    Acceso(Rutas.LIGA, "Liga del Agua", "Puntaje, ranking y duelos", R.drawable.ic_ahorro_trofeo),
    Acceso(Rutas.RECIBOS_EPS, "Recibos EPS", "Consulta tu recibo de agua", R.drawable.ic_recibo),
    Acceso(Rutas.FAMILIA, "Mi familia", "Comparte el tanque con tu casa", R.drawable.ic_familia)
)

@Composable
fun InicioScreen(
    onAbrir: (String) -> Unit,
    onLlenado: (TipoLlenado) -> Unit,
    viewModel: DepositoViewModel = koinViewModel()
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val deposito = estado.vista
    var elegirLlenado by rememberSaveable { mutableStateOf(false) }
    IconosClarosEnBarraDeEstado(claros = true)

    Column(Modifier.fillMaxSize().background(Fondo), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth().background(AguaMedia).statusBarsPadding(), contentAlignment = Alignment.Center) {
            Row(
                Modifier.widthIn(max = 620.dp).fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(AguaSuave), contentAlignment = Alignment.Center) {
                    Icon(painterResource(R.drawable.ic_gota), null, tint = Blanco)
                }
                Text("Aguarda", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = Blanco)
            }
        }
        Column(
            Modifier.widthIn(max = 620.dp).fillMaxWidth().weight(1f)
                .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Inicio", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold)
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(AguaProfunda)) {
                Canvas(Modifier.matchParentSize()) {
                    val x = size.width / 300f
                    val y = size.height / 150f
                    drawPath(Path().apply {
                        moveTo(300 * x, 55 * y); cubicTo(262 * x, 62 * y, 250 * x, 118 * y, 170 * x, 150 * y)
                        lineTo(300 * x, 150 * y); close()
                    }, AguaOla.copy(alpha = 0.25f))
                    drawPath(Path().apply {
                        moveTo(300 * x, 100 * y); cubicTo(272 * x, 104 * y, 250 * x, 138 * y, 215 * x, 150 * y)
                        lineTo(300 * x, 150 * y); close()
                    }, AguaOla.copy(alpha = 0.19f))
                }
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Mi depósito", style = MaterialTheme.typography.titleMedium, color = Blanco, fontWeight = FontWeight.Bold)
                    if (deposito != null) {
                        Text("${deposito.porcentaje} %", style = MaterialTheme.typography.displayMedium, color = Blanco, fontWeight = FontWeight.ExtraBold)
                        Text("${formatearMiles(deposito.nivelLitros)} L disponibles de ${formatearMiles(deposito.capacidadLitros)} L", color = AguaClara)
                    } else {
                        Text(if (estado.cargando) "Consultando nivel…" else "Registra un llenado para ver la reserva.", color = AguaClara)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { onAbrir(Rutas.MI_DEPOSITO) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = AguaClara, contentColor = AguaProfunda)
                        ) { Text("Ver depósito") }
                        OutlinedButton(
                            onClick = { elegirLlenado = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = AguaProfunda, contentColor = Blanco),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AguaClara)
                        ) { Text("Registrar llenado") }
                    }
                }
            }
            Text("Explorar Aguarda", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            accesos.chunked(2).forEach { fila ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    fila.forEach { acceso ->
                        OutlinedCard(onClick = { onAbrir(acceso.ruta) }, modifier = Modifier.weight(1f).aspectRatio(1f)) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(AguaClara), contentAlignment = Alignment.Center) {
                                    Icon(painterResource(acceso.icono), null, tint = AguaMedia)
                                }
                                Text(acceso.titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(acceso.detalle, style = MaterialTheme.typography.bodyMedium, color = TintaSuave)
                            }
                        }
                    }
                    if (fila.size == 1) Box(Modifier.weight(1f))
                }
            }
        }
    }

    if (elegirLlenado) {
        fun registrar(tipo: TipoLlenado) {
            elegirLlenado = false
            onLlenado(tipo)
        }
        AlertDialog(
            onDismissRequest = { elegirLlenado = false },
            title = { Text("Registrar llenado") },
            confirmButton = { TextButton(onClick = { registrar(TipoLlenado.COMPLETO) }) { Text("Completo") } },
            dismissButton = { TextButton(onClick = { registrar(TipoLlenado.PARCIAL) }) { Text("Parcial") } }
        )
    }
}
