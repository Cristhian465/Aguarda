package com.example.aguardapp.feature.sensor.domain.usecase

import kotlin.math.roundToInt
import kotlin.time.ExperimentalTime
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.minus
import com.example.aguardapp.feature.sensor.domain.model.LecturaSensor

enum class EstadoFuga {
    /** No hay lecturas suficientes para opinar. */
    SIN_DATOS,

    /** El nivel se mantuvo estable. */
    SIN_FUGA,

    /** El nivel bajó de forma constante sin señales de uso. */
    POSIBLE_FUGA,

    /** Hubo un llenado o un uso puntual: no se puede saber si hay fuga. */
    NO_CONCLUYENTE
}

data class AnalisisFuga(
    val estado: EstadoFuga,
    val litrosPerdidos: Int = 0,
    val litrosPorHora: Double = 0.0,
    val litrosAlMes: Int = 0
)

private const val HORA_INICIO = 1
private const val HORA_FIN = 5
private const val MINUTOS_POR_BLOQUE = 30
private const val LECTURAS_MINIMAS_POR_BLOQUE = 3
private const val BLOQUES_MINIMOS = 4

// Puntos porcentuales del tanque. Por debajo de esta caída no se distingue del ruido del sensor.
private const val CAIDA_MINIMA = 1.5
private const val SUBIDA_DE_LLENADO = 1.0

// Si un solo bloque concentra más de esta parte de la caída, fue un uso puntual y no una fuga.
private const val PARTE_MAXIMA_DE_UN_PASO = 0.6
private const val HORAS_AL_MES = 24 * 30

/** La última madrugada completa (de 1 a 5 a. m.): la de hoy si ya pasaron las 5, si no la de ayer. */
fun ultimaMadrugada(ahora: LocalDateTime): Pair<LocalDateTime, LocalDateTime> {
    val dia = if (ahora.hour >= HORA_FIN) ahora.date else ahora.date.minus(1, DateTimeUnit.DAY)
    return LocalDateTime(dia, LocalTime(HORA_INICIO, 0)) to LocalDateTime(dia, LocalTime(HORA_FIN, 0))
}

/**
 * Busca una fuga en las lecturas de una madrugada. Agrupa las lecturas en bloques de 30 minutos y usa
 * la mediana de cada bloque para filtrar el ruido del sensor. Una fuga baja el nivel poco a poco;
 * un uso puntual lo baja de golpe y un llenado lo sube: en esos dos casos no se opina.
 */
@OptIn(ExperimentalTime::class)
fun detectarFuga(lecturas: List<LecturaSensor>, capacidadLitros: Double): AnalisisFuga {
    if (capacidadLitros <= 0 || lecturas.isEmpty()) return AnalisisFuga(EstadoFuga.SIN_DATOS)
    val ordenadas = lecturas.sortedBy { it.medidaEn }
    val inicio = ordenadas.first().medidaEn
    val bloques = ordenadas
        .groupBy { ((it.medidaEn - inicio).inWholeMinutes / MINUTOS_POR_BLOQUE).toInt() }
        .filterValues { it.size >= LECTURAS_MINIMAS_POR_BLOQUE }
        .toSortedMap()
    if (bloques.size < BLOQUES_MINIMOS) return AnalisisFuga(EstadoFuga.SIN_DATOS)

    val niveles = bloques.values.map { bloque -> mediana(bloque.map { it.porcentaje }) }
    val pasos = niveles.zipWithNext { antes, despues -> antes - despues }
    if (pasos.any { it < -SUBIDA_DE_LLENADO }) return AnalisisFuga(EstadoFuga.NO_CONCLUYENTE)

    val caida = niveles.first() - niveles.last()
    if (caida < CAIDA_MINIMA) return AnalisisFuga(EstadoFuga.SIN_FUGA)
    if (pasos.max() > caida * PARTE_MAXIMA_DE_UN_PASO) return AnalisisFuga(EstadoFuga.NO_CONCLUYENTE)

    val horas = (bloques.lastKey() - bloques.firstKey()) * MINUTOS_POR_BLOQUE / 60.0
    val litros = capacidadLitros * caida / 100
    val litrosPorHora = litros / horas
    return AnalisisFuga(
        estado = EstadoFuga.POSIBLE_FUGA,
        litrosPerdidos = litros.roundToInt(),
        litrosPorHora = litrosPorHora,
        litrosAlMes = (litrosPorHora * HORAS_AL_MES).roundToInt()
    )
}

private fun mediana(valores: List<Double>): Double {
    val ordenados = valores.sorted()
    val mitad = ordenados.size / 2
    return if (ordenados.size % 2 == 1) ordenados[mitad] else (ordenados[mitad - 1] + ordenados[mitad]) / 2
}
