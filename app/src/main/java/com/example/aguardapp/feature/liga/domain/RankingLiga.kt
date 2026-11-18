package com.example.aguardapp.feature.liga.domain

import kotlinx.datetime.LocalDate

/** Lo único que sale del teléfono: un código anónimo y el resultado semanal del hogar. */
data class EnvioSemanal(
    val token: String,
    val lunes: LocalDate,
    val ubigeo: String,
    val litrosPorPersonaDia: Double,
    val puntaje: Int,
    val verificado: Boolean
)

/** Resultado de un distrito según el servidor. Sin 5 hogares, el puntaje llega vacío. */
data class PosicionDistrito(
    val ubigeo: String,
    val hogares: Int,
    val verificados: Int,
    val puntaje: Double?,
    val litrosPorPersonaDia: Double?
)

/** Fila del ranking ya ordenada. Los distritos sin datos suficientes no tienen puesto. */
data class FilaRanking(
    val puesto: Int?,
    val ubigeo: String,
    val nombre: String,
    val hogares: Int,
    val verificados: Int,
    val puntaje: Double?
)

interface RankingRepository {
    /** `false` mientras no se configure el proyecto de Supabase. */
    val conectado: Boolean

    suspend fun enviar(envio: EnvioSemanal): Result<Unit>

    suspend fun ranking(lunes: LocalDate): Result<List<PosicionDistrito>>

    suspend fun borrarMisDatos(token: String): Result<Unit>
}

/** Une los distritos del mapa con los resultados: primero los que compiten, de mayor a menor puntaje. */
fun armarRanking(distritos: List<Pair<String, String>>, posiciones: List<PosicionDistrito>): List<FilaRanking> {
    val porUbigeo = posiciones.associateBy { it.ubigeo }
    val (compiten, sinDatos) = distritos.partition { (ubigeo, _) -> porUbigeo[ubigeo]?.puntaje != null }
    val ordenados = compiten.sortedByDescending { (ubigeo, _) -> porUbigeo.getValue(ubigeo).puntaje }
    val conPuesto = ordenados.mapIndexed { indice, (ubigeo, nombre) -> fila(indice + 1, ubigeo, nombre, porUbigeo[ubigeo]) }
    return conPuesto + sinDatos.map { (ubigeo, nombre) -> fila(null, ubigeo, nombre, porUbigeo[ubigeo]) }
}

private fun fila(puesto: Int?, ubigeo: String, nombre: String, posicion: PosicionDistrito?) = FilaRanking(
    puesto = puesto,
    ubigeo = ubigeo,
    nombre = nombre,
    hogares = posicion?.hogares ?: 0,
    verificados = posicion?.verificados ?: 0,
    puntaje = posicion?.puntaje
)
