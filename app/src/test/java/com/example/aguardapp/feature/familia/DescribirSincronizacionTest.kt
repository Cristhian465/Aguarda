package com.example.aguardapp.feature.familia

import com.example.aguardapp.feature.familia.presentation.describirSincronizacion
import org.junit.Assert.assertEquals
import org.junit.Test

class DescribirSincronizacionTest {
    private val ahora = 1_800_000_000_000L
    private val minuto = 60_000L

    @Test fun sinSincronizarLoDice() {
        assertEquals("Aún no se ha sincronizado", describirSincronizacion(null, ahora))
    }

    @Test fun describeElTiempoEnMinutosHorasYDias() {
        assertEquals("Sincronizado hace un momento", describirSincronizacion(ahora - 20_000, ahora))
        assertEquals("Sincronizado hace 5 min", describirSincronizacion(ahora - 5 * minuto, ahora))
        assertEquals("Sincronizado hace 3 h", describirSincronizacion(ahora - 190 * minuto, ahora))
        assertEquals("Sincronizado hace 2 días", describirSincronizacion(ahora - 2 * 1_440 * minuto, ahora))
    }

    @Test fun unRelojAdelantadoNoDaTiemposNegativos() {
        assertEquals("Sincronizado hace un momento", describirSincronizacion(ahora + 5 * minuto, ahora))
    }
}
