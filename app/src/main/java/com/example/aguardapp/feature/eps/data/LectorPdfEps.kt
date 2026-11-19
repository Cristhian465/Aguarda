package com.example.aguardapp.feature.eps.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.example.aguardapp.feature.eps.domain.ErrorEps
import com.example.aguardapp.feature.eps.domain.extraerReciboEps
import com.example.aguardapp.feature.eps.domain.LineaReciboEps
import com.example.aguardapp.feature.eps.domain.alinearCamposEps
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/** PDF temporal privado: se elimina tanto si termina como si falla o se cancela. */
internal class LectorPdfEps(private val context: Context) {
    suspend fun leer(bytes: ByteArray): String {
        // El recibo digital conserva texto: extraerlo evita confundir cifras pequeñas con OCR.
        PDFBoxResourceLoader.init(context.applicationContext)
        val digital = try {
            PDDocument.load(bytes).use { documento ->
                if (documento.numberOfPages !in 1..4) throw ErrorEps("Esperábamos un recibo de entre una y cuatro páginas.")
                PDFTextStripper().apply { sortByPosition = true }.getText(documento)
            }
        } catch (error: ErrorEps) { throw error }
        catch (error: Exception) { null }
        if (!digital.isNullOrBlank() && runCatching { extraerReciboEps(digital) }.isSuccess) return digital
        val archivo = File.createTempFile("eps-recibo-", ".pdf", context.cacheDir)
        try {
            archivo.writeBytes(bytes)
            return PdfRenderer(ParcelFileDescriptor.open(archivo, ParcelFileDescriptor.MODE_READ_ONLY)).use { pdf ->
                if (pdf.pageCount !in 1..4) throw ErrorEps("Esperábamos un recibo de entre una y cuatro páginas.")
                buildString {
                    repeat(pdf.pageCount) { indice ->
                        pdf.openPage(indice).use { pagina ->
                            if (pagina.width <= 0 || pagina.height <= 0) throw ErrorEps("El PDF de EPS no es válido.")
                            val escala = minOf(3.0, 3200.0 / maxOf(pagina.width, pagina.height))
                            val bitmap = Bitmap.createBitmap(
                                (pagina.width * escala).toInt().coerceAtLeast(1),
                                (pagina.height * escala).toInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
                            try {
                                bitmap.eraseColor(Color.WHITE)
                                pagina.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                appendLine(reconocer(bitmap))
                            } finally { bitmap.recycle() }
                        }
                    }
                }
            }
        } finally { archivo.delete() }
    }

    private suspend fun reconocer(bitmap: Bitmap): String {
        val resultado = detectar(bitmap)
        val lineas = resultado.textBlocks.flatMap { it.lines }.mapNotNull { linea ->
            linea.boundingBox?.let { b -> LineaReciboEps(linea.text, b.left, b.top, b.right, b.bottom) }
        }
        val alineado = alinearCamposEps(lineas, bitmap.width)
        val volumenRecortado = if ("Consumo del mes:" !in alineado) {
            val etiquetas = lineas.filter { Regex("^volumen\\s+fac(?:turado)?\\s+m[3³?]?$", RegexOption.IGNORE_CASE).matches(it.texto.trim()) }
            if (etiquetas.size == 1) {
                val e = etiquetas.single()
                val altura = (e.abajo - e.arriba).coerceAtLeast(1)
                val izquierda = (e.derecha + maxOf(8, altura / 2)).coerceAtMost(bitmap.width - 1)
                val derecha = (izquierda + bitmap.width / 5).coerceAtMost(bitmap.width)
                val arriba = (e.arriba - altura / 2).coerceAtLeast(0)
                val abajo = (e.abajo + altura / 2).coerceAtMost(bitmap.height)
                val recorte = Bitmap.createBitmap(bitmap, izquierda, arriba, derecha - izquierda, abajo - arriba)
                val ampliado = Bitmap.createScaledBitmap(recorte, recorte.width * 3, recorte.height * 3, true)
                try {
                    val numeros = detectar(ampliado).text.lines().map(String::trim)
                        .filter { it.matches(Regex("\\d{1,3}(?:[.,]\\d{1,2})?")) }
                        .mapNotNull { it.replace(',', '.').toDoubleOrNull()?.takeIf { n -> n in 0.0..500.0 } }.distinct()
                    numeros.singleOrNull()?.let { "Consumo del mes: $it m3" }.orEmpty()
                } finally { ampliado.recycle(); recorte.recycle() }
            } else ""
        } else ""
        return resultado.text + "\n" + alineado + "\n" + volumenRecortado
    }

    private suspend fun detectar(bitmap: Bitmap): com.google.mlkit.vision.text.Text = suspendCancellableCoroutine { continuacion ->
        val lector = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val copia = bitmap.copy(Bitmap.Config.ARGB_8888, false)
        try {
            lector.process(InputImage.fromBitmap(copia, 0))
                .addOnSuccessListener { if (continuacion.isActive) continuacion.resume(it) }
                .addOnFailureListener { if (continuacion.isActive) continuacion.resumeWithException(it) }
                .addOnCompleteListener { lector.close(); copia.recycle() }
        } catch (error: Exception) {
            lector.close()
            copia.recycle()
            if (continuacion.isActive) continuacion.resumeWithException(error)
        }
    }
}
