package com.example.aguardapp.feature.eps.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.aguardapp.core.ui.theme.IconosClarosEnBarraDeEstado
import com.example.aguardapp.feature.deposito.presentation.componentes.BarraSuperior
import com.example.aguardapp.feature.deposito.presentation.componentes.TarjetaBlanca
import com.example.aguardapp.feature.eps.domain.ReciboGuardado
import com.example.aguardapp.feature.eps.domain.compararRecibos
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs
import kotlinx.datetime.LocalDate
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun RecibosEpsScreen(onVolver: () -> Unit, viewModel: ReciboViewModel = koinViewModel()) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val historial = estado.historial

    IconosClarosEnBarraDeEstado(claros = false)
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        BarraSuperior("Recibos EPS", "Consulta y compara tus recibos de agua", onVolver)
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { TarjetaEps() }
            item { TarjetaRecibo(estado, viewModel) }
            item {
                Text("Historial de recibos", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            when (historial.size) {
                0 -> item { Text("Actualiza desde EPS o añade una foto para guardar tu primer recibo.") }
                1 -> item { Text("Añade otro periodo para comparar tus recibos.") }
                else -> item { TarjetaComparacion(historial[0], historial[1]) }
            }
            items(historial, key = { it.mes.toString() }) { recibo -> TarjetaPeriodo(recibo, estado.hoy) }
        }
    }
}

@Composable
private fun TarjetaComparacion(reciente: ReciboGuardado, anterior: ReciboGuardado) {
    val cambio = compararRecibos(reciente, anterior)
    TarjetaBlanca {
        Text("Comparación de periodos", fontWeight = FontWeight.Bold)
        Text("${reciente.mes} frente a ${anterior.mes}")
        Text("Consumo: ${describirCambio(cambio.metrosCubicos, unidad = " m³")}")
        Text("Total a pagar: ${cambio.totalPagar?.let { describirCambio(it, prefijo = "S/ ", decimales = 2) } ?: "Falta el total de uno o ambos periodos"}")
        cambio.litrosPorDia?.let { Text("Consumo diario: ${describirCambio(it, unidad = " L/día", decimales = 1)}") }
    }
}

@Composable
private fun TarjetaPeriodo(recibo: ReciboGuardado, hoy: LocalDate) {
    TarjetaBlanca {
        Text(recibo.mes.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text("Consumo: ${decimal(recibo.metrosCubicos)} m³")
        Text("Total a pagar: ${recibo.totalPagar?.let { "S/ ${decimal(it, 2, 2)}" } ?: "No registrado"}")
        recibo.totalMes?.let { Text("Cargo del mes: S/ ${decimal(it, 2, 2)}") }
        recibo.deuda?.let { Text("Deuda: S/ ${decimal(it, 2, 2)}") }
        recibo.litrosPorDia?.let {
            Text("${decimal(it, maximo = 1)} L/día · ${recibo.diasFacturados} días facturados")
        }
        Text(
            if (recibo.leidoDeEps) "Obtenido de EPS Tacna" else "Añadido por foto",
            style = MaterialTheme.typography.bodySmall
        )
        if (recibo.verificado(hoy)) Text("Verificado", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
    }
}

private fun describirCambio(valor: Double, prefijo: String = "", unidad: String = "", decimales: Int = 2): String {
    val magnitud = decimal(abs(valor), minimo = if (prefijo.isEmpty()) 0 else decimales, maximo = decimales)
    if (abs(valor) < 0.5 / Math.pow(10.0, decimales.toDouble())) return "Igual"
    return "$prefijo$magnitud$unidad ${if (valor > 0) "más" else "menos"}"
}

private val localeRecibos = Locale.forLanguageTag("es-PE")

private fun decimal(valor: Double, minimo: Int = 0, maximo: Int = 2): String =
    NumberFormat.getNumberInstance(localeRecibos).apply {
        minimumFractionDigits = minimo
        maximumFractionDigits = maximo
    }.format(valor)
