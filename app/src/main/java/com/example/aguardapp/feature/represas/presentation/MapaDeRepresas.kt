package com.example.aguardapp.feature.represas.presentation

import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import kotlin.math.roundToInt
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.example.aguardapp.core.ui.theme.Blanco
import com.example.aguardapp.core.ui.theme.Tinta
import com.example.aguardapp.feature.mapa.domain.model.Coordenada
import com.example.aguardapp.feature.represas.domain.model.Presa
import com.example.aguardapp.feature.represas.domain.model.RecorridoDelAgua

// La sierra de Tacna, donde están las represas.
private val CENTRO_INICIAL = LatLng(-17.6, -70.1)
private const val ZOOM_INICIAL = 8.3f
private const val ZOOM_DE_UNA_PRESA = 10f
private const val MARGEN_DEL_ENCUADRE_DP = 36

/**
 * Mapa quieto (sin arrastrar, para no pelear con el desplazamiento de la pantalla; se acerca con los botones +/-).
 * Cada represa lleva un marcador del color de su uso; las del recorrido del hogar, un anillo oscuro.
 */
@Composable
fun MapaDeRepresas(presas: List<Presa>, recorrido: RecorridoDelAgua?, onAbrirPresa: (String) -> Unit, modifier: Modifier = Modifier) {
    val densidad = LocalDensity.current.density
    val camara = rememberCameraPositionState { position = CameraPosition.fromLatLngZoom(CENTRO_INICIAL, ZOOM_INICIAL) }
    var cargado by remember { mutableStateOf(false) }
    // Al cargar el mapa o cambiar el filtro, se encuadran las represas visibles.
    LaunchedEffect(cargado, presas.map { it.nombre }) {
        if (cargado) encuadrar(camara, presas, (MARGEN_DEL_ENCUADRE_DP * densidad).roundToInt())
    }
    Box(modifier.fillMaxWidth().height(240.dp).clip(RoundedCornerShape(18.dp))) {
        GoogleMap(
            modifier = Modifier.matchParentSize(),
            cameraPositionState = camara,
            uiSettings = MapUiSettings(
                scrollGesturesEnabled = false,
                zoomGesturesEnabled = false,
                rotationGesturesEnabled = false,
                tiltGesturesEnabled = false,
                zoomControlsEnabled = true,
                mapToolbarEnabled = false,
                myLocationButtonEnabled = false
            ),
            onMapLoaded = { cargado = true }
        ) {
            presas.forEach { presa ->
                key(presa.nombre) {
                    val resaltada = recorrido?.abasteceDesde(presa.nombre) == true
                    val color = colorDelUso(presa.usoPrincipal)
                    val icono = remember(color, resaltada) { iconoDePresa(color, resaltada, densidad) }
                    Marker(
                        state = remember { MarkerState(presa.ubicacion.aLatLng()) },
                        title = presa.nombre,
                        icon = icono,
                        anchor = Offset(0.5f, 0.5f),
                        zIndex = if (resaltada) 1f else 0f,
                        onClick = { onAbrirPresa(presa.nombre); true }
                    )
                }
            }
        }
    }
}

private fun encuadrar(camara: CameraPositionState, presas: List<Presa>, margenPx: Int) {
    if (presas.isEmpty()) return
    val limites = LatLngBounds.builder().apply { presas.forEach { include(it.ubicacion.aLatLng()) } }.build()
    val actualizacion = if (presas.size == 1) {
        CameraUpdateFactory.newLatLngZoom(limites.center, ZOOM_DE_UNA_PRESA)
    } else {
        CameraUpdateFactory.newLatLngBounds(limites, margenPx)
    }
    camara.move(actualizacion)
}

// Círculo del color del uso con borde blanco. Solo se puede crear con el mapa ya iniciado.
private fun iconoDePresa(color: Color, resaltada: Boolean, densidad: Float): BitmapDescriptor {
    val radio = (if (resaltada) 9f else 7f) * densidad
    val borde = 2.5f * densidad
    val anillo = if (resaltada) 3f * densidad else 0f
    val lado = ((radio + borde + anillo) * 2).roundToInt()
    val imagen = createBitmap(lado, lado)
    val lienzo = Canvas(imagen)
    val centro = lado / 2f
    val pincel = Paint(Paint.ANTI_ALIAS_FLAG)
    if (resaltada) {
        pincel.color = Tinta.toArgb()
        lienzo.drawCircle(centro, centro, radio + borde + anillo, pincel)
    }
    pincel.color = Blanco.toArgb()
    lienzo.drawCircle(centro, centro, radio + borde, pincel)
    pincel.color = color.toArgb()
    lienzo.drawCircle(centro, centro, radio, pincel)
    return BitmapDescriptorFactory.fromBitmap(imagen)
}

private fun Coordenada.aLatLng() = LatLng(latitud, longitud)
