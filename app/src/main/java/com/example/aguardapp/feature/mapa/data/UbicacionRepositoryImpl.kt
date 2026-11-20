package com.example.aguardapp.feature.mapa.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.os.CancellationSignal
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import com.example.aguardapp.feature.mapa.domain.model.Coordenada
import com.example.aguardapp.feature.mapa.domain.repository.UbicacionRepository

class UbicacionRepositoryImpl(context: Context) : UbicacionRepository {
    private val contexto = context.applicationContext
    private val gestor = contexto.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    override suspend fun ubicacionActual(): Coordenada? {
        if (!tienePermiso() || !LocationManagerCompat.isLocationEnabled(gestor)) return null
        val reciente = ultimaConocidaReciente()
        if (reciente != null) return reciente.aCoordenada()
        val proveedor = proveedorDisponible() ?: return null
        return withTimeoutOrNull(ESPERA_MAXIMA_MS) { leerUnaVez(proveedor) }?.aCoordenada()
    }

    private fun tienePermiso(): Boolean = PERMISOS.any {
        ContextCompat.checkSelfPermission(contexto, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun proveedorDisponible(): String? =
        listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
            .firstOrNull { runCatching { gestor.isProviderEnabled(it) }.getOrDefault(false) }

    private fun ultimaConocidaReciente(): Location? = try {
        gestor.getProviders(true)
            .mapNotNull { gestor.getLastKnownLocation(it) }
            .filter { SystemClock.elapsedRealtimeNanos() - it.elapsedRealtimeNanos < ANTIGUEDAD_MAXIMA_NS }
            .maxByOrNull { it.elapsedRealtimeNanos }
    } catch (e: SecurityException) {
        null
    }

    private suspend fun leerUnaVez(proveedor: String): Location? = suspendCancellableCoroutine { continuacion ->
        val cancelar = CancellationSignal()
        continuacion.invokeOnCancellation { cancelar.cancel() }
        try {
            LocationManagerCompat.getCurrentLocation(gestor, proveedor, cancelar, ContextCompat.getMainExecutor(contexto)) {
                if (continuacion.isActive) continuacion.resume(it)
            }
        } catch (e: SecurityException) {
            if (continuacion.isActive) continuacion.resume(null)
        }
    }

    private fun Location.aCoordenada() = Coordenada(latitude, longitude)

    private companion object {
        val PERMISOS = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        const val ESPERA_MAXIMA_MS = 15_000L
        const val ANTIGUEDAD_MAXIMA_NS = 2 * 60 * 1_000_000_000L
    }
}
