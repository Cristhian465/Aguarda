package com.example.aguardapp.feature.liga

import com.example.aguardapp.feature.liga.domain.Duelo
import com.example.aguardapp.feature.liga.domain.FilaRanking
import com.example.aguardapp.feature.liga.domain.PuntajeHogar
import com.example.aguardapp.feature.liga.domain.ResumenSemanal
import com.example.aguardapp.feature.liga.presentation.Bronce
import com.example.aguardapp.feature.liga.presentation.GrisLiga
import com.example.aguardapp.feature.liga.presentation.LigaUiState
import com.example.aguardapp.feature.liga.presentation.Oro
import com.example.aguardapp.feature.liga.presentation.Plata
import com.example.aguardapp.feature.liga.presentation.RankingLigaUiState
import com.example.aguardapp.feature.liga.presentation.SeccionLiga
import com.example.aguardapp.feature.liga.presentation.colorDeMedalla
import com.example.aguardapp.feature.liga.presentation.detalleDe
import com.example.aguardapp.feature.liga.presentation.detalleDeConsumo
import com.example.aguardapp.feature.liga.presentation.detalleDeDuelos
import com.example.aguardapp.feature.liga.presentation.detalleDeMiDistrito
import com.example.aguardapp.feature.liga.presentation.detalleDePuntaje
import com.example.aguardapp.feature.liga.presentation.fraccionDeHogares
import com.example.aguardapp.feature.liga.presentation.fraccionDelDuelo
import com.example.aguardapp.feature.liga.presentation.fraccionDelPuntaje
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class MenuLigaTest {
    private fun fila(ubigeo: String, hogares: Int, puntaje: Double?, puesto: Int?) =
        FilaRanking(puesto, ubigeo, "Distrito $ubigeo", hogares, 0, puntaje)

    private fun resumen(litros: Double?, total: Int?) = ResumenSemanal(
        LocalDate(2026, 11, 16), litros, null, 0, 0, total?.let { PuntajeHogar(it, 0, 0, 0, null) }
    )

    @Test
    fun lasTresPrimerasPosicionesTienenMedalla() {
        assertEquals(Oro, colorDeMedalla(1))
        assertEquals(Plata, colorDeMedalla(2))
        assertEquals(Bronce, colorDeMedalla(3))
        assertEquals(GrisLiga, colorDeMedalla(4))
        assertEquals(GrisLiga, colorDeMedalla(null))
    }

    @Test
    fun elDetalleDelConsumoYDelPuntajeDicenSiHayMedicion() {
        assertEquals("89 L por persona al día", detalleDeConsumo(resumen(89.4, 6)))
        assertEquals("Aún sin medición", detalleDeConsumo(resumen(null, null)))
        assertEquals("6 de 100", detalleDePuntaje(resumen(89.4, 6)))
        assertEquals("Aún sin puntaje", detalleDePuntaje(null))
    }

    @Test
    fun elDetalleDeMiDistritoDiceElPuestoOLoQueFalta() {
        val filas = listOf(fila("230102", 8, 74.0, 1), fila("230110", 9, 61.0, 2), fila("230101", 3, null, null), fila("230104", 0, null, null))
        assertEquals("Tu distrito va 2º", detalleDeMiDistrito(filas, "230110"))
        assertEquals("Faltan 2 hogares", detalleDeMiDistrito(filas, "230101"))
        assertEquals("Tu distrito aún no tiene hogares", detalleDeMiDistrito(filas, "230104"))
        assertEquals("Distritos de Tacna", detalleDeMiDistrito(filas, null))
    }

    @Test
    fun elDetalleDeLosDuelosCuentaLosEnfrentamientos() {
        val duelo = Duelo(fila("230101", 9, 55.0, 4), fila("230110", 9, 61.0, 3))
        assertEquals("Aún sin duelos", detalleDeDuelos(emptyList()))
        assertEquals("1 duelo esta semana", detalleDeDuelos(listOf(duelo)))
        assertEquals("2 duelos esta semana", detalleDeDuelos(listOf(duelo, duelo)))
    }

    @Test
    fun siNoParticipaElMenuInvitaAParticiparEnVezDeMostrarDatos() {
        val estado = LigaUiState()
        val sinParticipar = RankingLigaUiState(participa = false)
        assertEquals("Representa a tu distrito", detalleDe(SeccionLiga.RANKING, estado, sinParticipar))
        assertEquals("Participa para verlos", detalleDe(SeccionLiga.DUELOS, estado, sinParticipar))
        assertEquals("Marca cuánta agua tienes", detalleDe(SeccionLiga.TANQUE, estado, sinParticipar))
    }

    @Test
    fun lasBarrasSonProporcionales() {
        assertEquals(0.5f, fraccionDelDuelo(50.0, 50.0), 0.001f)
        assertEquals(0.75f, fraccionDelDuelo(75.0, 25.0), 0.001f)
        assertEquals(0.5f, fraccionDelDuelo(0.0, 0.0), 0.001f)
        assertEquals(0.61f, fraccionDelPuntaje(61.0), 0.001f)
        assertEquals(1f, fraccionDelPuntaje(140.0), 0.001f)
        assertEquals(0.6f, fraccionDeHogares(3), 0.001f)
        assertEquals(1f, fraccionDeHogares(9), 0.001f)
    }

    @Test
    fun todasLasSeccionesTienenTituloEIcono() {
        assertEquals(5, SeccionLiga.entries.size)
        SeccionLiga.entries.forEach { assert(it.titulo.isNotBlank() && it.icono != 0) }
    }
}
