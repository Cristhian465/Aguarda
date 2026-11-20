package com.example.aguardapp.feature.mapa

import com.example.aguardapp.feature.mapa.data.leerDistritos
import com.example.aguardapp.feature.mapa.presentation.colorDelDistrito
import com.example.aguardapp.feature.mapa.presentation.nombreCorto
import com.example.aguardapp.feature.mapa.presentation.opacidadDelDistrito
import com.example.aguardapp.feature.mapa.presentation.opacidadDelPuntaje
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ColoresDeDistritoTest {
    private val distritos = leerDistritos(File("src/main/assets/distritos_tacna.geojson").readText())

    @Test
    fun cadaDistritoTieneSuPropioColor() {
        val colores = distritos.map { colorDelDistrito(it.ubigeo) }
        assertEquals(distritos.size, colores.toSet().size)
    }

    @Test
    fun unDistritoDesconocidoNoUsaElColorDeOtro() {
        distritos.forEach { assertNotEquals(colorDelDistrito("999999"), colorDelDistrito(it.ubigeo)) }
    }

    @Test
    fun unDistritoQueCompiteSeVeMasFuerteQueUnoSinPuntaje() {
        assertTrue(opacidadDelDistrito(0.0, false) > opacidadDelDistrito(null, false))
        assertTrue(opacidadDelPuntaje(80.0) > opacidadDelPuntaje(20.0))
    }

    @Test
    fun elDistritoElegidoSinPuntajeSeResalta() {
        assertTrue(opacidadDelDistrito(null, true) > opacidadDelDistrito(null, false))
    }

    @Test
    fun elNombreLargoSeAcortaEnLaLeyenda() {
        assertEquals("Gregorio Albarracín", nombreCorto("Coronel Gregorio Albarracín Lanchipa"))
        assertEquals("Pocollay", nombreCorto("Pocollay"))
    }
}
