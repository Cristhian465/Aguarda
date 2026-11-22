package com.example.aguardapp.feature.familia.data

import android.content.SharedPreferences
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Estado del modo familia en este teléfono: el secreto del hogar, el id del teléfono y lo que se
 * sincronizó por última vez. El respaldo automático de Android está desactivado en la app.
 */
class PreferenciasFamilia(private val preferencias: SharedPreferences) {
    private val secretoGuardado = MutableStateFlow(preferencias.getString(SECRETO, null))

    val secretoActual: StateFlow<String?> = secretoGuardado.asStateFlow()

    val secreto: String? get() = secretoGuardado.value

    val apodo: String? get() = preferencias.getString(APODO, null)

    val reemplazoPendiente: Boolean get() = preferencias.getBoolean(REEMPLAZO, false)

    val perfilSincronizado: String? get() = preferencias.getString(PERFIL, null)

    val conocidos: Set<String> get() = preferencias.getStringSet(CONOCIDOS, null)?.toSet().orEmpty()

    val ultimaSincronizacion: Long? get() = preferencias.getLong(ULTIMA, 0L).takeIf { it > 0 }

    /** Id fijo de este teléfono dentro de la familia. */
    fun miembroId(): String = preferencias.getString(MIEMBRO, null)
        ?: UUID.randomUUID().toString().also { preferencias.edit().putString(MIEMBRO, it).commit() }

    fun entrar(secreto: String, apodo: String, reemplazarDatosLocales: Boolean) {
        preferencias.edit()
            .putString(SECRETO, secreto)
            .putString(APODO, apodo)
            .putBoolean(REEMPLAZO, reemplazarDatosLocales)
            .remove(PERFIL)
            .remove(CONOCIDOS)
            .remove(ULTIMA)
            .commit()
        secretoGuardado.value = secreto
    }

    fun cambiarSecreto(nuevo: String) {
        preferencias.edit().putString(SECRETO, nuevo).commit()
        secretoGuardado.value = nuevo
    }

    fun guardarSincronizacion(perfil: String?, conocidos: Set<String>, momentoMillis: Long) {
        preferencias.edit()
            .putString(PERFIL, perfil)
            .putStringSet(CONOCIDOS, conocidos)
            .putLong(ULTIMA, momentoMillis)
            .putBoolean(REEMPLAZO, false)
            .commit()
    }

    /** Sale de la familia. El apodo y el id del teléfono se conservan para la próxima vez. */
    fun salir() {
        preferencias.edit()
            .remove(SECRETO)
            .remove(REEMPLAZO)
            .remove(PERFIL)
            .remove(CONOCIDOS)
            .remove(ULTIMA)
            .commit()
        secretoGuardado.value = null
    }

    /** Código anónimo de la Liga compartido por la familia, o null si el teléfono no está en una. */
    fun tokenLiga(): String? = secreto?.let(::tokenLigaDe)

    private companion object {
        const val SECRETO = "secreto"
        const val APODO = "apodo"
        const val MIEMBRO = "miembro"
        const val REEMPLAZO = "reemplazo_pendiente"
        const val PERFIL = "perfil_sincronizado"
        const val CONOCIDOS = "eventos_conocidos"
        const val ULTIMA = "ultima_sincronizacion"
    }
}

/** Todos los teléfonos de la familia obtienen el mismo código de Liga, distinto del secreto del hogar. */
fun tokenLigaDe(secreto: String): String =
    UUID.nameUUIDFromBytes("liga:$secreto".toByteArray(Charsets.UTF_8)).toString()
