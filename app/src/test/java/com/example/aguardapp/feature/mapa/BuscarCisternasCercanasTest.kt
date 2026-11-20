package com.example.aguardapp.feature.mapa

import com.example.aguardapp.feature.mapa.domain.model.Coordenada
import com.example.aguardapp.feature.mapa.domain.model.EstadoCisterna
import com.example.aguardapp.feature.mapa.domain.model.PuntoCisterna
import com.example.aguardapp.feature.mapa.domain.usecase.BuscarCisternasCercanas
import kotlinx.datetime.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BuscarCisternasCercanasTest {
    private val buscar = BuscarCisternasCercanas()
    private val casa = Coordenada(-18.0000, -70.2500)

    private fun punto(id: String, latitud: Double, estado: EstadoCisterna = EstadoCisterna.ACTIVO) = PuntoCisterna(
        id = id,
        nombre = id,
        ubicacion = Coordenada(latitud, -70.2500),
        horarioInicio = LocalTime(7, 0),
        horarioFin = LocalTime(13, 0),
        estado = estado
    )

    private val puntos = listOf(
        punto("lejos", -18.0300),
        punto("cerca", -18.0100),
        punto("fuera", -18.0600)
    )

    @Test
    fun descartaLasQueEstanFueraDelRadio() {
        val ids = buscar.buscar(puntos, casa, radioKm = 4.0).map { it.punto.id }
        assertEquals(listOf("cerca", "lejos"), ids)
    }

    @Test
    fun ordenaDeLaMasCercanaALaMasLejana() {
        val resultado = buscar.buscar(puntos, casa, radioKm = 10.0)
        assertEquals(listOf("cerca", "lejos", "fuera"), resultado.map { it.punto.id })
        assertEquals(1.11, resultado.first().distanciaKm, 0.01)
    }

    @Test
    fun incluyeLasQueYaTerminaronSuReparto() {
        val terminada = punto("terminada", -18.0050, EstadoCisterna.TERMINADO)
        val resultado = buscar.buscar(listOf(terminada), casa)
        assertEquals(EstadoCisterna.TERMINADO, resultado.single().punto.estado)
    }

    @Test
    fun usaUnRadioDe10KmPorDefecto() {
        val aOnceKm = punto("a-11-km", -18.1000)
        assertTrue(buscar.buscar(listOf(aOnceKm), casa).isEmpty())
    }

    @Test
    fun sinPuntosDevuelveUnaListaVacia() {
        assertTrue(buscar.buscar(emptyList(), casa).isEmpty())
    }
}
