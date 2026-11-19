package com.example.aguardapp.feature.sensor.data

import android.content.SharedPreferences

/** Recuerda la madrugada cuyo aviso de fuga descartó el usuario ("fui yo"), para no repetirlo. */
class PreferenciasFuga(private val preferencias: SharedPreferences) {
    fun nocheDescartada(): String? = preferencias.getString(CLAVE, null)

    fun descartar(noche: String) {
        preferencias.edit().putString(CLAVE, noche).apply()
    }

    private companion object {
        const val CLAVE = "noche_descartada"
    }
}
