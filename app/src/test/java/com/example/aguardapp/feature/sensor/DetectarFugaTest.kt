package com.example.aguardapp.feature.sensor

import com.example.aguardapp.feature.sensor.domain.model.LecturaSensor
import com.example.aguardapp.feature.sensor.domain.usecase.EstadoFuga
import com.example.aguardapp.feature.sensor.domain.usecase.detectarFuga
import com.example.aguardapp.feature.sensor.domain.usecase.ultimaMadrugada
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Lecturas sintéticas: una por minuto durante cuatro horas, como una madrugada de 1 a 5 a. m. */
@OptIn(ExperimentalTime::class)
class DetectarFugaTest {
    private val capacidad = 1100.0
    private val minutos = 240

    private fun lecturas(cantidad: Int = minutos, nivel: (Int) -> Double): List<LecturaSensor> =
        (0 until cantidad).map { minuto ->
            LecturaSensor(nivel(minuto), null, Instant.fromEpochSeconds(1_795_000_000L + minuto * 60L))
        }

    private fun ruido(minuto: Int) = if (minuto % 2 == 0) 0.3 else -0.3

    @Test fun unNivelEstableConRuidoDelSensorNoEsFuga() {
        val analisis = detectarFuga(lecturas { 80.0 + ruido(it) }, capacidad)
        assertEquals(EstadoFuga.SIN_FUGA, analisis.estado)
    }

    @Test fun unaBajadaConstanteEsPosibleFugaYCalculaLosLitros() {
        // Baja 4 puntos en 4 horas: entre el primer y el último bloque hay 3.5 puntos, unos 38 L.
        val analisis = detectarFuga(lecturas { 80.0 - 4.0 * it / minutos }, capacidad)
        assertEquals(EstadoFuga.POSIBLE_FUGA, analisis.estado)
        assertTrue("litros: ${analisis.litrosPerdidos}", analisis.litrosPerdidos in 37..40)
        assertTrue("al mes: ${analisis.litrosAlMes}", analisis.litrosAlMes in 7_500..8_300)
    }

    @Test fun laFugaSeDetectaAunqueElSensorTengaRuido() {
        val analisis = detectarFuga(lecturas { 80.0 - 4.0 * it / minutos + ruido(it) }, capacidad)
        assertEquals(EstadoFuga.POSIBLE_FUGA, analisis.estado)
    }

    @Test fun elOrdenDeLasLecturasNoCambiaElResultado() {
        val ordenadas = lecturas { 80.0 - 4.0 * it / minutos }
        assertEquals(detectarFuga(ordenadas, capacidad), detectarFuga(ordenadas.reversed(), capacidad))
    }

    @Test fun unaBajadaMenorAlRuidoDelSensorNoSeReportaComoFuga() {
        val analisis = detectarFuga(lecturas { 80.0 - 1.0 * it / minutos }, capacidad)
        assertEquals(EstadoFuga.SIN_FUGA, analisis.estado)
    }

    @Test fun unUsoPuntualDeMadrugadaNoSeConfundeConUnaFuga() {
        val analisis = detectarFuga(lecturas { if (it < 120) 80.0 else 76.0 }, capacidad)
        assertEquals(EstadoFuga.NO_CONCLUYENTE, analisis.estado)
    }

    @Test fun unLlenadoDeMadrugadaNoPermiteOpinar() {
        val analisis = detectarFuga(lecturas { if (it < 120) 50.0 else 90.0 }, capacidad)
        assertEquals(EstadoFuga.NO_CONCLUYENTE, analisis.estado)
    }

    @Test fun conPocasLecturasNoOpina() {
        assertEquals(EstadoFuga.SIN_DATOS, detectarFuga(lecturas(cantidad = 10) { 80.0 - it }, capacidad).estado)
        assertEquals(EstadoFuga.SIN_DATOS, detectarFuga(emptyList(), capacidad).estado)
    }

    @Test fun sinCapacidadDelTanqueNoOpina() {
        assertEquals(EstadoFuga.SIN_DATOS, detectarFuga(lecturas { 80.0 - 4.0 * it / minutos }, 0.0).estado)
    }

    @Test fun despuesDeLasCincoSeAnalizaLaMadrugadaDeHoy() {
        val (desde, hasta) = ultimaMadrugada(LocalDateTime(2026, 11, 19, 9, 0))
        assertEquals(LocalDateTime(2026, 11, 19, 1, 0), desde)
        assertEquals(LocalDateTime(2026, 11, 19, 5, 0), hasta)
    }

    @Test fun antesDeLasCincoSeAnalizaLaMadrugadaDeAyer() {
        val (desde, hasta) = ultimaMadrugada(LocalDateTime(2026, 11, 1, 3, 0))
        assertEquals(LocalDateTime(2026, 10, 31, 1, 0), desde)
        assertEquals(LocalDateTime(2026, 10, 31, 5, 0), hasta)
    }
}
