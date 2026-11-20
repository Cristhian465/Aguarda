package com.example.aguardapp.feature.mapa.presentation

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.aguardapp.core.ui.theme.AguaMedia
import com.example.aguardapp.core.ui.theme.Blanco
import com.example.aguardapp.core.ui.theme.FuenteTexto

private val PERMISOS_UBICACION = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

@Composable
fun rememberPermisoDeUbicacion(): Boolean {
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

@Composable
fun BotonVolverALaLista(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Text(
        "✕  Volver a la lista",
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(AguaMedia)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        fontFamily = FuenteTexto,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = Blanco
    )
}

fun subtituloDe(cantidad: Int): String = when (cantidad) {
    0 -> "Sin puntos de reparto por ahora"
    1 -> "1 punto de reparto en Tacna"
    else -> "$cantidad puntos de reparto en Tacna"
}
