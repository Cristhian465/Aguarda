package com.example.aguardapp.feature.mapa.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMapComposable
import com.google.maps.android.compose.Polygon
import com.example.aguardapp.core.ui.theme.AguaMedia
import com.example.aguardapp.core.ui.theme.Blanco
import com.example.aguardapp.core.ui.theme.FuenteTexto
import com.example.aguardapp.core.ui.theme.Tinta
import com.example.aguardapp.core.ui.theme.TintaSuave
import com.example.aguardapp.feature.liga.domain.FilaRanking
import com.example.aguardapp.feature.liga.domain.HOGARES_MINIMOS
import com.example.aguardapp.feature.mapa.domain.model.Distrito

private const val OPACIDAD_SIN_PUNTAJE = 0.25f
private const val OPACIDAD_ELEGIDO = 0.55f

internal fun opacidadDelPuntaje(puntaje: Double): Float = (0.35 + puntaje.coerceIn(0.0, 100.0) / 100 * 0.4).toFloat()

internal fun opacidadDelDistrito(puntaje: Double?, elegido: Boolean): Float = when {
    puntaje != null -> opacidadDelPuntaje(puntaje)
    elegido -> OPACIDAD_ELEGIDO
    else -> OPACIDAD_SIN_PUNTAJE
}

@Composable
@GoogleMapComposable
fun CapaDistritos(distritos: List<Distrito>, elegido: Distrito?, liga: List<FilaRanking>) {
    distritos.forEach { distrito ->
        val estaElegido = distrito.ubigeo == elegido?.ubigeo
        val puntaje = liga.firstOrNull { it.ubigeo == distrito.ubigeo }?.puntaje
        val color = colorDelDistrito(distrito.ubigeo)
        distrito.poligonos.forEachIndexed { indice, poligono ->
            key(distrito.ubigeo, indice) {
                Polygon(
                    points = poligono.map { LatLng(it.latitud, it.longitud) },
                    fillColor = color.copy(alpha = opacidadDelDistrito(puntaje, estaElegido)),
                    strokeColor = if (estaElegido) Blanco else color.copy(alpha = 0.9f),
                    strokeWidth = if (estaElegido) 7f else 4f
                )
            }
        }
    }
}

@Composable
fun LeyendaDeDistritos(distritos: List<Distrito>, modifier: Modifier = Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(12.dp)).background(Blanco.copy(alpha = 0.92f)).padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        distritos.forEach { distrito ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                PuntoDeColor(colorDelDistrito(distrito.ubigeo))
                Text(nombreCorto(distrito.nombre), fontFamily = FuenteTexto, fontSize = 12.sp, color = TintaSuave)
            }
        }
    }
}

@Composable
private fun PuntoDeColor(color: Color) {
    Box(Modifier.size(10.dp).clip(CircleShape).background(color))
}

@Composable
fun TarjetaDistrito(
    distrito: Distrito,
    esMiDistrito: Boolean,
    fila: FilaRanking?,
    aviso: String? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Blanco).pointerInput(Unit) {}.padding(16.dp)) {
        if (esMiDistrito) {
            Text("Tu distrito", fontFamily = FuenteTexto, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = AguaMedia)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PuntoDeColor(colorDelDistrito(distrito.ubigeo))
            Text(distrito.nombre, fontFamily = FuenteTexto, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Tinta)
        }
        Text(
            textoDeLiga(fila),
            modifier = Modifier.padding(top = 2.dp),
            fontFamily = FuenteTexto,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = TintaSuave
        )
        aviso?.let {
            Text(
                it,
                modifier = Modifier.padding(top = 4.dp),
                fontFamily = FuenteTexto,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = AguaMedia
            )
        }
        Text(
            "Límites: © colaboradores de OpenStreetMap",
            modifier = Modifier.padding(top = 6.dp),
            fontFamily = FuenteTexto,
            fontSize = 11.sp,
            color = TintaSuave
        )
    }
}

internal fun textoDeLiga(fila: FilaRanking?): String {
    val puntaje = fila?.puntaje
    return when {
        fila == null || fila.hogares == 0 -> "Liga del Agua: aún no hay hogares participando."
        puntaje == null -> "Liga del Agua: ${fila.hogares} de $HOGARES_MINIMOS hogares necesarios para competir."
        else -> "Puesto ${fila.puesto} · ${puntaje.roundToInt()} puntos · ${fila.hogares} hogares"
    }
}
