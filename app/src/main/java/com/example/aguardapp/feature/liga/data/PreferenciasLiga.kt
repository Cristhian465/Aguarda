package com.example.aguardapp.feature.liga.data

import android.content.SharedPreferences
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.aguardapp.feature.liga.domain.MesRecibo
import com.example.aguardapp.feature.liga.domain.ReciboGuardado

/** Decisión del usuario, código anónimo y último recibo. Se guardan solo en el teléfono. */
class PreferenciasLiga(private val preferencias: SharedPreferences) {
    private val _recibo = MutableStateFlow(leerRecibo())
    val recibo: StateFlow<ReciboGuardado?> = _recibo.asStateFlow()

    val participa: Boolean get() = preferencias.getBoolean(CLAVE_PARTICIPA, false)

    val ubigeo: String? get() = preferencias.getString(CLAVE_UBIGEO, null)

    /** Se crea una sola vez y no identifica a la persona. */
    fun token(): String = preferencias.getString(CLAVE_TOKEN, null)
        ?: UUID.randomUUID().toString().also { preferencias.edit().putString(CLAVE_TOKEN, it).apply() }

    fun aceptar(ubigeo: String) {
        preferencias.edit().putBoolean(CLAVE_PARTICIPA, true).putString(CLAVE_UBIGEO, ubigeo).apply()
    }

    /** Al salir se olvida el código: si vuelve a participar, será un hogar nuevo. */
    fun salir() {
        preferencias.edit().remove(CLAVE_PARTICIPA).remove(CLAVE_UBIGEO).remove(CLAVE_TOKEN).apply()
    }

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
        const val CLAVE_PARTICIPA = "participa"
        const val CLAVE_UBIGEO = "ubigeo"
        const val CLAVE_TOKEN = "token"
        const val CLAVE_RECIBO_MES = "recibo_mes"
        const val CLAVE_RECIBO_M3 = "recibo_m3"
        const val CLAVE_RECIBO_LEIDO = "recibo_leido"
    }
}
