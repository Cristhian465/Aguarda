package com.example.aguardapp.feature.liga

import com.example.aguardapp.feature.liga.domain.FilaRanking
import com.example.aguardapp.feature.eps.domain.MesRecibo
import com.example.aguardapp.feature.eps.domain.ReciboGuardado
import com.example.aguardapp.feature.liga.domain.duelosDeLaSemana
import com.example.aguardapp.feature.eps.domain.leerConsumoDelRecibo
import com.example.aguardapp.feature.eps.domain.leerMesDelRecibo
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReciboYDuelosTest {
    private val hoy = LocalDate(2026, 11, 18)

    private fun fila(ubigeo: String, puntaje: Double?) = FilaRanking(null, ubigeo, ubigeo, 5, 0, puntaje)

    @Test
    fun leeElConsumoDeLaLineaQueDiceConsumo() {
        assertEquals(18.0, leerConsumoDelRecibo("Lectura actual 1250 m3\nConsumo del mes: 18 m3\nTotal S/ 45.20")!!, 0.001)
    }

    @Test
    fun aceptaLaUnidadAntesDelNumeroYConTilde() {
        assertEquals(12.5, leerConsumoDelRecibo("CONSUMO (m³): 12,5")!!, 0.001)
    }

    @Test
    fun conUnSoloValorEnM3LoUsaAunqueNoDigaConsumo() {
        assertEquals(22.0, leerConsumoDelRecibo("Volumen facturado 22 m3")!!, 0.001)
    }

    @Test
    fun noAdivinaSiHayVariosValoresPosibles() {
        assertNull(leerConsumoDelRecibo("Lectura anterior 300 m3\nLectura actual 318 m3"))
    }

    @Test
    fun sinM3NoDevuelveNada() {
        assertNull(leerConsumoDelRecibo("Total a pagar S/ 45.20"))
    }

    @Test
    fun leeElMesEnLetrasOEnNumeros() {
        assertEquals(MesRecibo(2026, 10), leerMesDelRecibo("Periodo: OCTUBRE 2026"))
        assertEquals(MesRecibo(2026, 9), leerMesDelRecibo("Mes facturado 09/2026"))
        assertEquals(MesRecibo(2026, 9), leerMesDelRecibo("Periodo: Setiembre de 2026"))
    }

    @Test
    fun convierteElReciboALitrosPorPersonaAlDia() {
        val recibo = ReciboGuardado(MesRecibo(2026, 11), 12.0, leidoDeLaFoto = true)
        assertEquals(100.0, recibo.litrosPorPersonaDia(4), 0.01)
    }

    @Test
    fun soloVerificaSiSalioDeLaFotoYEstaVigente() {
        assertTrue(ReciboGuardado(MesRecibo(2026, 10), 12.0, true).verificado(hoy))
        assertFalse(ReciboGuardado(MesRecibo(2026, 10), 12.0, false).verificado(hoy))
        assertFalse(ReciboGuardado(MesRecibo(2026, 6), 12.0, true).verificado(hoy))
    }

    @Test
    fun sinDosDistritosConDatosNoHayDuelos() {
        assertTrue(duelosDeLaSemana(listOf(fila("230101", 50.0), fila("230104", null)), LocalDate(2026, 11, 16)).isEmpty())
    }

    @Test
    fun conNumeroImparUnDistritoDescansa() {
        val filas = listOf(fila("230101", 50.0), fila("230102", 60.0), fila("230104", 70.0))
        assertEquals(1, duelosDeLaSemana(filas, LocalDate(2026, 11, 16)).size)
    }

    @Test
    fun losRivalesCambianDeUnaSemanaAOtra() {
        val filas = listOf(fila("230101", 50.0), fila("230102", 60.0), fila("230104", 70.0), fila("230108", 40.0))
        val estaSemana = duelosDeLaSemana(filas, LocalDate(2026, 11, 16)).first().visitante.ubigeo
        val siguiente = duelosDeLaSemana(filas, LocalDate(2026, 11, 23)).first().visitante.ubigeo
        assertTrue(estaSemana != siguiente)
    }

    @Test
    fun ganaElDeMayorPuntajeYElEmpateNoTieneGanador() {
        val duelo = duelosDeLaSemana(listOf(fila("230101", 50.0), fila("230102", 60.0)), LocalDate(2026, 11, 16)).single()
        assertEquals("230102", duelo.ganador?.ubigeo)
        val empate = duelosDeLaSemana(listOf(fila("230101", 50.0), fila("230102", 50.0)), LocalDate(2026, 11, 16)).single()
        assertNull(empate.ganador)
    }
}
