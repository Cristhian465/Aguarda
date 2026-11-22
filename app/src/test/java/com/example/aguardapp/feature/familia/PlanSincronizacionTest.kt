package com.example.aguardapp.feature.familia

import com.example.aguardapp.feature.familia.data.tokenLigaDe
import com.example.aguardapp.feature.familia.domain.model.EventoCompartido
import com.example.aguardapp.feature.familia.domain.model.PerfilCompartido
import com.example.aguardapp.feature.familia.domain.model.TipoEventoFamiliar
import com.example.aguardapp.feature.familia.domain.usecase.MAX_EVENTOS_POR_ENVIO
import com.example.aguardapp.feature.familia.domain.usecase.PREFIJO_QR
import com.example.aguardapp.feature.familia.domain.usecase.desdeParaSincronizar
import com.example.aguardapp.feature.familia.domain.usecase.eventosParaGuardar
import com.example.aguardapp.feature.familia.domain.usecase.eventosParaSubir
import com.example.aguardapp.feature.familia.domain.usecase.formatearCodigo
import com.example.aguardapp.feature.familia.domain.usecase.normalizarCodigo
import com.example.aguardapp.feature.familia.domain.usecase.perfilParaAplicar
import com.example.aguardapp.feature.familia.domain.usecase.perfilParaSubir
import com.example.aguardapp.feature.familia.domain.usecase.validarApodo
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanSincronizacionTest {
    private val desde = "2026-08-27"

    private fun id(numero: Int) = "%08d-0000-4000-8000-000000000000".format(numero)

    private fun llenado(numero: Int, momento: String = "2026-11-20T08:00") =
        EventoCompartido(id(numero), TipoEventoFamiliar.LLENADO, momento, 1000.0)

    private val perfil = PerfilCompartido("TANQUE_ELEVADO", 1100.0, 4, 4, true, false, "05:00", 20.5, null)

    @Test fun desdeSonOchentaYCincoDiasAtras() {
        assertEquals(desde, desdeParaSincronizar(LocalDate(2026, 11, 20)))
    }

    @Test fun subeSoloLosEventosQueElServidorNoConoce() {
        val subir = eventosParaSubir(listOf(llenado(1), llenado(2), llenado(3)), setOf(id(2)), desde)
        assertEquals(listOf(id(1), id(3)), subir.map { it.id })
    }

    @Test fun noSubeEventosAnterioresAlPeriodoCompartido() {
        val subir = eventosParaSubir(listOf(llenado(1, "2026-08-26T23:59"), llenado(2, "2026-08-27T00:00")), emptySet(), desde)
        assertEquals(listOf(id(2)), subir.map { it.id })
    }

    @Test fun noSubeIdsQueNoSonUuid() {
        val raro = llenado(1).copy(id = "no-es-un-uuid")
        assertTrue(eventosParaSubir(listOf(raro), emptySet(), desde).isEmpty())
    }

    @Test fun conMuchosEventosSubePrimeroLosMasRecientes() {
        val eventos = (0 until 250).map { llenado(it, "2026-11-20T%02d:%02d".format(it / 60, it % 60)) }
        val subir = eventosParaSubir(eventos, emptySet(), desde)
        assertEquals(MAX_EVENTOS_POR_ENVIO, subir.size)
        assertTrue(subir.any { it.id == id(249) })
        assertTrue(subir.none { it.id == id(0) })
        assertEquals(subir.sortedBy { it.momento }, subir)
    }

    @Test fun guardaSoloLosEventosRemotosQueFaltanEnElTelefono() {
        val nuevos = eventosParaGuardar(listOf(llenado(1), llenado(2)), setOf(id(1)))
        assertEquals(listOf(id(2)), nuevos.map { it.id })
    }

    @Test fun subeElPerfilSoloSiCambioDesdeLaUltimaSincronizacion() {
        assertEquals(perfil, perfilParaSubir(perfil, null))
        assertNull(perfilParaSubir(perfil, perfil))
        val editado = perfil.copy(habitantes = 5)
        assertEquals(editado, perfilParaSubir(editado, perfil))
        assertNull(perfilParaSubir(null, perfil))
    }

    @Test fun adoptaElPerfilDeLaFamiliaSiNoSubioCambios() {
        val remoto = perfil.copy(capacidadLitros = 2500.0)
        assertEquals(remoto, perfilParaAplicar(perfil, remoto, seSubio = false))
        assertNull(perfilParaAplicar(perfil, remoto, seSubio = true))
        assertNull(perfilParaAplicar(perfil, perfil, seSubio = false))
        assertNull(perfilParaAplicar(perfil, null, seSubio = false))
        assertEquals(remoto, perfilParaAplicar(null, remoto, seSubio = false))
    }

    @Test fun aceptaElCodigoEscritoDeVariasFormasYElDelQr() {
        assertEquals("3F9A1C0B7E", normalizarCodigo("3f9a1-c0b7e"))
        assertEquals("3F9A1C0B7E", normalizarCodigo(" 3F9A1 C0B7E "))
        assertEquals("3F9A1C0B7E", normalizarCodigo(PREFIJO_QR + "3F9A1C0B7E"))
        assertNull(normalizarCodigo("3F9A1"))
        assertNull(normalizarCodigo("ZZZZZZZZZZ"))
        assertEquals("3F9A1-C0B7E", formatearCodigo("3F9A1C0B7E"))
    }

    @Test fun elApodoSeRecortaYNoPuedeQuedarVacioNiSerMuyLargo() {
        assertEquals("Mamá", validarApodo("  Mamá "))
        assertThrows(IllegalArgumentException::class.java) { validarApodo("   ") }
        assertThrows(IllegalArgumentException::class.java) { validarApodo("x".repeat(31)) }
    }

    @Test fun elCodigoDeLaLigaEsElMismoParaLaFamiliaYNoRevelaElSecreto() {
        val secreto = "0d7f3c52-9b1e-4f0a-8a3b-2c5d6e7f8a9b"
        assertEquals(tokenLigaDe(secreto), tokenLigaDe(secreto))
        assertNotEquals(secreto, tokenLigaDe(secreto))
        assertNotEquals(tokenLigaDe(secreto), tokenLigaDe("1d7f3c52-9b1e-4f0a-8a3b-2c5d6e7f8a9b"))
    }
}
