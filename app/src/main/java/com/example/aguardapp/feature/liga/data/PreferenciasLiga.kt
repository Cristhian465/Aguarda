package com.example.aguardapp.feature.liga.data

import android.content.SharedPreferences
import java.util.UUID

/**
 * Participación en la Liga. En modo familia, [tokenCompartido] da el mismo código anónimo a todos los
 * teléfonos del hogar, para que la casa cuente una sola vez en el ranking y en los reportes de vecinos.
 */
class PreferenciasLiga(
    private val preferencias: SharedPreferences,
    private val tokenCompartido: () -> String? = { null }
) {
    val participa: Boolean get() = preferencias.getBoolean(CLAVE_PARTICIPA, false)

    val ubigeo: String? get() = preferencias.getString(CLAVE_UBIGEO, null)

    fun token(): String = tokenCompartido() ?: preferencias.getString(CLAVE_TOKEN, null)
        ?: UUID.randomUUID().toString().also { preferencias.edit().putString(CLAVE_TOKEN, it).apply() }

    fun aceptar(ubigeo: String) {
        preferencias.edit().putBoolean(CLAVE_PARTICIPA, true).putString(CLAVE_UBIGEO, ubigeo).apply()
    }

    fun salir() {
        preferencias.edit().remove(CLAVE_PARTICIPA).remove(CLAVE_UBIGEO).remove(CLAVE_TOKEN).apply()
    }

    private companion object {
        const val CLAVE_PARTICIPA = "participa"
        const val CLAVE_UBIGEO = "ubigeo"
        const val CLAVE_TOKEN = "token"
    }
}
