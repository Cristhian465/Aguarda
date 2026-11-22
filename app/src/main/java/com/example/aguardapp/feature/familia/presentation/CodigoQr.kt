package com.example.aguardapp.feature.familia.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

/** Dibuja el QR directamente con Compose, sin crear imágenes. */
@Composable
fun CodigoQr(texto: String, modifier: Modifier = Modifier) {
    val matriz = remember(texto) {
        runCatching { QRCodeWriter().encode(texto, BarcodeFormat.QR_CODE, 0, 0, mapOf(EncodeHintType.MARGIN to 1)) }.getOrNull()
    } ?: return
    Canvas(modifier.background(Color.White).semantics { contentDescription = "Código QR de invitación" }) {
        val celda = minOf(size.width / matriz.width, size.height / matriz.height)
        for (y in 0 until matriz.height) {
            for (x in 0 until matriz.width) {
                if (matriz.get(x, y)) drawRect(Color.Black, Offset(x * celda, y * celda), Size(celda, celda))
            }
        }
    }
}
