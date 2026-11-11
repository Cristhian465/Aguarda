package com.example.aguardapp.feature.represas.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import com.example.aguardapp.feature.mapa.domain.model.Coordenada
import com.example.aguardapp.feature.represas.domain.model.Distrito
import com.example.aguardapp.feature.represas.domain.model.Presa
import com.example.aguardapp.feature.represas.domain.model.RecorridosDeTacna
import com.example.aguardapp.feature.represas.domain.model.UsoPresa

class RepresasDominioTest {

    @Test
    fun reconoceElDistritoComoLoEscribeOpenStreetMap() {
        assertEquals(Distrito.GREGORIO_ALBARRACIN, Distrito.desdeTexto("Coronel Gregorio Albarracín Lanchipa"))
        assertEquals(Distrito.ALTO_DE_LA_ALIANZA, Distrito.desdeTexto("ALTO DE LA ALIANZA"))
        assertEquals(Distrito.PACHIA, Distrito.desdeTexto("Pachia"))
        assertEquals(Distrito.LA_YARADA_LOS_PALOS, Distrito.desdeTexto("La Yarada Los Palos"))
    }

    @Test
    fun tacnaDebeCoincidirCompletoPorqueTambienEsLaRegion() {
        assertEquals(Distrito.TACNA, Distrito.desdeTexto(" Tacna "))
        assertNull(Distrito.desdeTexto("Región Tacna"))
        assertNull(Distrito.desdeTexto("Lima"))
        assertNull(Distrito.desdeTexto(null))
    }

    @Test
    fun todosLosDistritosTienenUnRecorrido() {
        Distrito.entries.forEach { assertTrue(it.name, RecorridosDeTacna.de(it).tramos.isNotEmpty()) }
    }

    @Test
    fun ciudadNuevaRecibeAguaDeLasRepresasDelUchusuma() {
        val recorrido = RecorridosDeTacna.de(Distrito.CIUDAD_NUEVA)

        assertTrue(recorrido.abasteceDesde("PAUCARANI"))
        assertTrue(recorrido.abasteceDesde("Casiri"))
        assertFalse(recorrido.abasteceDesde("Jarumas"))
        assertTrue(recorrido.confirmado)
    }

    @Test
    fun gregorioAlbarracinSeAbasteceDePozosYNoDeRepresas() {
        val recorrido = RecorridosDeTacna.de(Distrito.GREGORIO_ALBARRACIN)

        assertTrue(recorrido.presas.isEmpty())
        assertTrue(recorrido.confirmado)
    }

    @Test
    fun elCercadoQuedaPorConfirmar() {
        assertFalse(RecorridosDeTacna.de(Distrito.TACNA).confirmado)
    }

    @Test
    fun calculaCuantosTanquesCabenEnUnaRepresa() {
        val paucarani = presa(volumenUtilHm3 = 8.5)

        assertEquals(7_727_272.7, paucarani.tanquesEquivalentes(1_100.0)!!, 0.1)
        assertNull(presa(volumenUtilHm3 = null).tanquesEquivalentes(1_100.0))
        assertNull(paucarani.tanquesEquivalentes(0.0))
    }

    @Test
    fun elAguaPotableDefineElColorAunqueTambienSeaDeRiego() {
        assertEquals(UsoPresa.AGUA_POTABLE, presa(usos = setOf(UsoPresa.RIEGO, UsoPresa.AGUA_POTABLE)).usoPrincipal)
        assertEquals(UsoPresa.OTRO, presa(usos = emptySet()).usoPrincipal)
    }

    private fun presa(volumenUtilHm3: Double? = 1.0, usos: Set<UsoPresa> = setOf(UsoPresa.RIEGO)) = Presa(
        nombre = "Prueba",
        ubicacion = Coordenada(-17.5, -69.8),
        rio = null,
        distrito = null,
        volumenUtilHm3 = volumenUtilHm3,
        alturaMetros = null,
        altitudMetros = null,
        anioInicio = null,
        usos = usos,
        operador = null
    )
}
