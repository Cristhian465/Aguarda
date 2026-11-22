package com.example.aguardapp.feature.eps.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import com.example.aguardapp.feature.deposito.presentation.componentes.TarjetaBlanca
import com.example.aguardapp.feature.eps.domain.PeriodoEvolucion
import com.example.aguardapp.feature.eps.domain.ReciboGuardado
import com.example.aguardapp.feature.eps.domain.periodosDeEvolucion
import com.example.aguardapp.feature.eps.domain.promedioConsumoAnterior

private val FORMATO_META = Regex("""[0-9]+(?:[.][0-9]{1,2})?""")
private const val SIN_DATOS = "Sin datos"
private val AlturaBarras = 76.dp
private val MargenBarras = 8.dp

@Composable
internal fun TarjetaEvolucion(historial: List<ReciboGuardado>, meta: Double?) {
    val periodos = remember(historial) { periodosDeEvolucion(historial) }
    val ultimo = periodos.lastOrNull() ?: return
    val promedio = remember(periodos) { promedioConsumoAnterior(periodos) }
    var verCargo by rememberSaveable { mutableStateOf(false) }
    // Una selección explícita se conserva al cambiar de métrica o al actualizar los recibos.
    var mesElegido by rememberSaveable { mutableStateOf<String?>(null) }
    val seleccionado = periodos.firstOrNull { it.mes.toString() == mesElegido } ?: ultimo
    val valores = remember(periodos, verCargo) {
        periodos.associate { periodo ->
            periodo.mes to periodo.recibo?.let { if (verCargo) it.totalMes else it.metrosCubicos }
        }
    }
    val metaVisible = meta?.takeUnless { verCargo }
    val maximoDatos = remember(valores) { valores.values.filterNotNull().maxOrNull() ?: 0.0 }
    val maximo = maxOf(maximoDatos, metaVisible ?: 0.0).takeIf { it > 0 } ?: 1.0
    val colorMeta = MaterialTheme.colorScheme.primary

    val desplazamiento = rememberLazyListState(initialFirstVisibleItemIndex = periodos.indexOf(seleccionado))
    LaunchedEffect(ultimo.mes, periodos.size) {
        if (mesElegido == null) desplazamiento.scrollToItem(periodos.lastIndex)
    }
    val espacioEtiqueta = with(LocalDensity.current) {
        MaterialTheme.typography.labelSmall.lineHeight.toDp() * 2 + 4.dp
    }

    TarjetaBlanca {
        val colores = MaterialTheme.colorScheme
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Evolución del consumo", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Historial completo", style = MaterialTheme.typography.labelMedium, color = colores.primary)
        }
        Row(
            Modifier.fillMaxWidth()
                .padding(vertical = 8.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(colores.surfaceVariant)
                .selectableGroup()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SegmentoEvolucion("Consumo (m³)", !verCargo, Modifier.weight(1f)) { verCargo = false }
            SegmentoEvolucion("Cargo del mes (S/)", verCargo, Modifier.weight(1f)) { verCargo = true }
        }
        if (!verCargo && meta != null) {
            Text("Meta · ${decimal(meta)} m³", style = MaterialTheme.typography.bodySmall)
        }
        LazyRow(
            state = desplazamiento,
            modifier = Modifier.fillMaxWidth().selectableGroup().drawBehind {
                if (metaVisible != null) {
                    val y = (MargenBarras + espacioEtiqueta).toPx() +
                        AlturaBarras.toPx() * (1 - metaVisible / maximo).toFloat()
                    drawLine(colorMeta, Offset(0f, y), Offset(size.width, y), 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 3.dp.toPx())))
                }
            },
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(periodos, key = { it.mes.toString() }) { periodo ->
                val valor = valores[periodo.mes]
                BarraPeriodo(periodo, valor, maximo, verCargo, periodo.mes == seleccionado.mes, espacioEtiqueta) {
                    mesElegido = periodo.mes.toString()
                }
            }
        }
        if (valores.values.any { it == null }) {
            Text("—  Sin datos", style = MaterialTheme.typography.bodySmall, color = colores.onSurfaceVariant)
        }
        DetallePeriodo(seleccionado, meta)
        val consumoUltimo = ultimo.recibo?.metrosCubicos
        if (promedio != null && consumoUltimo != null) {
            val cambio = describirCambio(consumoUltimo - promedio, unidad = " m³")
            val resumen = if (cambio == "Igual") "igual al promedio anterior" else "$cambio que el promedio anterior"
            Text("${ultimo.mes} · $resumen", fontWeight = FontWeight.SemiBold)
            Text("Promedio anterior · ${decimal(promedio)} m³", style = MaterialTheme.typography.bodySmall)
        } else {
            Text("Añade otro periodo para calcular el promedio anterior.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SegmentoEvolucion(texto: String, seleccionado: Boolean, modifier: Modifier, seleccionar: () -> Unit) {
    val colores = MaterialTheme.colorScheme
    Box(
        modifier.clip(RoundedCornerShape(10.dp))
            .background(if (seleccionado) Color(0xFF0A1931) else Color.Transparent)
            .selectable(selected = seleccionado, role = Role.Tab, onClick = seleccionar)
            .heightIn(min = 48.dp)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            texto,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = if (seleccionado) Color.White else colores.primary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun BarraPeriodo(
    periodo: PeriodoEvolucion,
    valor: Double?,
    maximo: Double,
    verCargo: Boolean,
    seleccionado: Boolean,
    espacioEtiqueta: Dp,
    onSeleccionar: () -> Unit
) {
    val colores = MaterialTheme.colorScheme
    val forma = RoundedCornerShape(8.dp)
    val etiqueta = valor?.let { if (verCargo) soles(it) else "${decimal(it)} m³" } ?: SIN_DATOS
    Column(
        Modifier.width(88.dp)
            .clip(forma)
            .background(if (seleccionado) colores.primaryContainer.copy(alpha = 0.55f) else Color.Transparent)
            .selectable(selected = seleccionado, role = Role.Tab, onClick = onSeleccionar)
            .semantics { contentDescription = "${periodo.mes}, $etiqueta" }
            .padding(vertical = MargenBarras),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(Modifier.height(AlturaBarras + espacioEtiqueta), contentAlignment = Alignment.BottomCenter) {
            if (valor == null) {
                Text("—", Modifier.clearAndSetSemantics {}, color = colores.onSurfaceVariant)
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        etiqueta,
                        Modifier.clearAndSetSemantics {}.padding(horizontal = 2.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = colores.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )
                    Box(
                        Modifier.width(24.dp)
                            .height((AlturaBarras * (valor / maximo).toFloat()).coerceAtLeast(2.dp))
                            .background(if (seleccionado) Color(0xFF0A1931) else Color(0xFFACCDF6), RoundedCornerShape(5.dp))
                    )
                }
            }
        }
        Text(
            "${periodo.mes.nombreCompleto().take(3)} ${periodo.mes.anio}",
            Modifier.clearAndSetSemantics {},
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (seleccionado) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun DetallePeriodo(periodo: PeriodoEvolucion, meta: Double?) {
    val recibo = periodo.recibo
    Column(
        Modifier.fillMaxWidth().padding(top = 4.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(periodo.mes.nombreCompleto(), fontWeight = FontWeight.Bold)
        if (recibo == null) {
            Text(SIN_DATOS)
        } else {
            Text("Consumo · ${decimal(recibo.metrosCubicos)} m³")
            if (meta != null) {
                Text(estadoMeta(recibo.metrosCubicos, meta))
            }
            Text("Cargo del mes · ${recibo.totalMes?.let(::soles) ?: SIN_DATOS}")
            recibo.totalPagar?.let { Text("Total a pagar · ${soles(it)}") }
        }
    }
}

internal fun soles(valor: Double): String = "S/ ${decimal(valor, minimo = 2, maximo = 2)}"

internal fun estadoMeta(consumo: Double, meta: Double): String {
    val diferencia = consumo - meta
    val cantidad = decimal(kotlin.math.abs(diferencia))
    return when {
        cantidad == decimal(0.0) -> "Alcanzaste tu meta de ${decimal(meta)} m³."
        diferencia > 0 -> "Superaste tu meta por $cantidad m³."
        else -> "$cantidad m³ por debajo de tu meta."
    }
}

@Composable
internal fun DialogoMeta(meta: Double?, promedio: Double, cerrar: () -> Unit, guardar: (Double?) -> Unit) {
    var texto by rememberSaveable { mutableStateOf(meta?.let { java.math.BigDecimal.valueOf(it).stripTrailingZeros().toPlainString() }.orEmpty()) }
    val numero = texto.trim().replace(',', '.')
    val valor = numero.takeIf { FORMATO_META.matches(it) }?.toDoubleOrNull()?.takeIf { it.isFinite() && it > 0 }
    AlertDialog(
        onDismissRequest = cerrar,
        title = { Text("Meta mensual de consumo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Tu promedio histórico es de ${decimal(promedio)} m³ al mes.")
                OutlinedTextField(
                    value = texto,
                    onValueChange = { texto = it },
                    singleLine = true,
                    label = { Text("Meta en m³") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = texto.isNotBlank() && valor == null,
                    supportingText = {
                        if (texto.isNotBlank() && valor == null) Text("Usa un valor positivo con hasta dos decimales.")
                    }
                )
                if (meta != null) TextButton(onClick = { guardar(null) }) { Text("Quitar meta") }
            }
        },
        confirmButton = {
            TextButton(enabled = valor != null, onClick = { valor?.let(guardar) }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = cerrar) { Text("Cancelar") } }
    )
}
