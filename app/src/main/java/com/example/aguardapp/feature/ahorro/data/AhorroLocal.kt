package com.example.aguardapp.feature.ahorro.data

import android.content.SharedPreferences
import kotlinx.datetime.LocalDate

// Cada fecha guarda los identificadores de los retos marcados ese día.
class AhorroLocal(private val preferencias: SharedPreferences) {
    fun historial(): Map<LocalDate, Set<String>> =
        preferencias.all.keys.associate { clave ->
            LocalDate.parse(clave) to preferencias.getStringSet(clave, emptySet()).orEmpty().toSet()
        }

    fun alternarReto(fecha: LocalDate, retoId: String) {
        val clave = fecha.toString()
        val cumplidos = preferencias.getStringSet(clave, emptySet()).orEmpty()
        val nuevos = if (retoId in cumplidos) cumplidos - retoId else cumplidos + retoId
        val editor = preferencias.edit()
        if (nuevos.isEmpty()) editor.remove(clave) else editor.putStringSet(clave, nuevos)
        editor.apply()
    }
}
