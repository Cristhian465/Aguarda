package com.example.aguardapp.feature.represas.domain.model

import com.example.aguardapp.feature.mapa.domain.model.Coordenada

/** Para qué se usa el agua de una represa, según el inventario de la ANA. */
enum class UsoPresa { AGUA_POTABLE, RIEGO, MINERIA, OTRO }

private const val LITROS_POR_HM3 = 1_000_000_000.0

/** Una represa del inventario oficial de la Autoridad Nacional del Agua (ANA). Los datos que la ANA no tiene llegan en null. */
data class Presa(
    val nombre: String,
    val ubicacion: Coordenada,
    val rio: String?,
    val distrito: String?,
    val volumenUtilHm3: Double?,
    val alturaMetros: Double?,
    val altitudMetros: Double?,
    val anioInicio: Int?,
    val usos: Set<UsoPresa>,
    val operador: String?
) {
    /** El uso que define su color: el agua potable pesa más que la minería, y esta más que el riego. */
    val usoPrincipal: UsoPresa
        get() = listOf(UsoPresa.AGUA_POTABLE, UsoPresa.MINERIA, UsoPresa.RIEGO).firstOrNull { it in usos } ?: UsoPresa.OTRO

    /** Cuántos tanques de [capacidadLitros] litros caben en su volumen útil. */
    fun tanquesEquivalentes(capacidadLitros: Double): Double? =
        volumenUtilHm3?.takeIf { capacidadLitros > 0 }?.let { it * LITROS_POR_HM3 / capacidadLitros }
}
