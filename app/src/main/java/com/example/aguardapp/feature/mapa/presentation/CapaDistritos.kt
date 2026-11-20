package com.example.aguardapp.feature.mapa.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
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
import com.example.aguardapp.core.ui.theme.AguaClara
import com.example.aguardapp.core.ui.theme.AguaMedia
import com.example.aguardapp.core.ui.theme.Blanco
import com.example.aguardapp.core.ui.theme.FuenteTexto
import com.example.aguardapp.core.ui.theme.Tinta
import com.example.aguardapp.core.ui.theme.TintaSuave
import com.example.aguardapp.feature.liga.domain.FilaRanking
import com.example.aguardapp.feature.mapa.domain.model.Distrito

private val RellenoSinDatos = Color(0x339AAAAE)
private val BordeSinDatos = Color(0xCC9AAAAE)

internal fun opacidadDelPuntaje(puntaje: Double): Float = (0.15 + puntaje.coerceIn(0.0, 100.0) / 100 * 0.55).toFloat()

@Composable
@GoogleMapComposable
fun CapaDistritos(distritos: List<Distrito>, elegido: Distrito?, liga: List<FilaRanking>) {
    distritos.forEach { distrito ->
        val estaElegido = distrito.ubigeo == elegido?.ubigeo
        val puntaje = liga.firstOrNull { it.ubigeo == distrito.ubigeo }?.puntaje
        val relleno = puntaje?.let { AguaClara.copy(alpha = opacidadDelPuntaje(it)) } ?: RellenoSinDatos
        distrito.poligonos.forEachIndexed { indice, poligono ->
            key(distrito.ubigeo, indice) {
                Polygon(
                    points = poligono.map { LatLng(it.latitud, it.longitud) },
                    fillColor = relleno,
                    strokeColor = if (estaElegido) AguaClara else BordeSinDatos,
                    strokeWidth = if (estaElegido) 6f else 3f
                )
            }
        }
    }
}

@Composable
fun TarjetaDistrito(distrito: Distrito, esMiDistrito: Boolean, fila: FilaRanking?, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Blanco).pointerInput(Unit) {}.padding(16.dp)) {
        if (esMiDistrito) {
            Text("Tu distrito", fontFamily = FuenteTexto, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = AguaMedia)
        }
        Text(distrito.nombre, fontFamily = FuenteTexto, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Tinta)
        Text(
            textoDeLiga(fila),
            modifier = Modifier.padding(top = 2.dp),
            fontFamily = FuenteTexto,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = TintaSuave
        )
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
        puntaje == null -> "Liga del Agua: ${fila.hogares} de 5 hogares necesarios para competir."
        else -> "Puesto ${fila.puesto} · ${puntaje.roundToInt()} puntos · ${fila.hogares} hogares"
    }
}
