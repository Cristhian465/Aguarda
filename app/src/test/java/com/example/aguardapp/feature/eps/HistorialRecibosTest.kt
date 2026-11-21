package com.example.aguardapp.feature.eps

import com.example.aguardapp.feature.eps.domain.MesRecibo
import com.example.aguardapp.feature.eps.domain.ReciboGuardado
import com.example.aguardapp.feature.eps.domain.actualizarHistorial
import com.example.aguardapp.feature.eps.domain.compararRecibos
import com.example.aguardapp.feature.eps.domain.leerDiasFacturados
import com.example.aguardapp.feature.eps.domain.extraerReciboEps
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HistorialRecibosTest {
    @Test fun importarUnPeriodoAnteriorNoDesplazaElUltimoYUnaActualizacionNoDuplica() {
        val noviembre = recibo(11, 8.0, total = 30.0)
        val octubre = recibo(10, 6.0)
        val historial = actualizarHistorial(listOf(noviembre), octubre)
        val corregido = octubre.copy(metrosCubicos = 7.0)
        assertEquals(listOf(noviembre, corregido), actualizarHistorial(historial, corregido))
    }

    @Test fun unaFotoPuedeCompletarLosDiasSinReemplazarLosDatosOficiales() {
        val oficial = recibo(11, 8.0, total = 30.0, eps = true)
        val foto = recibo(11, 7.0, total = 25.0, dias = 30)
        assertEquals(listOf(oficial.copy(diasFacturados = 30)), actualizarHistorial(listOf(oficial), foto))
    }

    @Test fun comparaConsumoYTotalPorSeparadoYNormalizaLosDiasReales() {
        val reciente = recibo(11, 9.0, total = 32.0, dias = 30)
        val anterior = recibo(10, 8.4, total = 33.0, dias = 28)
        val cambio = compararRecibos(reciente, anterior)
        assertEquals(0.6, cambio.metrosCubicos, 0.001)
        assertEquals(-1.0, cambio.totalPagar!!, 0.001)
        assertEquals(0.0, cambio.litrosPorDia!!, 0.001)
        val incompleto = compararRecibos(reciente, anterior.copy(totalPagar = null, diasFacturados = null))
        assertNull(incompleto.totalPagar)
        assertNull(incompleto.litrosPorDia)
    }

    @Test fun soloUsaDiasExplicitosOUnRangoIdentificadoComoPeriodoFacturado() {
        assertEquals(31, leerDiasFacturados("Días facturados: 31"))
        assertEquals(29, leerDiasFacturados("PERIODO DE CONSUMO: del 01/02/2028 al 01/03/2028"))
        assertEquals(30, leerDiasFacturados("PERIODO DE CONSUMO\nFecha actual: 16/10/2026\nFecha anterior: 16/09/2026"))
        assertNull(leerDiasFacturados("Emisión 01/10/2026 · Vencimiento 31/10/2026"))
        assertNull(leerDiasFacturados("Días facturados: 30\nDías facturados: 31"))
    }

    @Test fun lasFechasDelCicloNoSeConfundenConElMesFacturado() {
        val recibo = extraerReciboEps(
            "Facturación: noviembre 2026\nPeriodo de consumo: del 15/10/2026 al 15/11/2026" +
                "\nConsumo: 8 m³\nTotal a pagar: S/ 30.00"
        )
        assertEquals(MesRecibo(2026, 11), recibo.mes)
        assertEquals(31, recibo.diasFacturados)
    }

    private fun recibo(mes: Int, consumo: Double, total: Double? = null, dias: Int? = null, eps: Boolean = false) =
        ReciboGuardado(MesRecibo(2026, mes), consumo, !eps, eps, totalPagar = total, diasFacturados = dias)
}
