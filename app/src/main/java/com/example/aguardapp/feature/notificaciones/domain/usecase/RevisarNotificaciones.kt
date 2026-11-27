package com.example.aguardapp.feature.notificaciones.domain.usecase

import kotlin.coroutines.cancellation.CancellationException
import kotlinx.datetime.LocalDateTime
import com.example.aguardapp.feature.notificaciones.domain.model.Revision
import com.example.aguardapp.feature.notificaciones.domain.repository.EstadoNotificacionesRepository
import com.example.aguardapp.feature.notificaciones.domain.repository.FuentesNotificaciones
import com.example.aguardapp.feature.notificaciones.domain.repository.Notificador

class RevisarNotificaciones(
    private val fuentes: FuentesNotificaciones,
    private val estados: EstadoNotificacionesRepository,
    private val notificador: Notificador,
    private val ahora: () -> LocalDateTime
) {
    suspend operator fun invoke() {
        val momento = ahora()
        val revision = Revision(
            nivel = obtener { fuentes.nivelActual() },
            aviso = obtener { fuentes.avisoMasReciente() },
            racha = fuentes.racha(momento.date),
            ahora = momento
        )
        val resultado = evaluarNotificaciones(revision, estados.leer())
        resultado.notificaciones.forEach(notificador::mostrar)
        estados.guardar(resultado.estado)
    }

    private suspend fun <T> obtener(consulta: suspend () -> T?): T? =
        try {
            consulta()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
}
