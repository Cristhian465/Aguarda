package com.example.aguardapp.feature.liga

import com.example.aguardapp.feature.liga.domain.LitrosSemana
import com.example.aguardapp.feature.liga.domain.MarcaNivel
import com.example.aguardapp.feature.liga.domain.SEMANAS_DEL_HISTORIAL
import com.example.aguardapp.feature.liga.domain.historialSemanal
import com.example.aguardapp.feature.liga.presentation.alturaRelativa
import com.example.aguardapp.feature.liga.presentation.etiquetaDeSemana
import com.example.aguardapp.feature.liga.presentation.maximoDelGrafico
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class HistorialLitrosTest {
    private val hoy = LocalDate(2026, 11, 20)

    private fun marca(mes: Int, dia: Int, hora: Int, litros: Double) = MarcaNivel(LocalDateTime(2026, mes, dia, hora, 0), litros)

    @Test
    fun devuelveLasUltimasSemanasDeLaMasAntiguaALaActual() {
        val historial = historialSemanal(emptyList(), emptyList(), 4, hoy)
        assertEquals(SEMANAS_DEL_HISTORIAL, historial.size)
        assertEquals(LocalDate(2026, 11, 16), historial.last().lunes)
        assertEquals(LocalDate(2026, 10, 12), historial.first().lunes)
    }

    @Test
    fun sinMarcasNingunaSemanaTieneLitros() {
        assertEquals(true, historialSemanal(emptyList(), emptyList(), 4, hoy).all { it.litrosPorPersonaDia == null })
    }

    @Test
    fun cadaSemanaSeMideConSusPropiasMarcas() {
        val marcas = listOf(
            marca(11, 9, 8, 800.0), marca(11, 10, 8, 600.0),
            marca(11, 16, 8, 900.0), marca(11, 17, 8, 600.0)
        )
        val historial = historialSemanal(marcas, emptyList(), 4, hoy)
        assertEquals(50.0, historial[historial.size - 2].litrosPorPersonaDia!!, 0.01)
        assertEquals(75.0, historial.last().litrosPorPersonaDia!!, 0.01)
        assertNull(historial.first().litrosPorPersonaDia)
        assertNotNull(historial.last().litrosPorPersonaDia)
    }

    @Test
    fun laAlturaDeLaBarraEsProporcionalAlMaximo() {
        assertEquals(0.5f, alturaRelativa(50.0, 100.0), 0.001f)
        assertEquals(1f, alturaRelativa(150.0, 100.0), 0.001f)
        assertEquals(0f, alturaRelativa(0.0, 100.0), 0.001f)
    }

    @Test
    fun elMaximoDelGraficoIncluyeElConsumoHabitual() {
        val historial = listOf(LitrosSemana(hoy, 60.0), LitrosSemana(hoy, null))
        assertEquals(60.0, maximoDelGrafico(historial, null), 0.001)
        assertEquals(95.0, maximoDelGrafico(historial, 95.0), 0.001)
        assertEquals(1.0, maximoDelGrafico(emptyList(), null), 0.001)
    }

    @Test
    fun laEtiquetaDiceLaFechaOEstaParaLaSemanaActual() {
        assertEquals("16 nov", etiquetaDeSemana(LocalDate(2026, 11, 16), false))
        assertEquals("5 oct", etiquetaDeSemana(LocalDate(2026, 10, 5), false))
        assertEquals("Esta", etiquetaDeSemana(LocalDate(2026, 11, 16), true))
    }
}
