package com.example.aguardapp.feature.liga

import com.example.aguardapp.feature.liga.domain.PosicionDistrito
import com.example.aguardapp.feature.liga.domain.armarRanking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RankingLigaTest {
    private val distritos = listOf("230101" to "Tacna", "230104" to "Ciudad Nueva", "230108" to "Pocollay")

    private fun posicion(ubigeo: String, hogares: Int, puntaje: Double?) = PosicionDistrito(ubigeo, hogares, 0, puntaje, null)

    @Test
    fun ordenaDeMayorAMenorPuntaje() {
        val filas = armarRanking(distritos, listOf(posicion("230101", 6, 40.0), posicion("230104", 9, 72.5)))
        assertEquals(listOf("Ciudad Nueva", "Tacna", "Pocollay"), filas.map { it.nombre })
        assertEquals(listOf(1, 2, null), filas.map { it.puesto })
    }

    @Test
    fun unDistritoSinCincoHogaresNoTienePuesto() {
        val fila = armarRanking(distritos, listOf(posicion("230101", 3, null))).first { it.ubigeo == "230101" }
        assertNull(fila.puesto)
        assertEquals(3, fila.hogares)
    }

    @Test
    fun sinResultadosNingunDistritoCompite() {
        val filas = armarRanking(distritos, emptyList())
        assertEquals(3, filas.size)
        filas.forEach {
            assertNull(it.puntaje)
            assertEquals(0, it.hogares)
        }
    }
}
