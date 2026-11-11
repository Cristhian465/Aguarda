package com.example.aguardapp.core.navigation

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aguardapp.R
import com.example.aguardapp.core.ui.theme.AguaMedia
import com.example.aguardapp.core.ui.theme.Blanco
import com.example.aguardapp.core.ui.theme.FuenteTexto
import com.example.aguardapp.core.ui.theme.Tenue
import com.example.aguardapp.core.ui.theme.TintaTenue

/** Pestañas de la barra inferior. */
enum class Pestana(val ruta: String, val etiqueta: String, @param:DrawableRes val icono: Int) {
    DEPOSITO(Rutas.MI_DEPOSITO, "Mi depósito", R.drawable.ic_gota),
    MAPA(Rutas.MAPA, "Mapa", R.drawable.ic_mapa)
}

@Composable
fun BarraInferior(actual: Pestana, onSeleccionar: (Pestana) -> Unit) {
    NavigationBar(containerColor = Blanco) {
        Pestana.entries.forEach { pestana ->
            NavigationBarItem(
                selected = pestana == actual,
                onClick = { if (pestana != actual) onSeleccionar(pestana) },
                icon = { Icon(painterResource(pestana.icono), contentDescription = null, modifier = Modifier.size(22.dp)) },
                label = { Text(pestana.etiqueta, fontFamily = FuenteTexto, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = AguaMedia,
                    selectedTextColor = AguaMedia,
                    indicatorColor = Tenue,
                    unselectedIconColor = TintaTenue,
                    unselectedTextColor = TintaTenue
                )
            )
        }
    }
}
