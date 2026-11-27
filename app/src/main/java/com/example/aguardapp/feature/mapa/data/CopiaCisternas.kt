package com.example.aguardapp.feature.mapa.data

import android.content.SharedPreferences

interface CopiaCisternas {
    fun leer(): String?

    fun guardar(json: String)
}

class CopiaCisternasLocal(private val preferencias: SharedPreferences) : CopiaCisternas {
    override fun leer(): String? = preferencias.getString(CLAVE, null)

    override fun guardar(json: String) {
        preferencias.edit().putString(CLAVE, json).apply()
    }

    private companion object {
        const val CLAVE = "puntos_cisterna"
    }
}
