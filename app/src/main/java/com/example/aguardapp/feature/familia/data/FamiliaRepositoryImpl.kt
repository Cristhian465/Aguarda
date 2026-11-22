package com.example.aguardapp.feature.familia.data

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.IOException
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.LocalDate
import retrofit2.Response
import com.example.aguardapp.feature.familia.data.local.FamiliaDao
import com.example.aguardapp.feature.familia.data.remote.CrearFamiliaDto
import com.example.aguardapp.feature.familia.data.remote.FamiliaApi
import com.example.aguardapp.feature.familia.data.remote.MiembroDto
import com.example.aguardapp.feature.familia.data.remote.PerfilFamiliaDto
import com.example.aguardapp.feature.familia.data.remote.ReiniciarDto
import com.example.aguardapp.feature.familia.data.remote.SincronizarDto
import com.example.aguardapp.feature.familia.data.remote.UnirseDto
import com.example.aguardapp.feature.familia.domain.model.EstadoSincronizacion
import com.example.aguardapp.feature.familia.domain.model.EventoCompartido
import com.example.aguardapp.feature.familia.domain.model.FamiliaExpulsadaException
import com.example.aguardapp.feature.familia.domain.model.Invitacion
import com.example.aguardapp.feature.familia.domain.model.MiembroFamilia
import com.example.aguardapp.feature.familia.domain.model.PerfilCompartido
import com.example.aguardapp.feature.familia.domain.model.TipoEventoFamiliar
import com.example.aguardapp.feature.familia.domain.repository.FamiliaRepository
import com.example.aguardapp.feature.familia.domain.usecase.desdeParaSincronizar
import com.example.aguardapp.feature.familia.domain.usecase.eventosParaGuardar
import com.example.aguardapp.feature.familia.domain.usecase.eventosParaSubir
import com.example.aguardapp.feature.familia.domain.usecase.normalizarCodigo
import com.example.aguardapp.feature.familia.domain.usecase.perfilParaAplicar
import com.example.aguardapp.feature.familia.domain.usecase.perfilParaSubir
import com.example.aguardapp.feature.familia.domain.usecase.validarApodo
import com.example.aguardapp.feature.liga.data.remote.CLAVE_SUPABASE

private const val CODIGO_EXPULSADO = "AF001"
private const val CODIGO_MENSAJE_PROPIO = "P0001"
private const val MINUTOS_INVITACION = 15
private const val NO_EN_FAMILIA = "Este teléfono no está en una familia"

class FamiliaRepositoryImpl(
    private val api: FamiliaApi,
    private val dao: FamiliaDao,
    private val preferencias: PreferenciasFamilia,
    private val usuarioId: String,
    private val hoy: () -> LocalDate,
    private val clave: String = CLAVE_SUPABASE
) : FamiliaRepository {
    private val autorizacion = "Bearer $clave"
    private val gson = Gson()
    private val mutex = Mutex()

    override val enFamilia: Flow<Boolean> = preferencias.secretoActual.map { it != null }

    override fun apodo(): String? = preferencias.apodo

    override fun ultimaSincronizacion(): Long? = preferencias.ultimaSincronizacion

    override suspend fun crear(apodo: String): Result<Unit> = llamar {
        val nombre = validarApodo(apodo)
        check(preferencias.secreto == null) { "Este teléfono ya está en una familia" }
        val perfil = checkNotNull(dao.perfil(usuarioId)) { "Configura tu hogar antes de crear la familia" }
        val secreto = UUID.randomUUID().toString()
        api.crear(clave, autorizacion, CrearFamiliaDto(secreto, preferencias.miembroId(), nombre, perfil.aCompartido().aDto()))
            .exigirExito()
        preferencias.entrar(secreto, nombre, reemplazarDatosLocales = false)
    }

    override suspend fun unirse(codigo: String, apodo: String): Result<Unit> = llamar {
        val nombre = validarApodo(apodo)
        val limpio = requireNotNull(normalizarCodigo(codigo)) { "El código tiene 10 letras y números, por ejemplo 3F9A1-C0B7E" }
        check(preferencias.secreto == null) { "Este teléfono ya está en una familia. Sal de ella primero" }
        val union = api.unirse(clave, autorizacion, UnirseDto(limpio, preferencias.miembroId(), nombre)).cuerpo()
        when (union.error) {
            null -> Unit
            "demasiados_intentos" -> error("Hubo demasiados intentos con códigos incorrectos. Espera unos minutos")
            "familia_llena" -> error("Esa familia ya tiene 10 teléfonos")
            else -> error("El código no existe o ya venció. Pide uno nuevo")
        }
        val secreto = checkNotNull(union.secreto) { "Supabase no devolvió la familia. Inténtalo de nuevo" }
        preferencias.entrar(secreto, nombre, reemplazarDatosLocales = true)
    }

    override suspend fun invitar(): Result<Invitacion> = llamar {
        val secreto = checkNotNull(preferencias.secreto) { NO_EN_FAMILIA }
        val invitacion = api.invitar(clave, autorizacion, MiembroDto(secreto, preferencias.miembroId())).cuerpo()
        Invitacion(checkNotNull(invitacion.codigo) { "Supabase no devolvió el código" }, invitacion.minutos ?: MINUTOS_INVITACION)
    }

    override suspend fun sincronizar(): Result<EstadoSincronizacion> = llamar { mutex.withLock { sincronizarAhora() } }

    override suspend fun salir(): Result<Unit> = llamar {
        val secreto = preferencias.secreto ?: return@llamar
        try {
            api.salir(clave, autorizacion, MiembroDto(secreto, preferencias.miembroId())).exigirExito()
        } catch (_: FamiliaExpulsadaException) {
            // Ya no estaba en la familia: basta con olvidarla en este teléfono.
        }
        preferencias.salir()
    }

    override suspend fun reiniciar(): Result<Unit> = llamar {
        val secreto = checkNotNull(preferencias.secreto) { NO_EN_FAMILIA }
        val nuevo = UUID.randomUUID().toString()
        val apodo = preferencias.apodo ?: "Yo"
        api.reiniciar(clave, autorizacion, ReiniciarDto(secreto, nuevo, preferencias.miembroId(), apodo)).exigirExito()
        preferencias.cambiarSecreto(nuevo)
    }

    private suspend fun sincronizarAhora(): EstadoSincronizacion {
        val secreto = checkNotNull(preferencias.secreto) { NO_EN_FAMILIA }
        // Recién unido: el historial del teléfono no se sube, se reemplaza por el de la familia.
        val reemplazar = preferencias.reemplazoPendiente
        val desde = desdeParaSincronizar(hoy())
        val locales = if (reemplazar) emptyList() else eventosLocales()
        val subir = eventosParaSubir(locales, preferencias.conocidos, desde)
        val perfilLocal = if (reemplazar) null else dao.perfil(usuarioId)?.aCompartido()
        val perfilASubir = perfilParaSubir(perfilLocal, perfilGuardado())

        val respuesta = api.sincronizar(
            clave, autorizacion,
            SincronizarDto(secreto, preferencias.miembroId(), subir.map { it.aDto() }, desde, perfilASubir?.aDto())
        ).cuerpo()

        val remotos = respuesta.eventos.orEmpty().mapNotNull { it.aCompartido() }
        val nuevos = eventosParaGuardar(remotos, locales.map { it.id }.toSet())
        val perfilRemoto = respuesta.perfil?.aCompartido()?.takeIf { it.esValido() }
        val perfilAplicar = perfilParaAplicar(perfilLocal, perfilRemoto, seSubio = perfilASubir != null)
        dao.aplicar(
            usuarioId = usuarioId,
            reemplazar = reemplazar,
            perfil = perfilAplicar?.aEntidad(usuarioId),
            llenados = nuevos.filter { it.tipo == TipoEventoFamiliar.LLENADO }.map { it.aLlenado(usuarioId) },
            novedades = nuevos.filter { it.tipo == TipoEventoFamiliar.SIN_AGUA }.mapNotNull { it.aNovedad(usuarioId) }
        )

        val perfilFinal = perfilAplicar ?: perfilASubir ?: perfilLocal
        preferencias.guardarSincronizacion(
            perfil = perfilFinal?.let { gson.toJson(it.aDto()) },
            conocidos = remotos.map { it.id }.toSet(),
            momentoMillis = System.currentTimeMillis()
        )
        return EstadoSincronizacion(
            miembros = respuesta.miembros.orEmpty().mapNotNull { miembro ->
                miembro.apodo?.let { MiembroFamilia(it, miembro.propio == true) }
            },
            nuevosDeOtros = if (reemplazar) emptyList() else nuevos.filterNot { it.propio }
        )
    }

    private suspend fun eventosLocales(): List<EventoCompartido> =
        dao.llenados(usuarioId).map { it.aCompartido() } + dao.novedades(usuarioId).map { it.aCompartido() }

    private fun perfilGuardado(): PerfilCompartido? = preferencias.perfilSincronizado?.let { json ->
        runCatching { gson.fromJson(json, PerfilFamiliaDto::class.java)?.aCompartido() }.getOrNull()
    }

    private suspend fun <T> llamar(accion: suspend () -> T): Result<T> = try {
        Result.success(accion())
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        Result.failure(IllegalStateException("Sin conexión. Revisa tu internet e inténtalo de nuevo", e))
    } catch (e: RuntimeException) {
        Result.failure(e)
    }

    private fun Response<*>.exigirExito() {
        if (!isSuccessful) fallar()
    }

    private fun <T> Response<T>.cuerpo(): T {
        if (!isSuccessful) fallar()
        return body() ?: error("Supabase respondió sin datos. Inténtalo de nuevo")
    }

    private fun Response<*>.fallar(): Nothing {
        val json = runCatching { JsonParser.parseString(errorBody()?.string()).asJsonObject }.getOrNull()
        val codigoError = json.texto("code")
        if (codigoError == CODIGO_EXPULSADO) {
            preferencias.salir()
            throw FamiliaExpulsadaException()
        }
        if (code() == 404) error("El modo familia aún no está activado en Supabase. Falta ejecutar supabase/modo_familia.sql")
        val mensaje = json.texto("message")?.takeIf { codigoError == CODIGO_MENSAJE_PROPIO }
        error(mensaje ?: "Supabase respondió con error ${code()}")
    }

    private fun JsonObject?.texto(campo: String): String? =
        this?.get(campo)?.takeIf { !it.isJsonNull }?.asString
}
