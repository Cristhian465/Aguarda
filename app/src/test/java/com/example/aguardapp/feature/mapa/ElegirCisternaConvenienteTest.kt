package com.example.aguardapp.feature.mapa

import com.example.aguardapp.feature.mapa.domain.model.Coordenada
import com.example.aguardapp.feature.mapa.domain.model.EstadoCisterna
import com.example.aguardapp.feature.mapa.domain.model.PuntoCisterna
import com.example.aguardapp.feature.mapa.domain.usecase.CandidataCisterna
import com.example.aguardapp.feature.mapa.domain.usecase.ElegirCisternaConveniente
import kotlinx.datetime.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ElegirCisternaConvenienteTest {

    private val elegir = ElegirCisternaConveniente()

    private fun candidata(
        id: String,
        distanciaKm: Double?,
        inicio: Int = 7,
        fin: Int = 13,
        estado: EstadoCisterna = EstadoCisterna.ACTIVO
    ) = CandidataCisterna(
        PuntoCisterna(id, id, Coordenada(-18.0, -70.25), LocalTime(inicio, 0), LocalTime(fin, 0), estado),
        distanciaKm
    )

    @Test
    fun eligeLaMasCercanaQueSigueRepartiendo() {
        val elegida = elegir.elegir(listOf(candidata("lejos", 6.0), candidata("cerca", 2.0)), LocalTime(9, 0))
        assertEquals("cerca", elegida?.punto?.id)
    }

    @Test
    fun descartaLasTerminadasAunqueEstenMasCerca() {
        val candidatas = listOf(candidata("terminada", 1.0, estado = EstadoCisterna.TERMINADO), candidata("activa", 4.0))
        assertEquals("activa", elegir.elegir(candidatas, LocalTime(9, 0))?.punto?.id)
    }

    @Test
    fun descartaLasQueYaPasaronSuHoraDeFin() {
        val candidatas = listOf(candidata("cerro", 1.0, fin = 10), candidata("abierta", 3.0, fin = 14))
        assertEquals("abierta", elegir.elegir(candidatas, LocalTime(11, 0))?.punto?.id)
    }

    @Test
    fun aceptaLasQueAunNoEmpiezan() {
        val enRuta = candidata("en-ruta", 2.0, inicio = 15, fin = 18, estado = EstadoCisterna.EN_RUTA)
        assertEquals("en-ruta", elegir.elegir(listOf(enRuta), LocalTime(9, 0))?.punto?.id)
    }

    @Test
    fun sinDistanciasRespetaElOrdenRecibido() {
        val candidatas = listOf(candidata("primera", null), candidata("segunda", null))
        assertEquals("primera", elegir.elegir(candidatas, LocalTime(9, 0))?.punto?.id)
    }

    @Test
    fun siTodasCerraronNoEligeNinguna() {
        val candidatas = listOf(candidata("a", 1.0, fin = 10), candidata("b", 2.0, estado = EstadoCisterna.TERMINADO))
        assertNull(elegir.elegir(candidatas, LocalTime(12, 0)))
    }

    @Test
    fun laHoraDeFinYaNoCuentaComoAbierta() {
        assertNull(elegir.elegir(listOf(candidata("justo", 1.0, fin = 13)), LocalTime(13, 0)))
    }
}
