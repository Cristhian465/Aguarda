package com.example.aguardapp.feature.eps

import com.example.aguardapp.feature.eps.domain.ErrorEps
import com.example.aguardapp.feature.eps.domain.extraerReciboEps
import com.example.aguardapp.feature.eps.domain.MesRecibo
import com.example.aguardapp.feature.eps.domain.ReciboGuardado
import kotlinx.datetime.LocalDate
import org.junit.Assert.*
import org.junit.Test

/** Ejemplos sintéticos: no sustituyen validar un recibo real de EPS Tacna. */
class ExtraerReciboEpsTest {
    private fun texto(total: String = "S/ 45.20") = "Facturación: NOVIEMBRE 2026\nConsumo del mes: 18 m³\nTotal a pagar: $total"

    @Test fun extraeConsumoPeriodoYTotalDiferenciandoLasLecturas() {
        val recibo = extraerReciboEps("Lectura anterior: 300 m3\nLectura actual: 318 m3\n" + texto())
        assertEquals(MesRecibo(2026, 11), recibo.mes)
        assertEquals(18.0, recibo.metrosCubicos, 0.001)
        assertEquals(45.20, recibo.totalPagar, 0.001)
    }

    @Test fun admiteFormatosDeImporteConSeparadorDeMiles() {
        assertEquals(1234.56, extraerReciboEps(texto("S/ 1,234.56")).totalPagar, 0.001)
        assertEquals(1234.56, extraerReciboEps(texto("S/ 1.234,56")).totalPagar, 0.001)
        assertEquals(45.20, extraerReciboEps(texto("S/ 45,20")).totalPagar, 0.001)
    }

    @Test fun noDeduceConsumoAPartirDelImporteOLasLecturas() {
        assertThrows(ErrorEps::class.java) { extraerReciboEps("Facturación: NOVIEMBRE 2026\nLectura actual 318 m3\nTotal a pagar: S/ 45.20") }
    }

    @Test fun noUsaElPromedioHistoricoComoConsumoDelMes() {
        assertThrows(ErrorEps::class.java) { extraerReciboEps(texto().replace("Consumo del mes", "Consumo promedio")) }
    }

    @Test fun noAceptaResultadosAmbiguosOIncompletos() {
        listOf(texto() + "\nConsumo del mes: 20 m3", texto() + "\nFacturación: OCTUBRE 2026",
            texto() + "\nTotal a pagar: S/ 90.00", texto().replace("Total a pagar: S/ 45.20", ""))
            .forEach { assertThrows(ErrorEps::class.java) { extraerReciboEps(it) } }
    }

    @Test fun aceptaConsumoCeroYRechazaDosConsumosEnLaMismaLinea() {
        assertEquals(0.0, extraerReciboEps(texto().replace("18 m³", "0 m³")).metrosCubicos, 0.001)
        assertThrows(ErrorEps::class.java) { extraerReciboEps(texto().replace("18 m³", "18 m³ o 20 m³")) }
    }

    @Test fun elOrigenEpsVerificaSoloRecibosVigentes() {
        assertTrue(ReciboGuardado(MesRecibo(2026, 11), 18.0, false, leidoDeEps = true).verificado(LocalDate(2026, 11, 18)))
        assertFalse(ReciboGuardado(MesRecibo(2026, 7), 18.0, false, leidoDeEps = true).verificado(LocalDate(2026, 11, 18)))
        assertFalse(ReciboGuardado(MesRecibo(2026, 11), 18.0, false).verificado(LocalDate(2026, 11, 18)))
    }

    @Test fun leeLasEtiquetasAbreviadasDelPdfDeEpsConValoresEnOtraLinea() {
        val recibo = extraerReciboEps("Facturación:\nNOVIEMBRE-2026\nActual 180\nAnterior 165\nVolumen Fac m³\n15\nTOTAL A PAGAR:\nS/ 45.20")
        assertEquals(15.0, recibo.metrosCubicos, 0.001)
        assertEquals(MesRecibo(2026, 11), recibo.mes)
        assertEquals(45.20, recibo.totalPagar, 0.001)
    }

    @Test fun leeElRellenoDeAsteriscosDelImporteDeEps() {
        val recibo = extraerReciboEps("Facturación: NOVIEMBRE-2026 Ciclo: 002\nVolumen Fac m 15\nTOTAL A PAGAR: S/ *****45.20")
        assertEquals(15.0, recibo.metrosCubicos, 0.001)
        assertEquals(45.20, recibo.totalPagar, 0.001)
    }
}
