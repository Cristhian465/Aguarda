package com.example.aguardapp.feature.mapa

import java.io.IOException
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalTime
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response
import com.example.aguardapp.feature.mapa.data.CopiaCisternas
import com.example.aguardapp.feature.mapa.data.MapaRepositoryImpl
import com.example.aguardapp.feature.mapa.data.aPuntos
import com.example.aguardapp.feature.mapa.data.remote.CisternasApi
import com.example.aguardapp.feature.mapa.data.remote.PuntoCisternaDto
import com.example.aguardapp.feature.mapa.domain.model.EstadoCisterna
import com.example.aguardapp.feature.mapa.presentation.MapaUiState
import com.example.aguardapp.feature.mapa.presentation.avisoSinPuntos
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CisternasSupabaseTest {
    private fun fila(
        id: String? = "PC-01",
        estado: String? = "ACTIVO",
        latitud: Double? = -18.0495,
        inicio: String? = "07:00:00"
    ) = PuntoCisternaDto(id, "Viñani · Asoc. La Esperanza", latitud, -70.2548, inicio, "13:00:00", estado)

    private class CopiaEnMemoria(var json: String? = null) : CopiaCisternas {
        override fun leer(): String? = json
        override fun guardar(json: String) {
            this.json = json
        }
    }

    private class ApiDePrueba(private val respuesta: () -> Response<List<PuntoCisternaDto>>) : CisternasApi {
        override suspend fun puntos(clave: String, autorizacion: String) = respuesta()
    }

    @Test
    fun convierteUnaFilaDeSupabase() {
        val punto = aPuntos(listOf(fila())).single()
        assertEquals("PC-01", punto.id)
        assertEquals(LocalTime(7, 0), punto.horarioInicio)
        assertEquals(LocalTime(13, 0), punto.horarioFin)
        assertEquals(EstadoCisterna.ACTIVO, punto.estado)
        assertEquals(-18.0495, punto.ubicacion.latitud, 0.0)
    }

    @Test
    fun aceptaElEstadoEnMinusculasOConEspacio() {
        assertEquals(EstadoCisterna.EN_RUTA, aPuntos(listOf(fila(estado = "en ruta"))).single().estado)
    }

    @Test
    fun descartaLasFilasIncompletasOInvalidas() {
        val filas = listOf(
            fila(id = null), fila(id = " "), fila(estado = "PERDIDO"),
            fila(latitud = 120.0), fila(inicio = "7 am"), fila(id = "PC-09")
        )
        assertEquals(listOf("PC-09"), aPuntos(filas).map { it.id })
    }

    @Test
    fun guardaUnaCopiaCuandoDescarga() = runBlocking {
        val copia = CopiaEnMemoria()
        val puntos = MapaRepositoryImpl(ApiDePrueba { Response.success(listOf(fila())) }, copia, "clave").obtenerPuntosCisterna()
        assertEquals(1, puntos.size)
        assertTrue(copia.json!!.contains("PC-01"))
    }

    @Test
    fun sinInternetUsaLaUltimaCopia() = runBlocking {
        val copia = CopiaEnMemoria()
        MapaRepositoryImpl(ApiDePrueba { Response.success(listOf(fila())) }, copia, "clave").obtenerPuntosCisterna()
        val sinRed = MapaRepositoryImpl(ApiDePrueba { throw IOException("sin red") }, copia, "clave")
        assertEquals(listOf("PC-01"), sinRed.obtenerPuntosCisterna().map { it.id })
    }

    @Test
    fun siSupabaseFallaUsaLaUltimaCopia() = runBlocking {
        val copia = CopiaEnMemoria()
        MapaRepositoryImpl(ApiDePrueba { Response.success(listOf(fila())) }, copia, "clave").obtenerPuntosCisterna()
        val conError = MapaRepositoryImpl(ApiDePrueba { Response.error(500, "".toResponseBody()) }, copia, "clave")
        assertEquals(1, conError.obtenerPuntosCisterna().size)
    }

    @Test(expected = IOException::class)
    fun sinInternetNiCopiaAvisaElError(): Unit = runBlocking {
        MapaRepositoryImpl(ApiDePrueba { throw IOException("sin red") }, CopiaEnMemoria(), "clave").obtenerPuntosCisterna()
    }

    @Test
    fun unaListaVaciaDeSupabaseSeRespeta() = runBlocking {
        val copia = CopiaEnMemoria()
        MapaRepositoryImpl(ApiDePrueba { Response.success(listOf(fila())) }, copia, "clave").obtenerPuntosCisterna()
        val vacia = MapaRepositoryImpl(ApiDePrueba { Response.success(emptyList()) }, copia, "clave")
        assertTrue(vacia.obtenerPuntosCisterna().isEmpty())
    }

    @Test
    fun avisoCuandoNoHayPuntos() {
        assertTrue(avisoSinPuntos(MapaUiState(sinConexion = true))!!.startsWith("Sin conexión"))
        assertTrue(avisoSinPuntos(MapaUiState())!!.contains("no hay puntos"))
        assertNull(avisoSinPuntos(MapaUiState(puntos = aPuntos(listOf(fila())))))
    }
}
