package com.example.aguardapp.feature.sensor.data.remote

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import com.example.aguardapp.feature.sensor.domain.model.LecturaSensor

private const val CANAL = "https://api.thingspeak.com/channels/3575152/"
private const val CLAVE_LECTURA = "37NCWP3JEDUCB1MM"
private const val URL_ULTIMA_LECTURA = "${CANAL}feeds/last.json?api_key=$CLAVE_LECTURA"
private const val URL_HISTORIAL = "${CANAL}feeds.json?api_key=$CLAVE_LECTURA"
private const val TIEMPO_ESPERA_MS = 10_000

class ThingSpeakApi {
    suspend fun ultimaLectura(): LecturaSensor? {
        val texto = leer(URL_ULTIMA_LECTURA)
        return runCatching { JSONObject(texto) }.getOrNull()?.aLectura()
    }

    suspend fun historial(desde: String, hasta: String): List<LecturaSensor> {
        val url = "$URL_HISTORIAL&start=${URLEncoder.encode(desde, "UTF-8")}&end=${URLEncoder.encode(hasta, "UTF-8")}"
        val entradas = JSONObject(leer(url)).optJSONArray("feeds") ?: return emptyList()
        return (0 until entradas.length()).mapNotNull { entradas.optJSONObject(it)?.aLectura() }
    }

    private suspend fun leer(direccion: String): String = withContext(Dispatchers.IO) {
        val conexion = URL(direccion).openConnection() as HttpURLConnection
        conexion.connectTimeout = TIEMPO_ESPERA_MS
        conexion.readTimeout = TIEMPO_ESPERA_MS
        try {
            check(conexion.responseCode == HttpURLConnection.HTTP_OK) {
                "El servidor del sensor respondió con error ${conexion.responseCode}"
            }
            conexion.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conexion.disconnect()
        }
    }
}

@OptIn(ExperimentalTime::class)
private fun JSONObject.aLectura(): LecturaSensor? {
    val porcentaje = optString("field1").toDoubleOrNull()?.coerceIn(0.0, 100.0) ?: return null
    val momento = runCatching { Instant.parse(optString("created_at")) }.getOrNull() ?: return null
    return LecturaSensor(porcentaje, optString("field2").toDoubleOrNull(), momento)
}
