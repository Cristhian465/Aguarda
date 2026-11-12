package com.example.aguardapp.feature.mapa.presentation

import kotlinx.datetime.LocalDateTime
import com.example.aguardapp.feature.deposito.presentation.describirMomento
import com.example.aguardapp.feature.deposito.presentation.formatearHora
import com.example.aguardapp.feature.mapa.domain.usecase.CandidataCisterna
import com.example.aguardapp.feature.mapa.domain.usecase.ElegirCisternaConveniente

/** Aviso "Antes de que se acabe": cruza cuándo se acaba el agua del tanque con las cisternas del mapa. */
data class AvisoDeAgua(
    val titulo: String,
    val detalle: String,
    val faltaAgua: Boolean,
    val sugeridaId: String?
)

/**
 * Arma el aviso del mapa. Sin depósito (hogar sin llenados) no hay aviso.
 * Falta agua cuando el tanque se acaba antes del próximo llenado.
 */
fun armarAvisoDeAgua(
    agotamiento: LocalDateTime?,
    proximoLlenado: LocalDateTime?,
    ahora: LocalDateTime,
    candidatas: List<CandidataCisterna>,
    elegir: ElegirCisternaConveniente = ElegirCisternaConveniente()
): AvisoDeAgua? {
    if (agotamiento == null || proximoLlenado == null) return null
    val faltaAgua = agotamiento < proximoLlenado
    val sugerida = elegir.elegir(candidatas, ahora.time)
    val titulo = when {
        agotamiento <= ahora -> "Tu tanque ya está vacío"
        faltaAgua -> "Tu agua se acaba ${describirMomento(agotamiento, ahora)}"
        else -> "Tu agua alcanza hasta el próximo llenado"
    }
    return AvisoDeAgua(titulo, detalleDe(sugerida, faltaAgua, ahora, elegir), faltaAgua, sugerida?.punto?.id)
}

private fun detalleDe(
    sugerida: CandidataCisterna?,
    faltaAgua: Boolean,
    ahora: LocalDateTime,
    elegir: ElegirCisternaConveniente
): String {
    if (sugerida == null) {
        return if (faltaAgua) "Hoy ya no quedan cisternas repartiendo. Revisa qué recortar en Mi depósito."
        else "Hoy ya no quedan cisternas repartiendo."
    }
    val punto = sugerida.punto
    val lugar = punto.nombre + (sugerida.distanciaKm?.let { " (${formatearDistancia(it)})" } ?: "")
    val horario = if (elegir.yaEmpezo(punto, ahora.time)) {
        "reparte hasta ${conArticulo(formatearHora(punto.horarioFin))}"
    } else {
        "reparte hoy de ${formatearHora(punto.horarioInicio)} a ${formatearHora(punto.horarioFin)}"
    }
    // La hora ya termina en "a.m."/"p.m.": no se agrega otro punto.
    return if (faltaAgua) "Ve a $lugar: $horario" else "Si necesitas más, la que te conviene es $lugar: $horario"
}

/** "la 1:00 p.m." pero "las 3:00 p.m.". */
private fun conArticulo(hora: String): String = if (hora.startsWith("1:")) "la $hora" else "las $hora"
