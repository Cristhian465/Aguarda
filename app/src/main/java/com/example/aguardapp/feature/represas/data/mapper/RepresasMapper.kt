package com.example.aguardapp.feature.represas.data.mapper

import com.example.aguardapp.feature.mapa.domain.model.Coordenada
import com.example.aguardapp.feature.represas.data.local.DistritoHogarEntity
import com.example.aguardapp.feature.represas.data.local.PresaEntity
import com.example.aguardapp.feature.represas.data.remote.DireccionDto
import com.example.aguardapp.feature.represas.data.remote.PresaDto
import com.example.aguardapp.feature.represas.domain.model.Distrito
import com.example.aguardapp.feature.represas.domain.model.DistritoDelHogar
import com.example.aguardapp.feature.represas.domain.model.Presa
import com.example.aguardapp.feature.represas.domain.model.UsoPresa
import com.example.aguardapp.feature.represas.domain.model.sinTildes

private val SIN_DATO = setOf("no disponible", "nd", "n.d.", "-")
private val CONECTORES = setOf("de", "del", "la", "las", "los", "y")

/** Una presa de la ANA sin nombre o sin ubicación válida no sirve para el mapa: se descarta (null). */
fun PresaDto.aDominio(): Presa? {
    val datos = atributos ?: return null
    val nombre = datos.nombre.limpio()?.aNombrePropio() ?: return null
    val latitud = punto?.y
    val longitud = punto?.x
    if (latitud == null || longitud == null) return null
    val ubicacion = runCatching { Coordenada(latitud, longitud) }.getOrNull() ?: return null
    return Presa(
        nombre = nombre,
        ubicacion = ubicacion,
        rio = datos.rio.limpio(),
        distrito = datos.distrito.limpio(),
        volumenUtilHm3 = datos.volumenUtilHm3.comoNumero(),
        alturaMetros = datos.alturaMetros.comoNumero(),
        altitudMetros = datos.altitudMetros?.takeIf { it > 0 },
        anioInicio = datos.anioInicio.limpio()?.let { Regex("\\d{4}").find(it)?.value?.toInt() },
        usos = usosDe(listOfNotNull(datos.uso, datos.finalidad).joinToString(" ")),
        operador = datos.operador.limpio()
    )
}

fun Presa.aEntidad(descargadaEn: String) = PresaEntity(
    nombre = nombre,
    latitud = ubicacion.latitud,
    longitud = ubicacion.longitud,
    rio = rio,
    distrito = distrito,
    volumenUtilHm3 = volumenUtilHm3,
    alturaMetros = alturaMetros,
    altitudMetros = altitudMetros,
    anioInicio = anioInicio,
    usos = usos.joinToString(",") { it.name },
    operador = operador,
    descargadaEn = descargadaEn
)

fun PresaEntity.aDominio() = Presa(
    nombre = nombre,
    ubicacion = Coordenada(latitud, longitud),
    rio = rio,
    distrito = distrito,
    volumenUtilHm3 = volumenUtilHm3,
    alturaMetros = alturaMetros,
    altitudMetros = altitudMetros,
    anioInicio = anioInicio,
    usos = usos.split(",").mapNotNull { uso -> UsoPresa.entries.firstOrNull { it.name == uso } }.toSet(),
    operador = operador
)

fun DistritoHogarEntity.aDominio(): DistritoDelHogar? =
    Distrito.entries.firstOrNull { it.name == distrito }?.let { DistritoDelHogar(it, detectadoPorGps) }

/** El distrito de Tacna de una dirección de Nominatim, o null si queda fuera de los distritos conocidos. */
fun DireccionDto.distrito(): Distrito? {
    val partes = partes ?: return null
    return listOf(partes.ciudad, partes.pueblo, partes.caserio, partes.sector).firstNotNullOfOrNull { Distrito.desdeTexto(it) }
}

// La ANA escribe los usos en texto libre, a veces con erratas: "Riego, regadío / Suminsitro de agua (I/S)".
internal fun usosDe(texto: String): Set<UsoPresa> {
    val normalizado = texto.sinTildes().lowercase()
    return buildSet {
        if ("sumin" in normalizado) add(UsoPresa.AGUA_POTABLE)
        if ("riego" in normalizado || "regad" in normalizado) add(UsoPresa.RIEGO)
        if ("miner" in normalizado) add(UsoPresa.MINERIA)
    }
}

// Quita espacios sobrantes ("Palca ") y convierte "No disponible" en null.
internal fun String?.limpio(): String? {
    val texto = this?.trim()?.replace(Regex("\\s+"), " ") ?: return null
    return texto.takeUnless { it.isEmpty() || it.lowercase() in SIN_DATO }
}

internal fun String?.comoNumero(): Double? = limpio()?.replace(',', '.')?.toDoubleOrNull()?.takeIf { it > 0 }

// "PAMPA DE VACA" -> "Pampa de Vaca"
internal fun String.aNombrePropio(): String =
    lowercase().split(" ").mapIndexed { i, palabra ->
        if (i > 0 && palabra in CONECTORES) palabra else palabra.replaceFirstChar { it.titlecase() }
    }.joinToString(" ")
