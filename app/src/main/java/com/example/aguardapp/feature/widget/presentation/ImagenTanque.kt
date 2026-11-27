package com.example.aguardapp.feature.widget.presentation

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.roundToInt
import com.example.aguardapp.feature.deposito.presentation.componentes.dibujarIndicadorNivel

fun imagenTanque(context: Context, porcentaje: Int, ancho: Dp, alto: Dp): Bitmap {
    val densidad = context.resources.displayMetrics.density
    val anchoPx = (ancho.value * densidad).roundToInt().coerceAtLeast(1)
    val altoPx = (alto.value * densidad).roundToInt().coerceAtLeast(1)
    val imagen = ImageBitmap(anchoPx, altoPx)
    CanvasDrawScope().draw(Density(densidad), LayoutDirection.Ltr, Canvas(imagen), Size(anchoPx.toFloat(), altoPx.toFloat())) {
        dibujarIndicadorNivel(porcentaje / 100f, conOla = true)
    }
    return imagen.asAndroidBitmap()
}
