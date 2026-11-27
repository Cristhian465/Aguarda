package com.example.aguardapp.feature.notificaciones.presentation

import androidx.lifecycle.ViewModel
import com.example.aguardapp.feature.notificaciones.data.PreferenciasNotificaciones
import com.example.aguardapp.feature.notificaciones.domain.model.Notificacion
import com.example.aguardapp.feature.notificaciones.domain.model.TipoNotificacion
import com.example.aguardapp.feature.notificaciones.domain.repository.Notificador

class NotificacionesViewModel(
    private val preferencias: PreferenciasNotificaciones,
    private val notificador: Notificador
) : ViewModel() {
    val permisoSolicitado: Boolean get() = preferencias.permisoSolicitado

    fun onPermisoSolicitado() {
        preferencias.permisoSolicitado = true
    }

    fun onProbar() {
        notificador.mostrar(
            Notificacion(
                TipoNotificacion.AGUA_REGRESO,
                "Así te avisará Aguarda",
                "Recibirás avisos cuando vuelva el agua, cuando tu tanque se llene o baje al 20 %, cuando salga un aviso nuevo y para continuar tu racha."
            )
        )
    }
}
