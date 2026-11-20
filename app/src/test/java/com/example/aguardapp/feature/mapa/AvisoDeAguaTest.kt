package com.example.aguardapp.feature.mapa

import com.example.aguardapp.feature.mapa.domain.model.Coordenada
import com.example.aguardapp.feature.mapa.domain.model.EstadoCisterna
import com.example.aguardapp.feature.mapa.domain.model.PuntoCisterna
import com.example.aguardapp.feature.mapa.domain.usecase.CandidataCisterna
import com.example.aguardapp.feature.mapa.presentation.armarAvisoDeAgua
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AvisoDeAguaTest {
    private val ahora = LocalDateTime(2026, 11, 12, 9, 0)
    private val proximoLlenado = LocalDateTime(2026, 11, 13, 5, 0)

    private fun candidata(nombre: String, distanciaKm: Double?, inicio: Int = 7, fin: Int = 13) = CandidataCisterna(
        PuntoCisterna(nombre, nombre, Coordenada(-18.0, -70.25), LocalTime(inicio, 0), LocalTime(fin, 0), EstadoCisterna.ACTIVO),
        distanciaKm
    )

    @Test
    fun sinDepositoNoHayAviso() {
        assertNull(armarAvisoDeAgua(null, proximoLlenado, ahora, listOf(candidata("Viñani", 3.8))))
    }

    @Test
    fun siElAguaSeAcabaAntesDelLlenadoMandaALaCisternaMasCercanaAbierta() {
        val agotamiento = LocalDateTime(2026, 11, 12, 15, 10)
        val aviso = armarAvisoDeAgua(agotamiento, proximoLlenado, ahora, listOf(candidata("Viñani", 3.8)))!!
        assertTrue(aviso.faltaAgua)
        assertEquals("Tu agua se acaba hoy 3:10 p.m.", aviso.titulo)
        assertEquals("Ve a Viñani (a 3.8 km): reparte hasta la 1:00 p.m.", aviso.detalle)
        assertEquals("Viñani", aviso.sugeridaId)
    }

    @Test
    fun siLaCisternaAunNoEmpiezaDaSuHorarioCompleto() {
        val agotamiento = LocalDateTime(2026, 11, 12, 15, 10)
        val aviso = armarAvisoDeAgua(agotamiento, proximoLlenado, ahora, listOf(candidata("Plaza", null, inicio = 14, fin = 18)))!!
        assertEquals("Ve a Plaza: reparte hoy de 2:00 p.m. a 6:00 p.m.", aviso.detalle)
    }

    @Test
    fun siAlcanzaLoDiceSinAlarmar() {
        val agotamiento = LocalDateTime(2026, 11, 13, 11, 0)
        val aviso = armarAvisoDeAgua(agotamiento, proximoLlenado, ahora, listOf(candidata("Viñani", 3.8)))!!
        assertFalse(aviso.faltaAgua)
        assertEquals("Tu agua alcanza hasta el próximo llenado", aviso.titulo)
    }

    @Test
    fun siTodasCerraronLoAvisaYNoSugiereNinguna() {
        val agotamiento = LocalDateTime(2026, 11, 12, 15, 10)
        val tarde = LocalDateTime(2026, 11, 12, 14, 0)
        val aviso = armarAvisoDeAgua(agotamiento, proximoLlenado, tarde, listOf(candidata("Viñani", 3.8)))!!
        assertNull(aviso.sugeridaId)
        assertTrue(aviso.detalle.startsWith("Hoy ya no quedan cisternas repartiendo"))
    }

    @Test
    fun conElTanqueVacioLoDiceClaro() {
        val agotamiento = LocalDateTime(2026, 11, 12, 8, 0)
        val aviso = armarAvisoDeAgua(agotamiento, proximoLlenado, ahora, listOf(candidata("Viñani", 3.8)))!!
        assertEquals("Tu tanque ya está vacío", aviso.titulo)
    }
}
