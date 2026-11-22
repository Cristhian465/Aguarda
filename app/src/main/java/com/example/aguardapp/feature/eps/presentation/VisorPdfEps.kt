package com.example.aguardapp.feature.eps.presentation

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class PaginaPdf(val bitmap: Bitmap, val total: Int)

@Composable
internal fun VisorPdfEps(bytes: ByteArray, cerrar: () -> Unit) {
    val cache = LocalContext.current.cacheDir
    var indice by rememberSaveable { mutableIntStateOf(0) }
    var zoom by rememberSaveable { mutableIntStateOf(1) }
    val resultado by key(bytes, indice) {
        produceState<Result<PaginaPdf>?>(null) {
            value = try {
                Result.success(withContext(Dispatchers.IO) { renderizarPagina(cache, bytes, indice) })
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Result.failure(error)
            }
        }
    }
    val pagina = resultado?.getOrNull()

    Dialog(onDismissRequest = cerrar, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.safeDrawingPadding().padding(horizontal = 12.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Recibo de EPS", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                    TextButton(onClick = cerrar) { Text("Cerrar") }
                }
                if (pagina != null) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = { zoom-- }, enabled = zoom > 1) { Text("Reducir") }
                        TextButton(onClick = { zoom++ }, enabled = zoom < 3) { Text("Ampliar") }
                        if (pagina.total > 1) {
                            TextButton(onClick = { indice-- }, enabled = indice > 0) { Text("Anterior") }
                            TextButton(onClick = { indice++ }, enabled = indice < pagina.total - 1) { Text("Siguiente") }
                        }
                    }
                    if (pagina.total > 1) Text("Página ${indice + 1} de ${pagina.total}")
                    BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().padding(vertical = 8.dp)) {
                        val ancho = maxWidth * zoom
                        key(indice) {
                            Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).horizontalScroll(rememberScrollState())) {
                                Image(
                                    bitmap = pagina.bitmap.asImageBitmap(),
                                    contentDescription = "Recibo de EPS, página ${indice + 1}",
                                    modifier = Modifier.width(ancho).aspectRatio(pagina.bitmap.width.toFloat() / pagina.bitmap.height)
                                )
                            }
                        }
                    }
                } else {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        if (resultado == null) CircularProgressIndicator()
                        else Text("No se pudo mostrar el PDF de EPS.")
                    }
                }
            }
        }
    }
}

private fun renderizarPagina(cache: File, bytes: ByteArray, indice: Int): PaginaPdf {
    val archivo = File.createTempFile("eps-visor-", ".pdf", cache)
    try {
        archivo.writeBytes(bytes)
        return ParcelFileDescriptor.open(archivo, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
            // PdfRenderer toma posesión del descriptor que recibe.
            val copia = descriptor.dup()
            val renderer = try {
                PdfRenderer(copia)
            } catch (error: Exception) {
                copia.close()
                throw error
            }
            renderer.use { pdf ->
                pdf.openPage(indice).use { pagina ->
                    val escala = 1800.0 / maxOf(pagina.width, pagina.height)
                    val bitmap = Bitmap.createBitmap(
                        (pagina.width * escala).toInt().coerceAtLeast(1),
                        (pagina.height * escala).toInt().coerceAtLeast(1),
                        Bitmap.Config.ARGB_8888
                    )
                    try {
                        bitmap.eraseColor(Color.WHITE)
                        pagina.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        PaginaPdf(bitmap, pdf.pageCount)
                    } catch (error: Exception) {
                        bitmap.recycle()
                        throw error
                    }
                }
            }
        }
    } finally {
        archivo.delete()
    }
}
