package com.example.aguardapp.feature.mapa

import com.example.aguardapp.feature.mapa.data.leerDistritos
import com.example.aguardapp.feature.mapa.domain.model.Coordenada
import com.example.aguardapp.feature.mapa.domain.model.Distrito
import com.example.aguardapp.feature.mapa.domain.model.distritoDe
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DistritosTest {
    private val distritos = leerDistritos(File("src/main/assets/distritos_tacna.geojson").readText())

    private val cuadrado = Distrito(
        "000000", "Cuadrado",
        listOf(listOf(Coordenada(0.0, 0.0), Coordenada(0.0, 2.0), Coordenada(2.0, 2.0), Coordenada(2.0, 0.0), Coordenada(0.0, 0.0)))
    )

    @Test
    fun cargaLosCincoDistritosConSuUbigeo() {
        assertEquals(listOf("230101", "230102", "230104", "230108", "230110"), distritos.map { it.ubigeo }.sorted())
    }

    @Test
    fun todosLosBordesEstanCerrados() {
        distritos.flatMap { it.poligonos }.forEach { assertEquals(it.first(), it.last()) }
    }

    @Test
    fun laPlazaDeArmasEstaEnTacna() {
        assertEquals("230101", distritos.distritoDe(Coordenada(-18.0137, -70.2497))?.ubigeo)
    }

    @Test
    fun ubicaPuntosDeOtrosDistritos() {
        assertEquals("230110", distritos.distritoDe(Coordenada(-18.0495, -70.2548))?.ubigeo)
        assertEquals("230102", distritos.distritoDe(Coordenada(-17.9915, -70.2535))?.ubigeo)
        assertEquals("230104", distritos.distritoDe(Coordenada(-17.9845, -70.2365))?.ubigeo)
    }

    @Test
    fun unPuntoLejosDeTacnaNoTieneDistrito() {
        assertNull(distritos.distritoDe(Coordenada(-12.0464, -77.0428)))
    }

    @Test
    fun distingueDentroYFueraDelBorde() {
        assertTrue(cuadrado.contiene(Coordenada(1.0, 1.0)))
        assertFalse(cuadrado.contiene(Coordenada(3.0, 1.0)))
    }
}
