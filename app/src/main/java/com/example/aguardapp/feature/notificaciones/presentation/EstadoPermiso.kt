package com.example.aguardapp.feature.notificaciones.presentation

enum class EstadoPermiso(val etiqueta: String) {
    CONCEDIDO("Activadas"),
    SIN_SOLICITAR("Sin activar"),
    EXPLICACION("Necesitan tu permiso"),
    BLOQUEADO("Bloqueadas en ajustes")
}

fun estadoPermiso(concedido: Boolean, debeExplicar: Boolean, solicitadoAntes: Boolean, puedeSolicitar: Boolean): EstadoPermiso =
    when {
        concedido -> EstadoPermiso.CONCEDIDO
        !puedeSolicitar -> EstadoPermiso.BLOQUEADO
        debeExplicar -> EstadoPermiso.EXPLICACION
        solicitadoAntes -> EstadoPermiso.BLOQUEADO
        else -> EstadoPermiso.SIN_SOLICITAR
    }
