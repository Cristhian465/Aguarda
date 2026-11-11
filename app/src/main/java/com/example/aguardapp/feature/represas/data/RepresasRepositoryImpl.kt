package com.example.aguardapp.feature.represas.data

import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDateTime
import com.google.gson.JsonParseException
import retrofit2.HttpException
import com.example.aguardapp.feature.deposito.domain.model.masHoras
import com.example.aguardapp.feature.mapa.domain.model.Coordenada
import com.example.aguardapp.feature.represas.data.local.DistritoHogarEntity
import com.example.aguardapp.feature.represas.data.local.RepresasDao
import com.example.aguardapp.feature.represas.data.mapper.aDominio
import com.example.aguardapp.feature.represas.data.mapper.aEntidad
import com.example.aguardapp.feature.represas.data.mapper.distrito
import com.example.aguardapp.feature.represas.data.remote.AnaApi
import com.example.aguardapp.feature.represas.data.remote.NominatimApi
import com.example.aguardapp.feature.represas.domain.model.Distrito
import com.example.aguardapp.feature.represas.domain.model.DistritoDelHogar
import com.example.aguardapp.feature.represas.domain.model.Presa
import com.example.aguardapp.feature.represas.domain.repository.RepresasRepository

// El inventario de presas casi no cambia: basta con renovarlo una vez por semana.
private const val HORAS_DE_VIGENCIA = 7 * 24.0

/** Room es la fuente de verdad: la pantalla siempre lee lo guardado, y la red solo lo renueva. */
class RepresasRepositoryImpl(
    private val ana: AnaApi,
    private val nominatim: NominatimApi,
    private val dao: RepresasDao,
    private val usuarioId: String,
    private val ahora: () -> LocalDateTime
) : RepresasRepository {

    override fun observarPresas(): Flow<List<Presa>> =
        dao.observarPresas().map { presas -> presas.map { it.aDominio() } }

    override fun observarDescarga(): Flow<LocalDateTime?> =
        dao.observarDescarga().map { it?.let(LocalDateTime::parse) }

    override suspend fun actualizarPresas(forzar: Boolean): Result<Unit> {
        val ultima = dao.fechaDeDescarga()?.let(LocalDateTime::parse)
        if (!forzar && ultima != null && ahora() < ultima.masHoras(HORAS_DE_VIGENCIA)) return Result.success(Unit)
        return consultar("la ANA") {
            val respuesta = ana.presas()
            respuesta.error?.let { throw RespuestaInvalida("La ANA rechazó la consulta (${it.mensaje ?: it.codigo})") }
            val presas = respuesta.presas.orEmpty().mapNotNull { it.aDominio() }
            if (presas.isEmpty()) throw RespuestaInvalida("La ANA no devolvió represas de Tacna")
            val momento = ahora().toString()
            dao.reemplazarPresas(presas.map { it.aEntidad(momento) })
        }
    }

    override fun observarDistrito(): Flow<DistritoDelHogar?> =
        dao.observarDistrito(usuarioId).map { it?.aDominio() }

    override suspend fun elegirDistrito(distrito: Distrito) = guardarDistrito(distrito, detectadoPorGps = false)

    override suspend fun detectarDistrito(ubicacion: Coordenada): Result<Distrito> {
        val direccion = consultar("el buscador de direcciones") {
            nominatim.direccion(ubicacion.latitud, ubicacion.longitud)
        }.getOrElse { return Result.failure(it) }
        val distrito = direccion.distrito()
            ?: return Result.failure(IllegalStateException("Tu ubicación no está en un distrito de Tacna que conozcamos. Elige tu distrito de la lista."))
        guardarDistrito(distrito, detectadoPorGps = true)
        return Result.success(distrito)
    }

    private suspend fun guardarDistrito(distrito: Distrito, detectadoPorGps: Boolean) =
        dao.guardarDistrito(DistritoHogarEntity(usuarioId, distrito.name, detectadoPorGps))
}

private class RespuestaInvalida(mensaje: String) : Exception(mensaje)

// Convierte los fallos de red en mensajes para el usuario. Salir de la pantalla cancela la consulta: eso no es un error.
private suspend fun <T> consultar(servicio: String, llamada: suspend () -> T): Result<T> {
    val alInicio = servicio.replaceFirstChar { it.uppercase() }
    return try {
        Result.success(llamada())
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        Result.failure(IllegalStateException("Sin conexión con $servicio", e))
    } catch (e: HttpException) {
        Result.failure(IllegalStateException("$alInicio respondió con error ${e.code()}", e))
    } catch (e: RespuestaInvalida) {
        Result.failure(IllegalStateException(e.message, e))
    } catch (e: JsonParseException) {
        Result.failure(IllegalStateException("$alInicio envió datos que no se pudieron leer", e))
    }
}
