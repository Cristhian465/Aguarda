package com.example.aguardapp.feature.notificaciones.domain.repository

import kotlinx.datetime.LocalDate
import com.example.aguardapp.feature.notificaciones.domain.model.AvisoReciente
import com.example.aguardapp.feature.notificaciones.domain.model.EstadoNotificaciones
import com.example.aguardapp.feature.notificaciones.domain.model.EstadoRacha
import com.example.aguardapp.feature.notificaciones.domain.model.LecturaNivel
import com.example.aguardapp.feature.notificaciones.domain.model.Notificacion

interface FuentesNotificaciones {
    suspend fun nivelActual(): LecturaNivel?

    suspend fun avisoMasReciente(): AvisoReciente?

    fun racha(hoy: LocalDate): EstadoRacha
}

interface EstadoNotificacionesRepository {
    fun leer(): EstadoNotificaciones

    fun guardar(estado: EstadoNotificaciones)
}

interface Notificador {
    fun mostrar(notificacion: Notificacion)
}
