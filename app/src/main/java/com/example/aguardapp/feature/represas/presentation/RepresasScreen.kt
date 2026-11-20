package com.example.aguardapp.feature.represas.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.example.aguardapp.core.ui.theme.FuenteTexto
import com.example.aguardapp.core.ui.theme.IconosClarosEnBarraDeEstado
import com.example.aguardapp.core.ui.theme.Tinta
import com.example.aguardapp.core.ui.theme.TintaSuave
import com.example.aguardapp.feature.deposito.presentation.componentes.BarraSuperior
import com.example.aguardapp.feature.deposito.presentation.componentes.TarjetaBlanca
import com.example.aguardapp.feature.represas.domain.model.Presa

private val SIERRA_DE_TACNA = LatLng(-17.4, -70.1)

@Composable
fun RepresasScreen(onVolver: () -> Unit, viewModel: RepresasViewModel = koinViewModel()) {
    val presas by viewModel.presas.collectAsStateWithLifecycle()
    IconosClarosEnBarraDeEstado(claros = false)
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        BarraSuperior("Represas de Tacna", "Datos de la Autoridad Nacional del Agua", onVolver = onVolver)
        when {
            presas.isNotEmpty() -> ListaDeRepresas(presas, viewModel.sinConexion)
            viewModel.cargando -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            else -> TarjetaBlanca(Modifier.padding(20.dp)) {
                Texto("Sin conexión con la ANA", negrita = true)
                Texto("Se necesita internet la primera vez; después se ven sin conexión.")
                TextButton(onClick = viewModel::actualizar) { Text("Reintentar") }
            }
        }
    }
}

@Composable
private fun ListaDeRepresas(presas: List<Presa>, sinConexion: Boolean) = Column {
    // mapa fuera de la lista para poder moverlo
    MapaDeRepresas(presas)
    LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (sinConexion) item { Texto("Sin conexión: mostrando los datos guardados.") }
        items(presas, key = { it.nombre }) { presa ->
            TarjetaBlanca {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Texto(presa.nombre, negrita = true)
                    Texto(presa.volumenHm3?.let { "$it hm³" } ?: "s/d", negrita = true)
                }
                Texto("${presa.uso} · ${presa.distrito}")
            }
        }
    }
}

// mapa con los marcadores
@Composable
private fun MapaDeRepresas(presas: List<Presa>) {
    val camara = rememberCameraPositionState { position = CameraPosition.fromLatLngZoom(SIERRA_DE_TACNA, 8.3f) }
    GoogleMap(
        modifier = Modifier.padding(start = 20.dp, top = 20.dp, end = 20.dp)
            .fillMaxWidth().height(240.dp).clip(RoundedCornerShape(18.dp)),
        cameraPositionState = camara,
        uiSettings = MapUiSettings(mapToolbarEnabled = false)
    ) {
        presas.forEach { presa ->
            Marker(
                state = MarkerState(LatLng(presa.latitud, presa.longitud)),
                title = presa.nombre,
                snippet = "${presa.volumenHm3 ?: "s/d"} hm³ · ${presa.uso}"
            )
        }
    }
}

@Composable
private fun Texto(texto: String, negrita: Boolean = false) {
    Text(
        texto,
        fontFamily = FuenteTexto,
        fontSize = if (negrita) 15.sp else 13.sp,
        fontWeight = if (negrita) FontWeight.Bold else FontWeight.Medium,
        color = if (negrita) Tinta else TintaSuave
    )
}
