package com.example.aguardapp.feature.ahorro.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.aguardapp.R
import com.example.aguardapp.core.ui.theme.Agua
import com.example.aguardapp.core.ui.theme.AguaProfunda
import com.example.aguardapp.core.ui.theme.Blanco
import com.example.aguardapp.core.ui.theme.Fondo
import com.example.aguardapp.core.ui.theme.IconosClarosEnBarraDeEstado
import com.example.aguardapp.feature.deposito.presentation.VolverAlInicio
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AhorroScreen(onVolver: () -> Unit, viewModel: AhorroViewModel = koinViewModel()) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.actualizarFecha() }
    IconosClarosEnBarraDeEstado(claros = true)

    Column(Modifier.fillMaxSize().background(Fondo).verticalScroll(rememberScrollState())) {
        CabeceraAhorro(estado.diasDeRacha, onVolver)
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
            RachaSemanal(estado.diasDeRacha, estado.hoy, estado.diasCumplidos)
            CalendarioAhorro(estado.hoy, estado.diasCumplidos)
            EtiquetaSeccion("RETOS DE LA SEMANA", Modifier.padding(top = 22.dp, bottom = 8.dp))
            RetosSemana(estado.retosCumplidosHoy, viewModel::alternarReto)
            EtiquetaSeccion("INSIGNIAS", Modifier.padding(top = 18.dp, bottom = 8.dp))
            InsigniasAhorro(estado.diasCumplidos.size, estado.litrosEstimados)
        }
    }
}

@Composable
private fun CabeceraAhorro(dias: Int, onVolver: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().clipToBounds()
            .background(Brush.horizontalGradient(listOf(AguaProfunda, Agua)))
    ) {
        Box(Modifier.matchParentSize()) {
            Box(
                Modifier.align(Alignment.TopEnd).offset(x = 24.dp, y = (-95).dp)
                    .size(190.dp).background(Blanco.copy(alpha = 0.12f), CircleShape)
            )
        }
        Column(Modifier.statusBarsPadding().padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 20.dp)) {
            VolverAlInicio(onVolver)
            Text("Tu ahorro", color = Blanco, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("Esta semana · actualiza tus retos de hoy", color = Blanco.copy(alpha = 0.85f), fontSize = 11.sp)
            Row(
                Modifier.fillMaxWidth().padding(top = 14.dp)
                    .background(Blanco.copy(alpha = 0.13f), RoundedCornerShape(15.dp))
                    .padding(horizontal = 13.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(painterResource(R.drawable.ic_ahorro_racha), null, Modifier.size(22.dp), tint = Blanco)
                Column(Modifier.padding(start = 8.dp)) {
                    Text(if (dias == 1) "1 día" else "$dias días", color = Blanco, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text("de racha completando retos", color = Blanco.copy(alpha = 0.85f), fontSize = 10.sp)
                }
            }
        }
    }
}
