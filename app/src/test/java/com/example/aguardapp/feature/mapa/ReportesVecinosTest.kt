package com.example.aguardapp.feature.mapa

import com.example.aguardapp.feature.mapa.data.mensajeDelServidor
import com.example.aguardapp.feature.mapa.domain.model.ReporteVecinos
import com.example.aguardapp.feature.mapa.domain.model.TipoReporte
import com.example.aguardapp.feature.mapa.presentation.PanelReporte
import com.example.aguardapp.feature.mapa.presentation.avisoDeCisterna
import com.example.aguardapp.feature.mapa.presentation.avisoDeDistrito
import com.example.aguardapp.feature.mapa.presentation.textoHace
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportesVecinosTest {
    private fun reporte(tipo: TipoReporte, ubigeo: String = "230110", punto: String? = null, hogares: Int = 2, hace: Int = 10) =
        ReporteVecinos(tipo, ubigeo, punto, hogares, hace)

    @Test
    fun sinReportesNoHayAviso() {
        assertNull(avisoDeDistrito(emptyList(), "230110"))
        assertNull(avisoDeCisterna(emptyList(), "PC-01"))
    }

    @Test
    fun elAvisoDelDistritoSoloUsaReportesDeEseDistrito() {
        val reportes = listOf(reporte(TipoReporte.SIN_AGUA, "230101"), reporte(TipoReporte.SIN_AGUA, "230110", hogares = 3))
        assertEquals("3 vecinos reportan sin agua · hace 10 min", avisoDeDistrito(reportes, "230110"))
        assertNull(avisoDeDistrito(reportes, "230104"))
    }

    @Test
    fun entreSinAguaYVolvioGanaElReporteMasReciente() {
        val reportes = listOf(reporte(TipoReporte.SIN_AGUA, hace = 90), reporte(TipoReporte.VOLVIO_AGUA, hace = 15))
        assertEquals("2 vecinos dicen que ya volvió el agua · hace 15 min", avisoDeDistrito(reportes, "230110"))
    }

    @Test
    fun losReportesDeCisternaNoSeMezclanConLosDelDistrito() {
        val reportes = listOf(reporte(TipoReporte.CISTERNA_LLEGO, punto = "PC-01"))
        assertNull(avisoDeDistrito(reportes, "230110"))
        assertEquals("2 vecinos dicen que llegó la cisterna · hace 10 min", avisoDeCisterna(reportes, "PC-01"))
        assertNull(avisoDeCisterna(reportes, "PC-02"))
    }

    @Test
    fun elTextoDelTiempoPasaDeMinutosAHoras() {
        assertEquals("hace un momento", textoHace(0))
        assertEquals("hace 59 min", textoHace(59))
        assertEquals("hace 2 h", textoHace(125))
    }

    @Test
    fun elReporteDeDistritoNecesitaElDistritoYElDeCisternaLaCisterna() {
        assertFalse(PanelReporte().listo)
        assertFalse(PanelReporte(tipo = TipoReporte.SIN_AGUA).listo)
        assertTrue(PanelReporte(tipo = TipoReporte.SIN_AGUA, ubigeo = "230110").listo)
        assertFalse(PanelReporte(tipo = TipoReporte.CISTERNA_LLEGO, ubigeo = "230110").listo)
        assertTrue(PanelReporte(tipo = TipoReporte.CISTERNA_LLEGO, puntoId = "PC-01").listo)
    }

    @Test
    fun conocePorSuClaveLosTiposDelServidor() {
        assertEquals(TipoReporte.CISTERNA_SE_FUE, TipoReporte.deClave("cisterna_se_fue"))
        assertNull(TipoReporte.deClave("inventado"))
        assertEquals(setOf(false, true), TipoReporte.entries.map { it.deCisterna }.toSet())
    }

    @Test
    fun soloMuestraLosMensajesPropiosDelServidor() {
        assertEquals(
            "Ya enviaste este reporte hace poco",
            mensajeDelServidor("""{"code":"P0001","details":null,"hint":null,"message":"Ya enviaste este reporte hace poco"}""")
        )
        assertNull(mensajeDelServidor("""{"code":"23514","message":"new row violates check constraint"}"""))
        assertNull(mensajeDelServidor("no es json"))
        assertNull(mensajeDelServidor(null))
    }
}
