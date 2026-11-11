package com.example.aguardapp.feature.represas.data

import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDateTime
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.example.aguardapp.feature.mapa.domain.model.Coordenada
import com.example.aguardapp.feature.represas.data.local.DistritoHogarEntity
import com.example.aguardapp.feature.represas.data.local.PresaEntity
import com.example.aguardapp.feature.represas.data.local.RepresasDao
import com.example.aguardapp.feature.represas.data.remote.AnaApi
import com.example.aguardapp.feature.represas.data.remote.NominatimApi
import com.example.aguardapp.feature.represas.data.remote.URL_ANA
import com.example.aguardapp.feature.represas.data.remote.URL_NOMINATIM
import com.example.aguardapp.feature.represas.domain.model.Distrito
import com.example.aguardapp.feature.represas.domain.model.DistritoDelHogar
import com.example.aguardapp.feature.represas.domain.model.UsoPresa

/**
 * Pasa por Retrofit y Gson de verdad, pero sin red: un interceptor responde con las respuestas reales
 * de la ANA y de Nominatim guardadas en src/test/resources (descargadas el 11/11/2026).
 */
class RepresasRepositoryImplTest {

    private val ahora = LocalDateTime(2026, 11, 11, 12, 0)
    private val dao = DaoEnMemoria()
    private val pedidos = mutableListOf<HttpUrl>()
    private var responder: (Request) -> Response = { error("La prueba no preparó una respuesta") }

    private val cliente = OkHttpClient.Builder()
        .addInterceptor { cadena -> cadena.request().also { pedidos += it.url }.let(responder) }
        .build()

    private val repositorio = RepresasRepositoryImpl(
        api(URL_ANA, AnaApi::class.java),
        api(URL_NOMINATIM, NominatimApi::class.java),
        dao,
        USUARIO
    ) { ahora }

    @Test
    fun descargaLasOchoPresasDeTacnaYLimpiaSusDatos() = runBlocking {
        responder = { json(it, recurso("ana_presas_tacna.json")) }

        assertTrue(repositorio.actualizarPresas().isSuccess)

        val presas = repositorio.observarPresas().first()
        assertEquals(8, presas.size)
        val paucarani = presas.single { it.nombre == "Paucarani" }
        assertEquals(8.5, paucarani.volumenUtilHm3!!, 0.0)
        assertEquals("Palca", paucarani.distrito)  // la ANA lo envía como "Palca "
        assertEquals(2003, paucarani.anioInicio)
        assertEquals(-17.534, paucarani.ubicacion.latitud, 0.001)
        assertEquals(setOf(UsoPresa.RIEGO, UsoPresa.AGUA_POTABLE), paucarani.usos)  // "Suminsitro", con errata
        val diqueCano = presas.single { it.nombre == "Dique Cano" }
        assertNull(diqueCano.volumenUtilHm3)  // "No disponible"
        assertNull(diqueCano.anioInicio)
        assertEquals(UsoPresa.MINERIA, presas.single { it.nombre == "Pampa de Vaca" }.usoPrincipal)
    }

    @Test
    fun consultaSoloLasPresasDeTacnaEnLatitudYLongitud() = runBlocking {
        responder = { json(it, recurso("ana_presas_tacna.json")) }

        repositorio.actualizarPresas()

        val url = pedidos.single()
        assertEquals("/server/rest/services/ONRH/Inventario_de_Presas/MapServer/0/query", url.encodedPath)
        assertEquals("DEPARTAMEN='TACNA'", url.queryParameter("where"))
        assertEquals("4326", url.queryParameter("outSR"))
        assertEquals("json", url.queryParameter("f"))
    }

    @Test
    fun noVuelveADescargarDuranteUnaSemanaSalvoQueSeFuerce() = runBlocking {
        responder = { json(it, recurso("ana_presas_tacna.json")) }

        repositorio.actualizarPresas()
        repositorio.actualizarPresas()
        assertEquals(1, pedidos.size)

        repositorio.actualizarPresas(forzar = true)
        assertEquals(2, pedidos.size)
    }

    @Test
    fun unErrorDeArcGisLlegaConHttp200YNoBorraLoGuardado() = runBlocking {
        responder = { json(it, recurso("ana_presas_tacna.json")) }
        repositorio.actualizarPresas()
        responder = { json(it, """{"error":{"code":400,"message":"Failed to execute query.","details":[]}}""") }

        val resultado = repositorio.actualizarPresas(forzar = true)

        assertEquals("La ANA rechazó la consulta (Failed to execute query.)", resultado.exceptionOrNull()?.message)
        assertEquals(8, repositorio.observarPresas().first().size)
    }

    @Test
    fun sinInternetDevuelveUnMensajeClaro() = runBlocking {
        responder = { throw IOException("Sin red") }

        assertEquals("Sin conexión con la ANA", repositorio.actualizarPresas().exceptionOrNull()?.message)
    }

    @Test
    fun unErrorHttpIncluyeSuCodigo() = runBlocking {
        responder = { json(it, "", codigo = 503) }

        assertEquals("La ANA respondió con error 503", repositorio.actualizarPresas().exceptionOrNull()?.message)
    }

    @Test
    fun detectaGregorioAlbarracinConLaDireccionDeNominatim() = runBlocking {
        responder = { json(it, recurso("nominatim_gregorio_albarracin.json")) }

        val resultado = repositorio.detectarDistrito(Coordenada(-18.045, -70.255))

        assertEquals(Distrito.GREGORIO_ALBARRACIN, resultado.getOrNull())
        assertEquals(DistritoDelHogar(Distrito.GREGORIO_ALBARRACIN, detectadoPorGps = true), repositorio.observarDistrito().first())
        assertEquals("/reverse", pedidos.single().encodedPath)
        assertEquals("-18.045", pedidos.single().queryParameter("lat"))
    }

    @Test
    fun fueraDeTacnaNoGuardaNingunDistrito() = runBlocking {
        responder = { json(it, """{"address":{"city":"Lima","state":"Lima","country":"Perú"}}""") }

        val resultado = repositorio.detectarDistrito(Coordenada(-12.046, -77.043))

        assertTrue(resultado.isFailure)
        assertNull(repositorio.observarDistrito().first())
    }

    @Test
    fun elDistritoElegidoDeLaListaNoSeMarcaComoDetectadoPorGps() = runBlocking {
        repositorio.elegirDistrito(Distrito.CIUDAD_NUEVA)

        assertEquals(DistritoDelHogar(Distrito.CIUDAD_NUEVA, detectadoPorGps = false), repositorio.observarDistrito().first())
    }

    private fun <T> api(urlBase: String, tipo: Class<T>): T =
        Retrofit.Builder().baseUrl(urlBase).client(cliente).addConverterFactory(GsonConverterFactory.create()).build().create(tipo)

    private fun json(pedido: Request, cuerpo: String, codigo: Int = 200): Response =
        Response.Builder()
            .request(pedido)
            .protocol(Protocol.HTTP_1_1)
            .code(codigo)
            .message(if (codigo == 200) "OK" else "Error")
            .body(cuerpo.toResponseBody("application/json".toMediaType()))
            .build()

    private fun recurso(nombre: String): String = requireNotNull(javaClass.classLoader?.getResource(nombre)).readText()

    private companion object {
        const val USUARIO = "usuario-de-prueba"
    }
}

/** Reemplaza a Room en las pruebas: guarda todo en memoria. */
private class DaoEnMemoria : RepresasDao() {
    private val presas = MutableStateFlow<List<PresaEntity>>(emptyList())
    private val distritos = MutableStateFlow<Map<String, DistritoHogarEntity>>(emptyMap())

    override fun observarPresas(): Flow<List<PresaEntity>> = presas
    override fun observarDescarga(): Flow<String?> = presas.map { lista -> lista.maxOfOrNull { it.descargadaEn } }
    override suspend fun fechaDeDescarga(): String? = presas.value.maxOfOrNull { it.descargadaEn }
    override suspend fun borrarPresas() { presas.value = emptyList() }
    override suspend fun guardarPresas(presas: List<PresaEntity>) { this.presas.value += presas }
    override fun observarDistrito(usuarioId: String): Flow<DistritoHogarEntity?> = distritos.map { it[usuarioId] }
    override suspend fun guardarDistrito(distrito: DistritoHogarEntity) { distritos.value += distrito.usuarioId to distrito }
}
