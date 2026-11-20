package com.example.aguardapp.feature.mapa.presentation

import kotlinx.datetime.LocalDateTime
import com.example.aguardapp.feature.deposito.presentation.describirMomento
import com.example.aguardapp.feature.deposito.presentation.formatearHora
import com.example.aguardapp.feature.mapa.domain.usecase.CandidataCisterna
import com.example.aguardapp.feature.mapa.domain.usecase.ElegirCisternaConveniente

data class AvisoDeAgua(
    val titulo: String,
    val detalle: String,
    val faltaAgua: Boolean,
    val sugeridaId: String?
)

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
    return if (faltaAgua) "Ve a $lugar: $horario" else "Si necesitas más, la que te conviene es $lugar: $horario"
}

private fun conArticulo(hora: String): String = if (hora.startsWith("1:")) "la $hora" else "las $hora"
