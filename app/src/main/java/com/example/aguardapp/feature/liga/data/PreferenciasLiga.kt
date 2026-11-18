package com.example.aguardapp.feature.liga.data

import android.content.SharedPreferences
import java.util.UUID

/** Decisión del usuario y código anónimo del hogar. Se guardan solo en el teléfono. */
class PreferenciasLiga(private val preferencias: SharedPreferences) {

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
        preferencias.edit().clear().apply()
    }

    private companion object {
        const val CLAVE_PARTICIPA = "participa"
        const val CLAVE_UBIGEO = "ubigeo"
        const val CLAVE_TOKEN = "token"
    }
}
