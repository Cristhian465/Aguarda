package com.example.aguardapp.feature.notificaciones

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import com.example.aguardapp.feature.notificaciones.domain.model.AvisoReciente
import com.example.aguardapp.feature.notificaciones.domain.model.EstadoNotificaciones
import com.example.aguardapp.feature.notificaciones.domain.model.EstadoRacha
import com.example.aguardapp.feature.notificaciones.domain.model.LecturaNivel
import com.example.aguardapp.feature.notificaciones.domain.model.Revision
import com.example.aguardapp.feature.notificaciones.domain.model.TipoNotificacion
import com.example.aguardapp.feature.notificaciones.domain.usecase.evaluarNotificaciones

class EvaluarNotificacionesTest {
    private val manana = LocalDateTime(2026, 11, 22, 9, 0)
    private val noche = LocalDateTime(2026, 11, 22, 20, 0)
    private val sinRacha = EstadoRacha(0, cumplidaHoy = false)

    private fun revision(nivel: LecturaNivel? = null, aviso: AvisoReciente? = null, racha: EstadoRacha = sinRacha, ahora: LocalDateTime = manana) =
        Revision(nivel, aviso, racha, ahora)

    private fun tipos(revision: Revision, estado: EstadoNotificaciones) =
        evaluarNotificaciones(revision, estado).notificaciones.map { it.tipo }

    @Test
    fun laPrimeraRevisionSoloGuardaElEstado() {
        val resultado = evaluarNotificaciones(
            revision(LecturaNivel(10.0, true), AvisoReciente("a1", "Corte", "EPS")),
            EstadoNotificaciones()
        )
        assertTrue(resultado.notificaciones.isEmpty())
        assertEquals(10.0, resultado.estado.ultimoPorcentaje!!, 0.0)
        assertEquals("a1", resultado.estado.ultimoAvisoId)
    }

    @Test
    fun avisaUnaSolaVezQueVolvioElAgua() {
        val antes = EstadoNotificaciones(ultimoPorcentaje = 30.0, ultimoDelSensor = true)
        val primera = evaluarNotificaciones(revision(LecturaNivel(45.0, true)), antes)
        assertEquals(listOf(TipoNotificacion.AGUA_REGRESO), primera.notificaciones.map { it.tipo })
        assertTrue(primera.estado.llenandose)
        assertTrue(tipos(revision(LecturaNivel(60.0, true)), primera.estado).isEmpty())
    }

    @Test
    fun noAvisaRegresoConElNivelEstimado() {
        val antes = EstadoNotificaciones(ultimoPorcentaje = 30.0, ultimoDelSensor = false)
        assertTrue(tipos(revision(LecturaNivel(100.0, false)), antes).isEmpty())
    }

    @Test
    fun avisaTanqueLlenoAlCruzarElNoventaYCinco() {
        val antes = EstadoNotificaciones(ultimoPorcentaje = 90.0, ultimoDelSensor = true, llenandose = true)
        assertEquals(listOf(TipoNotificacion.TANQUE_LLENO), tipos(revision(LecturaNivel(97.0, true)), antes))
        val yaLleno = antes.copy(ultimoPorcentaje = 97.0)
        assertFalse(TipoNotificacion.TANQUE_LLENO in tipos(revision(LecturaNivel(99.0, true)), yaLleno))
    }

    @Test
    fun avisaTanqueBajoAlCruzarElVeinte() {
        val antes = EstadoNotificaciones(ultimoPorcentaje = 23.0, ultimoDelSensor = false)
        assertEquals(listOf(TipoNotificacion.TANQUE_BAJO), tipos(revision(LecturaNivel(19.0, false)), antes))
        val yaBajo = antes.copy(ultimoPorcentaje = 19.0)
        assertTrue(tipos(revision(LecturaNivel(15.0, false)), yaBajo).isEmpty())
    }

    @Test
    fun avisaSoloLosAvisosNuevos() {
        val antes = EstadoNotificaciones(ultimoAvisoId = "a1")
        assertTrue(tipos(revision(aviso = AvisoReciente("a1", "Corte", "EPS")), antes).isEmpty())
        val nuevo = evaluarNotificaciones(revision(aviso = AvisoReciente("a2", "Mantenimiento", "EPS Tacna")), antes)
        assertEquals(listOf(TipoNotificacion.NUEVO_AVISO), nuevo.notificaciones.map { it.tipo })
        assertEquals("Mantenimiento", nuevo.notificaciones.single().mensaje)
        assertEquals("a2", nuevo.estado.ultimoAvisoId)
    }

    @Test
    fun recuerdaLaRachaUnaVezPorNocheSiNoSeCumplioHoy() {
        val enRiesgo = EstadoRacha(4, cumplidaHoy = false)
        val primera = evaluarNotificaciones(revision(racha = enRiesgo, ahora = noche), EstadoNotificaciones())
        assertEquals(listOf(TipoNotificacion.RACHA), primera.notificaciones.map { it.tipo })
        assertEquals(LocalDate(2026, 11, 22), primera.estado.rachaRecordadaEl)
        assertTrue(tipos(revision(racha = enRiesgo, ahora = noche), primera.estado).isEmpty())
    }

    @Test
    fun noRecuerdaLaRachaDeDiaNiSiYaSeCumplio() {
        assertTrue(tipos(revision(racha = EstadoRacha(4, false), ahora = manana), EstadoNotificaciones()).isEmpty())
        assertTrue(tipos(revision(racha = EstadoRacha(4, true), ahora = noche), EstadoNotificaciones()).isEmpty())
        assertTrue(tipos(revision(racha = EstadoRacha(0, false), ahora = noche), EstadoNotificaciones()).isEmpty())
    }
}
