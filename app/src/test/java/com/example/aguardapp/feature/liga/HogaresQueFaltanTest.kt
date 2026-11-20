package com.example.aguardapp.feature.liga

import com.example.aguardapp.feature.liga.domain.FilaRanking
import com.example.aguardapp.feature.liga.domain.HOGARES_MINIMOS
import com.example.aguardapp.feature.liga.domain.hogaresQueFaltan
import com.example.aguardapp.feature.liga.presentation.textoHogares
import com.example.aguardapp.feature.liga.presentation.textoHogaresQueFaltan
import org.junit.Assert.assertEquals
import org.junit.Test

class HogaresQueFaltanTest {
    private fun fila(hogares: Int) = FilaRanking(null, "230101", "Tacna", hogares, 0, null)

    @Test
    fun faltanLosHogaresHastaElMinimo() {
        assertEquals(HOGARES_MINIMOS, fila(0).hogaresQueFaltan)
        assertEquals(4, fila(1).hogaresQueFaltan)
        assertEquals(0, fila(HOGARES_MINIMOS).hogaresQueFaltan)
        assertEquals(0, fila(HOGARES_MINIMOS + 3).hogaresQueFaltan)
    }

    @Test
    fun elTextoDiceCuantosFaltanEnVezDeSinDatos() {
        assertEquals("Faltan 5 hogares", textoHogaresQueFaltan(5))
        assertEquals("Falta 1 hogar", textoHogaresQueFaltan(1))
        assertEquals("Calculando", textoHogaresQueFaltan(0))
    }

    @Test
    fun elTextoDeHogaresCuentaLosVerificados() {
        assertEquals("Aún sin hogares", textoHogares(0, 0))
        assertEquals("1 hogar", textoHogares(1, 0))
        assertEquals("6 hogares · 2 verificados", textoHogares(6, 2))
        assertEquals("9 hogares · 1 verificado", textoHogares(9, 1))
    }
}
