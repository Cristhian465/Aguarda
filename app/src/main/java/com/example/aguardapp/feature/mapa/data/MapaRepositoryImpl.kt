package com.example.aguardapp.feature.mapa.data

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.datetime.LocalTime
import com.example.aguardapp.feature.liga.data.remote.CLAVE_SUPABASE
import com.example.aguardapp.feature.mapa.data.remote.CisternasApi
import com.example.aguardapp.feature.mapa.data.remote.PuntoCisternaDto
import com.example.aguardapp.feature.mapa.domain.model.Coordenada
import com.example.aguardapp.feature.mapa.domain.model.EstadoCisterna
import com.example.aguardapp.feature.mapa.domain.model.PuntoCisterna
import com.example.aguardapp.feature.mapa.domain.repository.MapaRepository

class MapaRepositoryImpl(
    private val api: CisternasApi,
    private val copia: CopiaCisternas,
    private val clave: String = CLAVE_SUPABASE
) : MapaRepository {
    private val gson = Gson()
    private val tipoLista = object : TypeToken<List<PuntoCisternaDto>>() {}.type

    override fun centroDelMapa(): Coordenada = CENTRO_TACNA

    override suspend fun obtenerPuntosCisterna(): List<PuntoCisterna> {
        val descargados = try {
            descargar()
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            null
        } catch (e: RuntimeException) {
            null
        }
        if (descargados != null) {
            copia.guardar(gson.toJson(descargados))
            return aPuntos(descargados)
        }
        val guardados = copia.leer()?.let { runCatching { gson.fromJson<List<PuntoCisternaDto>>(it, tipoLista) }.getOrNull() }
            ?: throw IOException("Sin conexión y sin puntos guardados")
        return aPuntos(guardados)
    }

    private suspend fun descargar(): List<PuntoCisternaDto> {
        val respuesta = api.puntos(clave, "Bearer $clave")
        check(respuesta.isSuccessful) { "Supabase respondió con error ${respuesta.code()}" }
        return respuesta.body().orEmpty()
    }

    private companion object {
        val CENTRO_TACNA = Coordenada(-18.0130, -70.2500)
    }
}

fun aPuntos(filas: List<PuntoCisternaDto>): List<PuntoCisterna> = filas.mapNotNull(::aPunto)

private fun aPunto(fila: PuntoCisternaDto): PuntoCisterna? {
    val id = fila.id?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    val nombre = fila.nombre?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    val latitud = fila.latitud ?: return null
    val longitud = fila.longitud ?: return null
    val inicio = fila.horarioInicio?.trim() ?: return null
    val fin = fila.horarioFin?.trim() ?: return null
    val estado = EstadoCisterna.entries.firstOrNull { it.name == fila.estado?.trim()?.uppercase()?.replace(' ', '_') } ?: return null
    return runCatching {
        PuntoCisterna(id, nombre, Coordenada(latitud, longitud), LocalTime.parse(inicio), LocalTime.parse(fin), estado)
    }.getOrNull()
}
