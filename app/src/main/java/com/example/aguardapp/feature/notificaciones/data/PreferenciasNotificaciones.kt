package com.example.aguardapp.feature.notificaciones.data

import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.datetime.LocalDate
import com.example.aguardapp.feature.notificaciones.domain.model.EstadoNotificaciones
import com.example.aguardapp.feature.notificaciones.domain.repository.EstadoNotificacionesRepository

class PreferenciasNotificaciones(private val preferencias: SharedPreferences) : EstadoNotificacionesRepository {
    var permisoSolicitado: Boolean
        get() = preferencias.getBoolean(PERMISO_SOLICITADO, false)
        set(valor) { preferencias.edit { putBoolean(PERMISO_SOLICITADO, valor) } }

    override fun leer() = EstadoNotificaciones(
        ultimoPorcentaje = preferencias.getString(ULTIMO_PORCENTAJE, null)?.toDoubleOrNull(),
        ultimoDelSensor = preferencias.getBoolean(ULTIMO_DEL_SENSOR, false),
        llenandose = preferencias.getBoolean(LLENANDOSE, false),
        ultimoAvisoId = preferencias.getString(ULTIMO_AVISO, null),
        rachaRecordadaEl = preferencias.getString(RACHA_RECORDADA, null)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    )

    override fun guardar(estado: EstadoNotificaciones) {
        preferencias.edit {
            putString(ULTIMO_PORCENTAJE, estado.ultimoPorcentaje?.toString())
            putBoolean(ULTIMO_DEL_SENSOR, estado.ultimoDelSensor)
            putBoolean(LLENANDOSE, estado.llenandose)
            putString(ULTIMO_AVISO, estado.ultimoAvisoId)
            putString(RACHA_RECORDADA, estado.rachaRecordadaEl?.toString())
        }
    }

    private companion object {
        const val PERMISO_SOLICITADO = "permiso_solicitado"
        const val ULTIMO_PORCENTAJE = "ultimo_porcentaje"
        const val ULTIMO_DEL_SENSOR = "ultimo_del_sensor"
        const val LLENANDOSE = "llenandose"
        const val ULTIMO_AVISO = "ultimo_aviso"
        const val RACHA_RECORDADA = "racha_recordada"
    }
}
