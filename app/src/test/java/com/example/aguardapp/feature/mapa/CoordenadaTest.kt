package com.example.aguardapp.feature.mapa

import com.example.aguardapp.feature.mapa.domain.model.Coordenada
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CoordenadaTest {
    @Test
    fun unGradoDeLatitudEquivaleAUnos111Km() {
        val distancia = Coordenada(-18.0, -70.25).distanciaKmHasta(Coordenada(-17.0, -70.25))
        assertEquals(111.19, distancia, 0.01)
    }

    @Test
    fun laMismaCoordenadaEstaADistanciaCero() {
        val casa = Coordenada(-18.0, -70.25)
        assertEquals(0.0, casa.distanciaKmHasta(casa), 0.0001)
    }

    @Test
    fun laDistanciaEsLaMismaEnAmbosSentidos() {
        val vinani = Coordenada(-18.0495, -70.2548)
        val ciudadNueva = Coordenada(-17.9845, -70.2365)
        assertEquals(vinani.distanciaKmHasta(ciudadNueva), ciudadNueva.distanciaKmHasta(vinani), 0.0001)
    }

    @Test
    fun rechazaUnaLatitudFueraDeRango() {
        assertThrows(IllegalArgumentException::class.java) { Coordenada(-95.0, -70.25) }
    }

    @Test
    fun rechazaUnaLongitudFueraDeRango() {
        assertThrows(IllegalArgumentException::class.java) { Coordenada(-18.0, 181.0) }
    }
}
