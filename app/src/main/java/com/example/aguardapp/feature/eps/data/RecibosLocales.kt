package com.example.aguardapp.feature.eps.data

import android.content.SharedPreferences
import com.example.aguardapp.feature.eps.domain.MesRecibo
import com.example.aguardapp.feature.eps.domain.ReciboGuardado
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Guarda el último recibo. Reutiliza las claves de las instalaciones anteriores. */
class RecibosLocales(private val preferencias: SharedPreferences) {
    private val _recibo = MutableStateFlow(leerRecibo())
    val recibo = _recibo.asStateFlow()

    fun guardarRecibo(recibo: ReciboGuardado) {
        preferencias.edit()
            .putString(CLAVE_RECIBO_MES, recibo.mes.toString())
            .putString(CLAVE_RECIBO_M3, recibo.metrosCubicos.toString())
            .putBoolean(CLAVE_RECIBO_LEIDO, recibo.leidoDeLaFoto)
            .putBoolean("recibo_eps", recibo.leidoDeEps)
            .putString("recibo_total", recibo.totalPagar?.toString())
            .putString("recibo_actualizado", recibo.actualizadoEn)
            .apply()
        _recibo.value = recibo
    }

    private fun leerRecibo(): ReciboGuardado? {
        val mes = preferencias.getString(CLAVE_RECIBO_MES, null)?.let(MesRecibo::desdeTexto) ?: return null
        val metros = preferencias.getString(CLAVE_RECIBO_M3, null)?.toDoubleOrNull() ?: return null
        return ReciboGuardado(mes, metros, preferencias.getBoolean(CLAVE_RECIBO_LEIDO, false),
            leidoDeEps = preferencias.getBoolean("recibo_eps", false),
            totalPagar = preferencias.getString("recibo_total", null)?.toDoubleOrNull(),
            actualizadoEn = preferencias.getString("recibo_actualizado", null))
    }

    private companion object {
        const val CLAVE_RECIBO_MES = "recibo_mes"
        const val CLAVE_RECIBO_M3 = "recibo_m3"
        const val CLAVE_RECIBO_LEIDO = "recibo_leido"
    }
}
