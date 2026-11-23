package com.example.aguardapp.feature.cortes

import com.example.aguardapp.feature.cortes.data.remote.CorteDto
import com.example.aguardapp.feature.cortes.presentation.formatearFechaDeCorte
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CorteAguaTest {
    @Test
    fun avisoDelServidorConservaSectorYFechaEstimada() {
        val json = """
            {"id":"a1","titulo":"Corte","fuente":"SUNASS","url":"https://servicio.sunass.gob.pe/",
             "tipo":"PROGRAMADO","distrito":"Tacna","sector":"Av. Manuel A. Odría y Los Cedros",
             "inicio":"2026-11-22","reposicion_estimada":"2026-11-22T15:30-05:00",
             "categoria":"MANTENIMIENTO","sectores":["V"],"sector_origen":"REFERENCIA"}
        """.trimIndent()
        val corte = Gson().fromJson(json, CorteDto::class.java).aDominio()

        assertEquals(listOf("V"), corte.sectores)
        assertEquals("REFERENCIA", corte.sectorOrigen)
        assertEquals("MANTENIMIENTO", corte.categoria)
        assertEquals(listOf("Tacna"), corte.distritos)
        assertEquals("22/11/2026", formatearFechaDeCorte(corte.inicio))
        assertEquals("22/11/2026 a las 15:30", formatearFechaDeCorte(corte.reposicionEstimada))
        assertNull(formatearFechaDeCorte(corte.restablecido))
    }
}
