package com.example.aguardapp.feature.eps.data

import android.content.SharedPreferences
import com.example.aguardapp.feature.eps.domain.MesRecibo
import com.example.aguardapp.feature.eps.domain.ReciboGuardado
import com.example.aguardapp.feature.eps.domain.actualizarHistorial
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class RecibosLocales(private val preferencias: SharedPreferences) {
    private val gson = Gson()
    private val _historial = MutableStateFlow(leerHistorial())
    val historial = _historial.asStateFlow()
    private val _recibo = MutableStateFlow(_historial.value.firstOrNull())
    val recibo = _recibo.asStateFlow()

    var metaMensual: Double?
        get() = preferencias.getString("eps_meta_mensual", null)?.toDoubleOrNull()
        set(valor) {
            preferencias.edit().putString("eps_meta_mensual", valor?.toString()).apply()
        }

    var ultimoUsuarioEps: String?
        get() = preferencias.getString(CLAVE_USUARIO_EPS, null)
        set(usuario) {
            preferencias.edit().putString(CLAVE_USUARIO_EPS, usuario).apply()
        }

    fun guardarRecibo(recibo: ReciboGuardado) = guardarRecibos(listOf(recibo))

    @Synchronized
    fun guardarRecibos(recibos: List<ReciboGuardado>) {
        val historial = recibos.fold(_historial.value, ::actualizarHistorial)
        preferencias.edit().putString(CLAVE_HISTORIAL, gson.toJson(historial)).apply()
        _historial.value = historial
        _recibo.value = historial.firstOrNull()
    }

    private fun leerHistorial(): List<ReciboGuardado> {
        val guardados = preferencias.getString(CLAVE_HISTORIAL, null)
        if (guardados != null) {
            val historial = runCatching {
                gson.fromJson(guardados, Array<ReciboGuardado>::class.java)?.toList()
            }.getOrNull()
            if (!historial.isNullOrEmpty()) return historial
        }
        return listOfNotNull(leerRecibo())
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
        const val CLAVE_HISTORIAL = "eps_historial_recibos"
        const val CLAVE_USUARIO_EPS = "eps_ultimo_usuario"
        const val CLAVE_RECIBO_MES = "recibo_mes"
        const val CLAVE_RECIBO_M3 = "recibo_m3"
        const val CLAVE_RECIBO_LEIDO = "recibo_leido"
    }
}
