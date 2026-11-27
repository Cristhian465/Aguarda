package com.example.aguardapp.feature.widget

import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import com.example.aguardapp.feature.sensor.domain.model.LecturaSensor
import com.example.aguardapp.feature.widget.domain.model.EstadoTanque
import com.example.aguardapp.feature.widget.domain.usecase.estaConectado
import com.example.aguardapp.feature.widget.domain.usecase.estadoTanque

@OptIn(ExperimentalTime::class)
class EstadoTanqueTest {
    private val ahora = Instant.parse("2026-11-27T12:00:00Z")

    @Test
    fun calculaPorcentajeYLitrosConLaCapacidad() {
        val lectura = LecturaSensor(14.2, 105.8, ahora)
        assertEquals(EstadoTanque(14, 142, 1000, true), estadoTanque(lectura, 1000.0, conectado = true))
    }

    @Test
    fun sinLecturaMuestraCero() {
        assertEquals(EstadoTanque(0, 0, 1000, false), estadoTanque(null, 1000.0, conectado = false))
    }

    @Test
    fun estaConectadoSiLaLecturaTieneMenosDeDiezMinutos() {
        assertTrue(estaConectado(LecturaSensor(50.0, null, ahora - 9.minutes), ahora))
        assertFalse(estaConectado(LecturaSensor(50.0, null, ahora - 11.minutes), ahora))
    }
}
