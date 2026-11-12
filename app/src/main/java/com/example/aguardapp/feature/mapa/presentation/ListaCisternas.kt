package com.example.aguardapp.feature.mapa.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.example.aguardapp.core.ui.theme.AguaMedia
import com.example.aguardapp.core.ui.theme.Blanco
import com.example.aguardapp.core.ui.theme.Divisor
import com.example.aguardapp.core.ui.theme.FuenteTexto
import com.example.aguardapp.core.ui.theme.Tinta
import com.example.aguardapp.core.ui.theme.TintaSuave
import com.example.aguardapp.feature.mapa.domain.model.EstadoCisterna
import com.example.aguardapp.feature.mapa.domain.model.PuntoCisterna
import com.example.aguardapp.feature.mapa.domain.usecase.BuscarCisternasCercanas
import com.example.aguardapp.feature.mapa.domain.usecase.CandidataCisterna

/** Tono del marcador de Google Maps según el estado del punto. */
fun tonoDelEstado(estado: EstadoCisterna): Float = when (estado) {
    EstadoCisterna.ACTIVO -> BitmapDescriptorFactory.HUE_AZURE
    EstadoCisterna.EN_RUTA -> BitmapDescriptorFactory.HUE_ORANGE
    EstadoCisterna.TERMINADO -> BitmapDescriptorFactory.HUE_RED
}

private fun colorDelEstado(estado: EstadoCisterna) = Color.hsv(tonoDelEstado(estado), 1f, 1f)

/** Qué significa cada color de marcador. Va encima del mapa. */
@Composable
fun LeyendaDeEstados(modifier: Modifier = Modifier) {
    Row(
        modifier.clip(RoundedCornerShape(12.dp)).background(Blanco.copy(alpha = 0.92f)).padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        EstadoCisterna.entries.forEach { estado ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Punto(colorDelEstado(estado), 10)
                Texto(nombreDelEstado(estado), 12, TintaSuave)
            }
        }
    }
}

/** Lista bajo el mapa: las cisternas cercanas con su distancia o, sin ubicación, todos los puntos. */
@Composable
fun ListaCisternas(estado: MapaUiState, onSeleccionar: (PuntoCisterna) -> Unit, modifier: Modifier = Modifier) {
    val encontrada = estado.ubicacion == EstadoUbicacion.ENCONTRADA
    val conDistancia = encontrada && estado.cercanas.isNotEmpty()
    // Si no hay ninguna dentro del radio, se muestran igual todos los puntos (sin distancia).
    val filas = estado.candidatas
    // El aviso queda fijo arriba; debajo se desplaza la lista, para que el mapa conserve su espacio.
    Column(modifier.fillMaxWidth().background(Blanco)) {
        estado.aviso?.let { TarjetaAntesDeQueSeAcabe(it, Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)) }
        ListaDesplazable(estado, filas, encontrada, conDistancia, onSeleccionar)
    }
}

@Composable
private fun ListaDesplazable(
    estado: MapaUiState,
    filas: List<CandidataCisterna>,
    encontrada: Boolean,
    conDistancia: Boolean,
    onSeleccionar: (PuntoCisterna) -> Unit
) {
    LazyColumn {
        item(key = "encabezado") {
            Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 6.dp)) {
                Texto(if (conDistancia) "Cisternas cercanas" else "Puntos de reparto", 15, Tinta, FontWeight.ExtraBold)
                avisoDeUbicacion(estado.ubicacion, encontrada && !conDistancia)?.let { Texto(it, 12.5, TintaSuave) }
            }
        }
        items(filas, key = { it.punto.id }) { candidata ->
            val sugerida = candidata.punto.id == estado.aviso?.sugeridaId
            FilaCisterna(candidata.punto, candidata.distanciaKm, sugerida) { onSeleccionar(candidata.punto) }
            HorizontalDivider(color = Divisor)
        }
    }
}

private fun avisoDeUbicacion(estado: EstadoUbicacion, sinCercanas: Boolean): String? = when {
    sinCercanas -> "No hay cisternas a menos de ${BuscarCisternasCercanas.RADIO_KM.toInt()} km de ti."
    estado == EstadoUbicacion.SIN_PERMISO -> "Activa tu ubicación para ver las más cercanas."
    estado == EstadoUbicacion.BUSCANDO -> "Buscando tu ubicación…"
    estado == EstadoUbicacion.NO_DISPONIBLE -> "No pudimos obtener tu ubicación. Revisa que esté encendida."
    else -> null
}

@Composable
private fun FilaCisterna(punto: PuntoCisterna, distanciaKm: Double?, sugerida: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Punto(colorDelEstado(punto.estado), 12)
        Column(Modifier.weight(1f)) {
            Texto(punto.nombre, 14, Tinta, FontWeight.Bold)
            Texto(detalleDe(punto), 12.5, TintaSuave)
            if (sugerida) Texto("Te conviene", 12, AguaMedia, FontWeight.ExtraBold)
        }
        distanciaKm?.let { Texto(formatearDistancia(it), 13, Tinta, FontWeight.SemiBold) }
    }
}

@Composable
private fun Punto(color: Color, tamano: Int) {
    Box(Modifier.size(tamano.dp).clip(CircleShape).background(color))
}

@Composable
private fun Texto(texto: String, tamano: Number, color: Color, peso: FontWeight = FontWeight.Medium) {
    Text(texto, fontFamily = FuenteTexto, fontSize = tamano.toFloat().sp, fontWeight = peso, color = color)
}
