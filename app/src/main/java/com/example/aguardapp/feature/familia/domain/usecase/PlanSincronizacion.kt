package com.example.aguardapp.feature.familia.domain.usecase

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import com.example.aguardapp.feature.familia.domain.model.EventoCompartido
import com.example.aguardapp.feature.familia.domain.model.PerfilCompartido

/** Días de historial que se comparten. El servidor conserva 120 y devuelve desde esta fecha. */
const val DIAS_COMPARTIDOS = 85
const val MAX_EVENTOS_POR_ENVIO = 200
const val PREFIJO_QR = "aguarda-familia:"

private val UUID = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
private val CODIGO = Regex("^[0-9A-F]{10}$")
private const val LARGO_MAXIMO_APODO = 30

fun desdeParaSincronizar(hoy: LocalDate): String = hoy.minus(DIAS_COMPARTIDOS, DateTimeUnit.DAY).toString()

/**
 * Eventos del teléfono que el servidor todavía no tiene. Solo los recientes y con un id válido;
 * si son muchos se envían los más nuevos y el resto en la siguiente sincronización.
 */
fun eventosParaSubir(locales: List<EventoCompartido>, conocidos: Set<String>, desde: String): List<EventoCompartido> =
    locales.filter { it.id !in conocidos && it.momento >= desde && UUID.matches(it.id) }
        .sortedByDescending { it.momento }
        .take(MAX_EVENTOS_POR_ENVIO)
        .sortedBy { it.momento }

/** Eventos de la familia que faltan en el teléfono. */
fun eventosParaGuardar(remotos: List<EventoCompartido>, idsLocales: Set<String>): List<EventoCompartido> =
    remotos.filter { it.id !in idsLocales }

/** El perfil se sube solo si cambió en este teléfono desde la última sincronización. */
fun perfilParaSubir(local: PerfilCompartido?, ultimoSincronizado: PerfilCompartido?): PerfilCompartido? =
    local?.takeIf { it != ultimoSincronizado }

/** Si el teléfono no subió cambios, adopta el perfil de la familia cuando es distinto. Gana el último cambio. */
fun perfilParaAplicar(local: PerfilCompartido?, remoto: PerfilCompartido?, seSubio: Boolean): PerfilCompartido? =
    remoto?.takeIf { !seSubio && it != local }

/** Acepta el código escrito a mano (con o sin guion, en minúsculas) o el contenido del QR. */
fun normalizarCodigo(texto: String): String? {
    val limpio = texto.trim().removePrefix(PREFIJO_QR).uppercase().filter { it.isLetterOrDigit() }
    return limpio.takeIf { CODIGO.matches(it) }
}

fun formatearCodigo(codigo: String): String =
    if (codigo.length == 10) codigo.take(5) + "-" + codigo.drop(5) else codigo

fun validarApodo(texto: String): String {
    val apodo = texto.trim()
    require(apodo.length in 1..LARGO_MAXIMO_APODO) { "Escribe cómo te llamamos, de 1 a $LARGO_MAXIMO_APODO letras" }
    return apodo
}
