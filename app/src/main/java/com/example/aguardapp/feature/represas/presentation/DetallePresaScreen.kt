package com.example.aguardapp.feature.represas.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import com.example.aguardapp.core.ui.theme.AguaMedia
import com.example.aguardapp.core.ui.theme.Divisor
import com.example.aguardapp.core.ui.theme.FuenteTexto
import com.example.aguardapp.core.ui.theme.IconosClarosEnBarraDeEstado
import com.example.aguardapp.core.ui.theme.Tenue
import com.example.aguardapp.core.ui.theme.Tinta
import com.example.aguardapp.core.ui.theme.TintaSuave
import com.example.aguardapp.core.ui.theme.TintaTenue
import com.example.aguardapp.feature.deposito.presentation.componentes.BarraSuperior
import com.example.aguardapp.feature.deposito.presentation.componentes.TarjetaBlanca
import com.example.aguardapp.feature.deposito.presentation.formatearMiles
import com.example.aguardapp.feature.represas.domain.model.Presa

@Composable
fun DetallePresaScreen(
    nombre: String,
    onVolver: () -> Unit,
    viewModel: DetallePresaViewModel = koinViewModel { parametersOf(nombre) }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val presa = uiState.presa
    IconosClarosEnBarraDeEstado(claros = false)
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        BarraSuperior(presa?.nombre ?: nombre, presa?.distrito?.let { "Represa en $it" } ?: "Represa", onVolver)
        when {
            uiState.cargando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            presa == null -> Text(
                "No encontramos esta represa entre los datos guardados.",
                Modifier.padding(24.dp), fontFamily = FuenteTexto, fontSize = 14.sp, color = TintaSuave
            )
            else -> Contenido(presa, uiState)
        }
    }
}

@Composable
private fun Contenido(presa: Presa, uiState: DetallePresaUiState) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 24.dp, end = 24.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val color = colorDelUso(presa.usoPrincipal)
            Etiqueta(describirUsos(presa.usos), fondo = color.copy(alpha = 0.16f), colorTexto = Tinta)
            uiState.abasteceA?.let { Etiqueta(it, fondo = Tenue, colorTexto = AguaMedia) }
        }
        uiState.nota?.let { Text(it, fontFamily = FuenteTexto, fontSize = 13.sp, color = TintaSuave) }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Dato("Volumen útil", presa.volumenUtilHm3?.let { "${formatearDecimal(it)} hm³" }, Modifier.weight(1f))
            Dato("Altitud", presa.altitudMetros?.let { "${formatearDecimal(it)} m" }, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Dato("Altura de la presa", presa.alturaMetros?.let { "${formatearDecimal(it)} m" }, Modifier.weight(1f))
            Dato("Opera desde", presa.anioInicio?.toString(), Modifier.weight(1f))
        }

        val capacidad = uiState.capacidadTanqueLitros
        val tanques = capacidad?.let(presa::tanquesEquivalentes)
        if (capacidad != null && tanques != null) {
            Text(
                "Guarda lo mismo que ${describirTanques(tanques)} como el tuyo (${formatearMiles(capacidad.toInt())} L).",
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Tenue).padding(14.dp),
                fontFamily = FuenteTexto, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AguaMedia
            )
        }

        TarjetaBlanca(padding = 6) {
            FilaDeDato("Uso según la ANA", describirUsos(presa.usos))
            HorizontalDivider(color = Divisor)
            FilaDeDato("Río o quebrada", presa.rio)
            HorizontalDivider(color = Divisor)
            FilaDeDato("Distrito", presa.distrito)
            HorizontalDivider(color = Divisor)
            FilaDeDato("Operador", presa.operador)
        }
        Text(
            "Fuente: Autoridad Nacional del Agua (ANA), inventario de presas.",
            fontFamily = FuenteTexto, fontSize = 11.sp, color = TintaTenue
        )
    }
}

@Composable
private fun Dato(etiqueta: String, valor: String?, modifier: Modifier) {
    TarjetaBlanca(modifier, padding = 14) {
        Text(etiqueta, fontFamily = FuenteTexto, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TintaSuave)
        Text(
            valor ?: "Sin dato",
            fontFamily = FuenteTexto, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = if (valor == null) TintaTenue else Tinta
        )
    }
}

@Composable
private fun FilaDeDato(etiqueta: String, valor: String?) {
    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(etiqueta, fontFamily = FuenteTexto, fontSize = 13.sp, color = TintaSuave)
        Text(
            valor ?: "Sin dato",
            Modifier.weight(1f),
            textAlign = TextAlign.End,
            fontFamily = FuenteTexto, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = if (valor == null) TintaTenue else Tinta
        )
    }
}
