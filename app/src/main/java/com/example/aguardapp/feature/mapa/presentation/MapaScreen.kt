package com.example.aguardapp.feature.mapa.presentation

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.example.aguardapp.core.ui.theme.IconosClarosEnBarraDeEstado
import com.example.aguardapp.feature.deposito.presentation.componentes.BarraSuperior
import com.example.aguardapp.feature.mapa.domain.model.Coordenada
import com.example.aguardapp.feature.mapa.domain.model.EstadoCisterna
import com.example.aguardapp.feature.mapa.domain.model.PuntoCisterna

private const val ZOOM_INICIAL = 12.5f
private val PERMISOS_UBICACION = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

@Composable
fun MapaScreen(viewModel: MapaViewModel = viewModel { MapaViewModel.desdeInyeccion() }) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    IconosClarosEnBarraDeEstado(claros = false)
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        BarraSuperior("Puntos de cisterna", subtituloDe(uiState.puntos.size), onVolver = null)
        val centro = uiState.centro
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            if (uiState.cargando || centro == null) {
                CircularProgressIndicator()
            } else {
                MapaDeCisternas(centro, uiState.puntos)
            }
        }
    }
}

@Composable
private fun MapaDeCisternas(centro: Coordenada, puntos: List<PuntoCisterna>) {
    val permisoConcedido = rememberPermisoDeUbicacion()
    val camara = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(centro.aLatLng(), ZOOM_INICIAL)
    }
    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = camara,
        properties = MapProperties(isMyLocationEnabled = permisoConcedido),
        uiSettings = MapUiSettings(myLocationButtonEnabled = permisoConcedido, mapToolbarEnabled = false)
    ) {
        puntos.forEach { punto ->
            Marker(
                state = remember(punto.id) { MarkerState(punto.ubicacion.aLatLng()) },
                title = punto.nombre,
                snippet = detalleDe(punto),
                icon = BitmapDescriptorFactory.defaultMarker(colorDelEstado(punto.estado))
            )
        }
    }
}

/** Pide el permiso de ubicación al abrir el mapa; si se niega, el mapa funciona sin "mi ubicación". */
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

private fun colorDelEstado(estado: EstadoCisterna): Float = when (estado) {
    EstadoCisterna.ACTIVO -> BitmapDescriptorFactory.HUE_AZURE
    EstadoCisterna.EN_RUTA -> BitmapDescriptorFactory.HUE_ORANGE
    EstadoCisterna.TERMINADO -> BitmapDescriptorFactory.HUE_RED
}

private fun subtituloDe(cantidad: Int): String = when (cantidad) {
    0 -> "Sin puntos de reparto por ahora"
    1 -> "1 punto de reparto en Tacna"
    else -> "$cantidad puntos de reparto en Tacna"
}

private fun Coordenada.aLatLng() = LatLng(latitud, longitud)
