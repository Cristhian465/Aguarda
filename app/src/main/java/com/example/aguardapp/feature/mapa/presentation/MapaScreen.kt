package com.example.aguardapp.feature.mapa.presentation

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.example.aguardapp.core.ui.theme.IconosClarosEnBarraDeEstado
import com.example.aguardapp.feature.deposito.presentation.componentes.BarraSuperior
import com.example.aguardapp.feature.mapa.domain.model.Coordenada
import com.example.aguardapp.feature.mapa.domain.model.PuntoCisterna

private const val ZOOM_INICIAL = 12.5f
private const val ZOOM_PUNTO = 15f
private val PERMISOS_UBICACION = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

@Composable
fun MapaScreen(viewModel: MapaViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val permisoConcedido = rememberPermisoDeUbicacion()
    LaunchedEffect(permisoConcedido) { viewModel.onPermisoDeUbicacion(permisoConcedido) }
    IconosClarosEnBarraDeEstado(claros = false)
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        BarraSuperior("Puntos de cisterna", subtituloDe(uiState.puntos.size), onVolver = null)
        val centro = uiState.centro
        if (uiState.cargando || centro == null) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            val camara = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(centro.aLatLng(), ZOOM_INICIAL)
            }
            val marcadores = remember(uiState.puntos) {
                uiState.puntos.associate { it.id to MarkerState(it.ubicacion.aLatLng()) }
            }
            val alcance = rememberCoroutineScope()
            Box(Modifier.weight(1f).fillMaxWidth()) {
                MapaDeCisternas(camara, uiState.puntos, marcadores, permisoConcedido)
                LeyendaDeEstados(Modifier.align(Alignment.TopStart).padding(12.dp))
            }
            ListaCisternas(
                estado = uiState,
                onSeleccionar = { punto ->
                    alcance.launch {
                        camara.animate(CameraUpdateFactory.newLatLngZoom(punto.ubicacion.aLatLng(), ZOOM_PUNTO))
                        marcadores[punto.id]?.showInfoWindow()
                    }
                },
                modifier = Modifier.heightIn(max = 280.dp)
            )
        }
    }
}

@Composable
private fun MapaDeCisternas(
    camara: CameraPositionState,
    puntos: List<PuntoCisterna>,
    marcadores: Map<String, MarkerState>,
    permisoConcedido: Boolean
) {
    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = camara,
        properties = MapProperties(isMyLocationEnabled = permisoConcedido),
        uiSettings = MapUiSettings(myLocationButtonEnabled = permisoConcedido, mapToolbarEnabled = false)
    ) {
        puntos.forEach { punto ->
            Marker(
                state = marcadores.getValue(punto.id),
                title = punto.nombre,
                snippet = detalleDe(punto),
                icon = BitmapDescriptorFactory.defaultMarker(tonoDelEstado(punto.estado))
            )
        }
    }
}

@Composable
private fun rememberPermisoDeUbicacion(): Boolean {
    val context = LocalContext.current
    var concedido by remember {
        mutableStateOf(PERMISOS_UBICACION.any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED })
    }
    val solicitar = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { resultado ->
        concedido = resultado.values.any { it }
    }
    LaunchedEffect(Unit) { if (!concedido) solicitar.launch(PERMISOS_UBICACION) }
    return concedido
}

private fun subtituloDe(cantidad: Int): String = when (cantidad) {
    0 -> "Sin puntos de reparto por ahora"
    1 -> "1 punto de reparto en Tacna"
    else -> "$cantidad puntos de reparto en Tacna"
}

private fun Coordenada.aLatLng() = LatLng(latitud, longitud)
