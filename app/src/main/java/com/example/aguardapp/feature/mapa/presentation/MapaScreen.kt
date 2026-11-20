package com.example.aguardapp.feature.mapa.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
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

private const val ZOOM_INICIAL = 12.5f
private const val ZOOM_PUNTO = 15f

@Composable
fun MapaScreen(onVolver: () -> Unit, viewModel: MapaViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val permisoConcedido = rememberPermisoDeUbicacion()
    LaunchedEffect(permisoConcedido) { viewModel.onPermisoDeUbicacion(permisoConcedido) }
    LifecycleResumeEffect(Unit) {
        viewModel.onRefrescarLiga()
        viewModel.onRefrescarReportes()
        onPauseOrDispose { }
    }
    IconosClarosEnBarraDeEstado(claros = false)
    var pantallaCompleta by rememberSaveable { mutableStateOf(false) }
    val salirDePantallaCompleta = {
        pantallaCompleta = false
        viewModel.onCerrarDistrito()
    }
    BackHandler(enabled = pantallaCompleta, onBack = salirDePantallaCompleta)
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (!pantallaCompleta) BarraSuperior("Puntos de cisterna", subtituloDe(uiState.puntos.size), onVolver = onVolver)
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
                MapaDeCisternas(camara, uiState, marcadores, permisoConcedido, pantallaCompleta) { punto ->
                    if (pantallaCompleta) viewModel.onTocarDistrito(punto) else pantallaCompleta = true
                }
                val bajoLaBarraDeEstado = if (pantallaCompleta) Modifier.statusBarsPadding() else Modifier
                Column(
                    Modifier.align(Alignment.TopStart).then(bajoLaBarraDeEstado).padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LeyendaDeEstados()
                    if (pantallaCompleta) LeyendaDeDistritos(uiState.distritos)
                }
                BotonReportar(
                    Modifier.align(Alignment.TopEnd).then(bajoLaBarraDeEstado).padding(top = 60.dp, end = 12.dp),
                    onClick = viewModel::onAbrirReporte
                )
                if (pantallaCompleta) {
                    Column(
                        Modifier.align(Alignment.BottomCenter).padding(start = 16.dp, end = 72.dp, bottom = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        uiState.distritoElegido?.let { distrito ->
                            TarjetaDistrito(
                                distrito,
                                distrito.ubigeo == uiState.miDistrito?.ubigeo,
                                uiState.liga.firstOrNull { it.ubigeo == distrito.ubigeo },
                                avisoDeDistrito(uiState.reportes, distrito.ubigeo)
                            )
                        }
                        BotonVolverALaLista(onClick = salirDePantallaCompleta)
                    }
                }
            }
            uiState.reporte?.let { panel ->
                DialogoReporte(
                    estado = uiState,
                    panel = panel,
                    onTipo = viewModel::onElegirTipoReporte,
                    onDistrito = viewModel::onElegirDistritoDelReporte,
                    onCisterna = viewModel::onElegirCisternaDelReporte,
                    onEnviar = viewModel::onEnviarReporte,
                    onCerrar = viewModel::onCerrarReporte
                )
            }
            if (!pantallaCompleta) ListaCisternas(
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
    estado: MapaUiState,
    marcadores: Map<String, MarkerState>,
    permisoConcedido: Boolean,
    pantallaCompleta: Boolean,
    onTocarMapa: (Coordenada) -> Unit
) {
    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = camara,
        contentPadding = if (pantallaCompleta) WindowInsets.statusBars.asPaddingValues() else PaddingValues(),
        onMapClick = { onTocarMapa(Coordenada(it.latitude, it.longitude)) },
        properties = MapProperties(isMyLocationEnabled = permisoConcedido),
        uiSettings = MapUiSettings(myLocationButtonEnabled = permisoConcedido, mapToolbarEnabled = false)
    ) {
        CapaDistritos(estado.distritos, estado.distritoElegido, estado.liga)
        estado.puntos.forEach { punto ->
            Marker(
                state = marcadores.getValue(punto.id),
                title = punto.nombre,
                snippet = detalleDe(punto),
                icon = BitmapDescriptorFactory.defaultMarker(tonoDelEstado(punto.estado))
            )
        }
    }
}

private fun Coordenada.aLatLng() = LatLng(latitud, longitud)
