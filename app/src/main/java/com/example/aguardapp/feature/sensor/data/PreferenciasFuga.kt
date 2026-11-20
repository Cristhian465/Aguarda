package com.example.aguardapp.feature.sensor.data

import android.content.SharedPreferences

class PreferenciasFuga(private val preferencias: SharedPreferences) {
    fun nocheDescartada(): String? = preferencias.getString(CLAVE, null)

    fun descartar(noche: String) {
        preferencias.edit().putString(CLAVE, noche).apply()
    }

    private companion object {
        const val CLAVE = "noche_descartada"
    }
}
