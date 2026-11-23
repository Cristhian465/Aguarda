package com.example.aguardapp.feature.cortes.domain

data class CorteAgua(
    val id: String,
    val titulo: String,
    val fuente: String,
    val url: String,
    val tipo: String,
    val distrito: String?,
    val sector: String?,
    val motivo: String?,
    val inicio: String?,
    val reposicionEstimada: String?,
    val restablecido: String?,
    val publicado: String?,
    val categoria: String,
    val detalle: String?,
    val distritos: List<String>,
    val sectores: List<String>,
    val sectorOrigen: String?
)

data class CatalogoSectores(val distritos: List<String>, val sectores: List<SectorEps>)

data class SectorEps(
    val id: String,
    val nombre: String,
    val distritos: List<String>,
    val subsectores: List<Int>,
    val zonas: List<String>
) {
    val etiqueta get() = "$id · $nombre"
}

enum class CategoriaAviso(val etiqueta: String) {
    CORTE("Cortes"), MANTENIMIENTO("Mantenimiento"), REPARACION("Reparaciones"),
    ABASTECIMIENTO("Abastecimiento"), ATENCION("Atención al usuario"), COMUNICADO("Otros comunicados")
}

enum class PeriodoAviso(val codigo: String, val etiqueta: String) {
    DIA("24h", "Últimas 24 horas"), SEMANA("7d", "Últimos 7 días"), MES("30d", "Últimos 30 días"),
    ANIO("365d", "Último año"), TODOS("todos", "Todo el historial")
}

data class FiltrosAvisos(
    val sector: String? = null,
    val categoria: CategoriaAviso? = null,
    val periodo: PeriodoAviso = PeriodoAviso.SEMANA,
    val buscar: String = ""
)

const val AVISOS_POR_PAGINA = 50

interface CortesRepository {
    val configurado: Boolean
    suspend fun obtenerAvisos(filtros: FiltrosAvisos, desde: Int): List<CorteAgua>
}
