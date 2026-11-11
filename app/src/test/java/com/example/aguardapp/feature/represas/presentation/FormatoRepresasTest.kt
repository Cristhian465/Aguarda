package com.example.aguardapp.feature.represas.presentation

import org.junit.Assert.assertEquals
import org.junit.Test
import com.example.aguardapp.feature.represas.data.mapper.aNombrePropio
import com.example.aguardapp.feature.represas.data.mapper.limpio
import com.example.aguardapp.feature.represas.data.mapper.usosDe
import com.example.aguardapp.feature.represas.domain.model.UsoPresa

class FormatoRepresasTest {

    @Test
    fun formateaDecimalesSinCerosDeMas() {
        assertEquals("8.5", formatearDecimal(8.5))
        assertEquals("2.35", formatearDecimal(2.349))
        assertEquals("12", formatearDecimal(12.0))
        assertEquals("4 545", formatearDecimal(4545.0))
    }

    @Test
    fun describeLaCantidadDeTanquesConPalabras() {
        assertEquals("7.7 millones de tanques", describirTanques(7_727_272.7))
        assertEquals("545 mil tanques", describirTanques(545_454.5))
        assertEquals("850 tanques", describirTanques(850.0))
    }

    @Test
    fun describeLosUsosDeLaRepresa() {
        assertEquals("Riego y suministro de agua", describirUsos(setOf(UsoPresa.AGUA_POTABLE, UsoPresa.RIEGO)))
        assertEquals("Minería", describirUsos(setOf(UsoPresa.MINERIA)))
        assertEquals("Sin dato", describirUsos(emptySet()))
    }

    @Test
    fun limpiaLosTextosDeLaAna() {
        assertEquals("Pampa de Vaca", "PAMPA DE VACA".aNombrePropio())
        assertEquals("Huayñuma", "HUAYÑUMA".aNombrePropio())
        assertEquals("Palca", " Palca ".limpio())
        assertEquals(null, "No disponible".limpio())
        assertEquals(setOf(UsoPresa.MINERIA), usosDe("Minería (X) Minería"))
    }
}
