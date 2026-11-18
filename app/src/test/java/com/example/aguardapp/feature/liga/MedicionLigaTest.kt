package com.example.aguardapp.feature.liga

import com.example.aguardapp.feature.liga.domain.CalcularPuntaje
import com.example.aguardapp.feature.liga.domain.MarcaNivel
import com.example.aguardapp.feature.liga.domain.MedirConsumo
import com.example.aguardapp.feature.liga.domain.lunesDe
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MedicionLigaTest {

    private val medir = MedirConsumo()
    private val calcular = CalcularPuntaje()
    private val lunes = LocalDate(2026, 11, 16)

    private fun marca(dia: Int, hora: Int, litros: Double) = MarcaNivel(LocalDateTime(2026, 11, dia, hora, 0), litros)

    @Test
    fun laSemanaEmpiezaEnLunes() {
        assertEquals(lunes, lunesDe(LocalDate(2026, 11, 22)))
        assertEquals(lunes, lunesDe(lunes))
    }

    @Test
    fun mideLoQueBajoElNivelEntreDosMarcas() {
        val consumo = medir.enLaSemana(listOf(marca(16, 8, 800.0), marca(17, 8, 500.0)), emptyList(), lunes)!!
        assertEquals(300.0, consumo.litros, 0.01)
        assertEquals(24.0, consumo.horas, 0.01)
        assertEquals(75.0, consumo.litrosPorPersonaDia(4), 0.01)
    }

    @Test
    fun conMenosDe24HorasMedidasNoDaNingunNumero() {
        assertNull(medir.enLaSemana(listOf(marca(16, 8, 800.0), marca(16, 20, 700.0)), emptyList(), lunes))
    }

    @Test
    fun descartaElTramoDondeSubioElNivel() {
        val marcas = listOf(marca(16, 8, 300.0), marca(17, 8, 900.0), marca(18, 8, 700.0))
        val consumo = medir.enLaSemana(marcas, emptyList(), lunes)!!
        assertEquals(200.0, consumo.litros, 0.01)
        assertEquals(24.0, consumo.horas, 0.01)
    }

    @Test
    fun descartaElTramoConUnLlenadoEnMedio() {
        val marcas = listOf(marca(16, 8, 800.0), marca(17, 8, 700.0), marca(18, 8, 400.0))
        val consumo = medir.enLaSemana(marcas, listOf(LocalDateTime(2026, 11, 16, 20, 0)), lunes)!!
        assertEquals(300.0, consumo.litros, 0.01)
    }

    @Test
    fun ignoraLasMarcasDeOtraSemana() {
        assertNull(medir.enLaSemana(listOf(marca(15, 8, 800.0), marca(16, 8, 500.0)), emptyList(), lunes))
    }

    @Test
    fun mejorar20PorCientoDaLosPuntosMaximosDeMejora() {
        val puntaje = calcular.puntaje(actual = 80.0, base = 100.0, diasConRetos = 7, diasConMarca = 7)
        assertEquals(100, puntaje.total)
        assertEquals(20.0, puntaje.mejoraPct!!, 0.01)
    }

    @Test
    fun consumirMasQueSuBaseNoRestaPuntos() {
        val puntaje = calcular.puntaje(actual = 120.0, base = 100.0, diasConRetos = 0, diasConMarca = 0)
        assertEquals(0, puntaje.total)
    }

    @Test
    fun sinLineaBaseSoloSumanRetosYConstancia() {
        val puntaje = calcular.puntaje(actual = 90.0, base = null, diasConRetos = 7, diasConMarca = 7)
        assertEquals(0, puntaje.puntosMejora)
        assertEquals(50, puntaje.total)
        assertNull(puntaje.mejoraPct)
    }

    @Test
    fun laLineaBaseSaleDeLasSemanasAnteriores() {
        val marcas = listOf(marca(9, 8, 800.0), marca(10, 8, 400.0), marca(16, 8, 800.0), marca(17, 8, 480.0))
        val resumen = calcular.resumen(marcas, emptyList(), emptySet(), habitantes = 4, hoy = LocalDate(2026, 11, 18))
        assertEquals(100.0, resumen.lineaBase!!, 0.01)
        assertEquals(80.0, resumen.litrosPorPersonaDia!!, 0.01)
        assertEquals(50, resumen.puntaje!!.puntosMejora)
    }

    @Test
    fun sinMedicionDeLaSemanaNoHayPuntaje() {
        val resumen = calcular.resumen(emptyList(), emptyList(), setOf(lunes), habitantes = 4, hoy = lunes)
        assertNull(resumen.puntaje)
        assertEquals(1, resumen.diasConRetos)
    }
}
