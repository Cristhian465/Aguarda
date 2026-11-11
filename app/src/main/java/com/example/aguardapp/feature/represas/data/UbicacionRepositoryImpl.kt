package com.example.aguardapp.feature.represas.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import com.example.aguardapp.feature.mapa.domain.model.Coordenada
import com.example.aguardapp.feature.represas.domain.repository.UbicacionRepository

private const val ESPERA_MAXIMA_MS = 15_000L

/** Lee la ubicación con el LocationManager de Android, sin depender de Google Play Services. */
class UbicacionRepositoryImpl(private val context: Context) : UbicacionRepository {

    private val tienePermiso: Boolean
        get() = permitido(Manifest.permission.ACCESS_FINE_LOCATION) || permitido(Manifest.permission.ACCESS_COARSE_LOCATION)

    @SuppressLint("MissingPermission")  // cada proveedor se usa solo con su permiso concedido
    override suspend fun ubicacionActual(): Coordenada? {
        val gestor = context.getSystemService(LocationManager::class.java) ?: return null
        // La red basta para saber el distrito y responde más rápido; el GPS exige el permiso de ubicación precisa.
        val proveedor = buildList {
            if (tienePermiso) add(LocationManager.NETWORK_PROVIDER)
            if (permitido(Manifest.permission.ACCESS_FINE_LOCATION)) add(LocationManager.GPS_PROVIDER)
        }.firstOrNull { gestor.isProviderEnabled(it) } ?: return null
        val ubicacion = try {
            withTimeoutOrNull(ESPERA_MAXIMA_MS) { pedirUbicacion(gestor, proveedor) } ?: gestor.getLastKnownLocation(proveedor)
        } catch (e: SecurityException) {
            null  // el permiso se retiró mientras se buscaba la ubicación
        }
        return ubicacion?.let { runCatching { Coordenada(it.latitude, it.longitude) }.getOrNull() }
    }

    @SuppressLint("MissingPermission")
    private suspend fun pedirUbicacion(gestor: LocationManager, proveedor: String): Location? =
        suspendCancellableCoroutine { continuacion ->
            val cancelacion = CancellationSignal()
            continuacion.invokeOnCancellation { cancelacion.cancel() }
            LocationManagerCompat.getCurrentLocation(gestor, proveedor, cancelacion, ContextCompat.getMainExecutor(context)) { ubicacion ->
                continuacion.resume(ubicacion)
            }
        }

    private fun permitido(permiso: String): Boolean =
        ContextCompat.checkSelfPermission(context, permiso) == PackageManager.PERMISSION_GRANTED
}
