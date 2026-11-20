package com.example.aguardapp.feature.mapa

import com.example.aguardapp.feature.mapa.presentation.formatearDistancia
import org.junit.Assert.assertEquals
import org.junit.Test

class FormatoDistanciaTest {
    @Test
    fun menosDeUnKilometroSeMuestraEnMetrosRedondeadosADecenas() {
        assertEquals("a 850 m", formatearDistancia(0.853))
    }

    @Test
    fun nuncaMuestraCeroMetros() {
        assertEquals("a 10 m", formatearDistancia(0.001))
    }

    @Test
    fun desdeUnKilometroSeMuestraConUnDecimal() {
        assertEquals("a 1.2 km", formatearDistancia(1.24))
        assertEquals("a 7.0 km", formatearDistancia(7.01))
    }

    @Test
    fun casiUnKilometroPasaAKilometros() {
        assertEquals("a 1.0 km", formatearDistancia(0.998))
    }
}
