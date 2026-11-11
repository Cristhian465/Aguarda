package com.example.aguardapp.feature.represas.presentation

import androidx.compose.ui.graphics.Color
import kotlin.math.roundToInt
import com.example.aguardapp.core.ui.theme.Agua
import com.example.aguardapp.core.ui.theme.Coral
import com.example.aguardapp.core.ui.theme.Ocre
import com.example.aguardapp.core.ui.theme.TintaTenue
import com.example.aguardapp.feature.deposito.presentation.formatearMiles
import com.example.aguardapp.feature.represas.domain.model.UsoPresa

/** 8.5 -> "8.5", 4545.0 -> "4 545", 2.349 -> "2.35". */
fun formatearDecimal(valor: Double): String {
    val centesimos = (valor * 100).roundToInt()
    return if (centesimos % 100 == 0) formatearMiles(centesimos / 100) else (centesimos / 100.0).toString()
}

/** 7 727 272 -> "7.7 millones de tanques"; 545 454 -> "545 mil tanques". */
fun describirTanques(cantidad: Double): String = when {
    cantidad >= 1_000_000 -> "${formatearDecimal((cantidad / 100_000).roundToInt() / 10.0)} millones de tanques"
    cantidad >= 1_000 -> "${(cantidad / 1_000).roundToInt()} mil tanques"
    else -> "${cantidad.roundToInt()} tanques"
}

/** {RIEGO, AGUA_POTABLE} -> "Riego y suministro de agua". */
fun describirUsos(usos: Set<UsoPresa>): String {
    val nombres = listOf(UsoPresa.RIEGO to "riego", UsoPresa.AGUA_POTABLE to "suministro de agua", UsoPresa.MINERIA to "minería")
        .filter { (uso, _) -> uso in usos }
        .map { it.second }
    if (nombres.isEmpty()) return "Sin dato"
    val texto = if (nombres.size == 1) nombres[0] else nombres.dropLast(1).joinToString(", ") + " y " + nombres.last()
    return texto.replaceFirstChar { it.uppercase() }
}

fun colorDelUso(uso: UsoPresa): Color = when (uso) {
    UsoPresa.AGUA_POTABLE -> Agua
    UsoPresa.RIEGO -> Ocre
    UsoPresa.MINERIA -> Coral
    UsoPresa.OTRO -> TintaTenue
}
