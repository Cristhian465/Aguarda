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
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMapComposable
import com.google.maps.android.compose.Polygon
import com.example.aguardapp.core.ui.theme.AguaClara
import com.example.aguardapp.core.ui.theme.AguaMedia
import com.example.aguardapp.core.ui.theme.Blanco
import com.example.aguardapp.core.ui.theme.FuenteTexto
import com.example.aguardapp.core.ui.theme.Tinta
import com.example.aguardapp.core.ui.theme.TintaSuave
import com.example.aguardapp.feature.mapa.domain.model.Distrito

private val RellenoSinDatos = Color(0x339AAAAE)
private val BordeSinDatos = Color(0xCC9AAAAE)

/** Dibuja los distritos en gris: el color por puntaje llegará cuando existan datos reales de hogares. */
@Composable
@GoogleMapComposable
fun CapaDistritos(distritos: List<Distrito>, elegido: Distrito?) {
    distritos.forEach { distrito ->
        val estaElegido = distrito.ubigeo == elegido?.ubigeo
        distrito.poligonos.forEachIndexed { indice, poligono ->
            key(distrito.ubigeo, indice) {
                Polygon(
                    points = poligono.map { LatLng(it.latitud, it.longitud) },
                    fillColor = if (estaElegido) AguaClara.copy(alpha = 0.30f) else RellenoSinDatos,
                    strokeColor = if (estaElegido) AguaClara else BordeSinDatos,
                    strokeWidth = if (estaElegido) 6f else 3f
                )
            }
        }
    }
}

/** Tarjeta del distrito tocado. Solo muestra datos que existen. */
@Composable
fun TarjetaDistrito(distrito: Distrito, esMiDistrito: Boolean, modifier: Modifier = Modifier) {
    // pointerInput evita que un toque sobre la tarjeta llegue al mapa que está debajo.
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Blanco).pointerInput(Unit) {}.padding(16.dp)) {
        if (esMiDistrito) {
            Text("Tu distrito", fontFamily = FuenteTexto, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = AguaMedia)
        }
        Text(distrito.nombre, fontFamily = FuenteTexto, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Tinta)
        Text(
            "Liga del Agua: aún no hay hogares participando.",
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
