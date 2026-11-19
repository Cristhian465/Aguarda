package com.example.aguardapp.feature.liga.domain

import java.text.Normalizer
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.number
import kotlinx.datetime.plus

/** Mes facturado en el recibo. */
data class MesRecibo(val anio: Int, val mes: Int) {
    init {
        require(mes in 1..12) { "Mes inválido: $mes" }
    }

    val dias: Int
        get() = LocalDate(anio, mes, 1).let { it.daysUntil(it.plus(1, DateTimeUnit.MONTH)) }

    fun mesesHasta(hoy: LocalDate): Int = (hoy.year * 12 + hoy.month.number) - (anio * 12 + mes)

    override fun toString(): String = "${mes.toString().padStart(2, '0')}/$anio"

    companion object {
        /** Acepta "11/2026". */
        fun desdeTexto(texto: String): MesRecibo? {
            val (mes, anio) = Regex("""^\s*(\d{1,2})/(20\d{2})\s*$""").find(texto)?.destructured ?: return null
            return mes.toInt().takeIf { it in 1..12 }?.let { MesRecibo(anio.toInt(), it) }
        }
    }
}

/**
 * Consumo confirmado desde foto (sin cambios) u obtenido del portal EPS autenticado.
 * La verificación indica origen y vigencia; no valida firmas digitales del documento.
 */
data class ReciboGuardado(
    val mes: MesRecibo,
    val metrosCubicos: Double,
    val leidoDeLaFoto: Boolean,
    val leidoDeEps: Boolean = false,
    val totalPagar: Double? = null,
    val actualizadoEn: String? = null
) {
    fun vigente(hoy: LocalDate): Boolean = mes.mesesHasta(hoy) in 0..MESES_DE_VIGENCIA

    fun verificado(hoy: LocalDate): Boolean = (leidoDeLaFoto || leidoDeEps) && vigente(hoy)

    fun litrosPorPersonaDia(habitantes: Int): Double = metrosCubicos * LITROS_POR_M3 / mes.dias / habitantes

    private companion object {
        const val MESES_DE_VIGENCIA = 2
        const val LITROS_POR_M3 = 1000.0
    }
}

private const val NUMERO = """(\d{1,3}(?:[.,]\d{1,2})?)"""
private const val UNIDAD = """m\s*[3³]"""
private val NUMERO_Y_UNIDAD = Regex("""(?<![\d.,])$NUMERO\s*$UNIDAD(?![a-z\d])""")
private val UNIDAD_Y_NUMERO = Regex("""$UNIDAD\W{0,6}$NUMERO(?![\d/-])""")
private val MESES = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "se[pt]", "oct", "nov", "dic")
private val MES_EN_LETRAS = Regex("""\b(${MESES.joinToString("|")})[a-z]*\.?[\s/-]*(?:de\s+)?(20\d{2})\b""")
private val MES_EN_NUMEROS = Regex("""(?<!\d)(0?[1-9]|1[0-2])[/-](20\d{2})\b""")
private val LINEA_DE_PERIODO = Regex("""periodo|mes|facturac""")

private fun normalizar(texto: String): String =
    Normalizer.normalize(texto.lowercase(), Normalizer.Form.NFD).replace(Regex("""\p{M}"""), "")

private fun aNumero(texto: String): Double? = texto.replace(',', '.').toDoubleOrNull()?.takeIf { it > 0 && it <= 500 }

/**
 * Busca los m³ consumidos en el texto del recibo. Prefiere la línea que dice "consumo". Si hay varios
 * valores posibles y ninguno está en esa línea, no adivina: devuelve `null` y el usuario lo escribe.
 */
fun leerConsumoDelRecibo(texto: String): Double? {
    val lineas = normalizar(texto).lines()
    lineas.filter { "consumo" in it }.forEach { linea ->
        val valor = NUMERO_Y_UNIDAD.find(linea) ?: UNIDAD_Y_NUMERO.find(linea)
        valor?.let { aNumero(it.groupValues[1]) }?.let { return it }
    }
    return lineas.flatMap { linea -> NUMERO_Y_UNIDAD.findAll(linea).mapNotNull { aNumero(it.groupValues[1]) } }
        .distinct()
        .singleOrNull()
}

/** Busca el mes facturado; prefiere las líneas que hablan del periodo o del mes. */
fun leerMesDelRecibo(texto: String): MesRecibo? {
    val lineas = normalizar(texto).lines()
    val ordenadas = lineas.filter { LINEA_DE_PERIODO.containsMatchIn(it) } + lineas
    ordenadas.forEach { linea -> mesEnLinea(linea)?.let { return it } }
    return null
}

private fun mesEnLinea(linea: String): MesRecibo? {
    MES_EN_LETRAS.find(linea)?.let { encontrado ->
        val indice = MESES.indexOfFirst { Regex("^$it$").matches(encontrado.groupValues[1]) }
        return MesRecibo(encontrado.groupValues[2].toInt(), indice + 1)
    }
    return MES_EN_NUMEROS.find(linea)?.let { MesRecibo(it.groupValues[2].toInt(), it.groupValues[1].toInt()) }
}
