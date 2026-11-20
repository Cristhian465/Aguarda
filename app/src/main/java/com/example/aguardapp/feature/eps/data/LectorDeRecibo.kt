package com.example.aguardapp.feature.eps.data

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/** Lee el texto de la foto dentro del teléfono con ML Kit. La foto no se copia ni se envía. */
class LectorDeRecibo(context: Context) {
    private val contexto = context.applicationContext

    suspend fun leerTexto(foto: Uri): Result<String> = suspendCancellableCoroutine { continuacion ->
        val lector = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        continuacion.invokeOnCancellation { lector.close() }
        try {
            lector.process(InputImage.fromFilePath(contexto, foto))
                .addOnSuccessListener { if (continuacion.isActive) continuacion.resume(Result.success(it.text)) }
                .addOnFailureListener { if (continuacion.isActive) continuacion.resume(Result.failure(it)) }
                .addOnCompleteListener { lector.close() }
        } catch (e: java.io.IOException) {
            lector.close()
            continuacion.resume(Result.failure(e))
        }
    }
}
