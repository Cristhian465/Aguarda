package com.example.aguardapp.feature.eps.presentation

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.aguardapp.R
import com.example.aguardapp.core.ui.theme.IconosClarosEnBarraDeEstado
import com.example.aguardapp.feature.deposito.presentation.componentes.BarraSuperior
import com.example.aguardapp.feature.deposito.presentation.componentes.TarjetaBlanca
import com.example.aguardapp.feature.eps.domain.MesRecibo
import com.example.aguardapp.feature.eps.domain.PeriodoEvolucion
import com.example.aguardapp.feature.eps.domain.ReciboGuardado
import com.example.aguardapp.feature.eps.domain.compararRecibos
import com.example.aguardapp.feature.eps.domain.periodosDeEvolucion
import java.io.File
import java.io.IOException
import java.text.NumberFormat
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun RecibosEpsScreen(
    onVolver: () -> Unit,
    viewModel: ReciboViewModel = koinViewModel(),
    epsViewModel: EpsViewModel = koinViewModel()
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val eps by epsViewModel.uiState.collectAsStateWithLifecycle()
    val historial = estado.historial
    val periodos = remember(historial) { periodosDeEvolucion(historial).asReversed() }
    val lista = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val elegirFoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { foto ->
        foto?.let { viewModel.onFotoElegida(it) }
    }

    val contexto = LocalContext.current
    val resolver = contexto.applicationContext.contentResolver
    val crearPdf = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { destino ->
        if (destino == null) epsViewModel.descartarPdf()
        else epsViewModel.guardarPdf { resolver.openOutputStream(destino, "wt") }
    }
    LaunchedEffect(eps.elegirDestinoPdf) {
        if (eps.elegirDestinoPdf) {
            try {
                crearPdf.launch("Recibo-EPS-Tacna.pdf")
                epsViewModel.selectorPdfAbierto()
            } catch (error: ActivityNotFoundException) {
                epsViewModel.descartarPdf("No hay un selector de archivos disponible para guardar el PDF.")
            }
        }
    }
    val carpetaFotos = File(contexto.cacheDir, "fotos_recibos")
    val autoridad = "${contexto.packageName}.recibos.camara"
    var nombreCaptura by rememberSaveable { mutableStateOf<String?>(null) }
    var errorCamara by rememberSaveable { mutableStateOf<String?>(null) }
    val capturarFoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { guardada ->
        val captura = nombreCaptura?.let { File(carpetaFotos, it) }
        nombreCaptura = null
        if (guardada && captura != null && captura.length() > 0) {
            viewModel.onFotoElegida(FileProvider.getUriForFile(contexto, autoridad, captura), captura)
        } else {
            captura?.delete()
            if (guardada) errorCamara = "La cámara no guardó la foto. Intenta nuevamente."
        }
    }
    val tomarFoto = {
        fun mostrarError(mensaje: String) {
            nombreCaptura?.let { File(carpetaFotos, it).delete() }
            nombreCaptura = null
            errorCamara = mensaje
        }
        errorCamara = null
        try {
            carpetaFotos.mkdirs()
            val captura = File.createTempFile("recibo-", ".jpg", carpetaFotos)
            nombreCaptura = captura.name
            capturarFoto.launch(FileProvider.getUriForFile(contexto, autoridad, captura))
        } catch (_: ActivityNotFoundException) {
            mostrarError("No hay una aplicación de cámara disponible.")
        } catch (_: SecurityException) {
            mostrarError("No se pudo abrir la cámara.")
        } catch (_: IOException) {
            mostrarError("No se pudo preparar la foto del recibo.")
        }
    }

    eps.pdfVisible?.let { VisorPdfEps(it, epsViewModel::cerrarVisorPdf) }

    IconosClarosEnBarraDeEstado(claros = false)
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        BarraSuperior("Recibos EPS", "EPS Tacna", onVolver)
        LazyColumn(Modifier.weight(1f), state = lista, contentPadding = PaddingValues(16.dp)) {
            item {
                TarjetaEps(historial.firstOrNull(), historial, estado.metaMensual, viewModel::guardarMeta, eps, epsViewModel)
            }
            if (historial.isNotEmpty()) {
                item {
                    Box(Modifier.padding(top = 14.dp)) { TarjetaEvolucion(historial, estado.metaMensual) }
                }
            }
            item {
                Column(Modifier.padding(top = 20.dp, bottom = 10.dp)) {
                    Text("Historial de recibos", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    val cantidad = historial.size
                    Text(
                        when (cantidad) {
                            0 -> "Actualiza desde EPS o añade tu primer recibo por foto."
                            1 -> "1 periodo registrado"
                            else -> "$cantidad periodos registrados"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            itemsIndexed(periodos, key = { _, periodo -> periodo.mes.toString() }) { indice, periodo ->
                val arriba = if (indice == 0) 16.dp else 0.dp
                val abajo = if (indice == periodos.lastIndex) 16.dp else 0.dp
                FilaPeriodo(periodo, estado.hoy, RoundedCornerShape(arriba, arriba, abajo, abajo), indice != periodos.lastIndex)
            }
            item {
                Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                errorCamara = null
                                elegirFoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            enabled = !estado.leyendo && !estado.revisando && nombreCaptura == null,
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                        ) { Text("Añadir por foto") }
                        OutlinedButton(
                            onClick = tomarFoto,
                            enabled = !estado.leyendo && !estado.revisando && nombreCaptura == null,
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                        ) { Text("Tomar foto") }
                    }
                    OutlinedButton(
                        onClick = {
                            if (eps.cuenta != null) epsViewModel.importar() else epsViewModel.mostrarFormulario(true, AccionRecibos.IMPORTAR)
                            scope.launch { lista.scrollToItem(0) }
                        },
                        enabled = !eps.trabajando && !eps.formulario,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    ) { Text("Importar historial") }
                    errorCamara?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    if (estado.leyendo) Text("Leyendo el recibo…")
                    if (estado.revisando) TarjetaBlanca { FormularioRecibo(estado, viewModel) }
                    estado.mensaje?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                    if (eps.cuenta != null) {
                        TextButton(onClick = epsViewModel::desvincular, enabled = !eps.trabajando) { Text("Desvincular cuenta EPS") }
                    }
                }
            }
            if (historial.size >= 2) item {
                Box(Modifier.padding(top = 12.dp)) { TarjetaComparacion(historial[0], historial[1]) }
            }
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
private fun FilaPeriodo(periodo: PeriodoEvolucion, hoy: LocalDate, forma: RoundedCornerShape, separador: Boolean) {
    val recibo = periodo.recibo
    var desplegado by rememberSaveable { mutableStateOf(false) }
    val colores = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().clip(forma).background(colores.surface)) {
        val accion = if (recibo == null) Modifier else Modifier
            .clickable(role = Role.Button, onClickLabel = if (desplegado) "Cerrar detalle" else "Ver detalle") {
                desplegado = !desplegado
            }
            .semantics { stateDescription = if (desplegado) "Expandido" else "Contraído" }
        Row(
            Modifier.fillMaxWidth().then(accion).heightIn(min = 64.dp).padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Box(
                Modifier.size(34.dp).clip(RoundedCornerShape(9.dp)).background(colores.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(painterResource(R.drawable.ic_recibo), contentDescription = null, tint = colores.primary, modifier = Modifier.size(19.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(periodo.mes.nombreCompleto(), fontWeight = FontWeight.SemiBold)
                Text(
                    recibo?.let { "${decimal(it.metrosCubicos)} m³" } ?: "Sin lectura registrada",
                    style = MaterialTheme.typography.bodySmall,
                    color = colores.onSurfaceVariant
                )
            }
            Text(recibo?.totalPagar?.let(::soles) ?: "—", style = MaterialTheme.typography.labelLarge)
            if (recibo != null) {
                Canvas(Modifier.size(12.dp)) {
                    val signo = if (desplegado) -1 else 1
                    val mitad = center.y - 2.dp.toPx() * signo
                    val punta = center.y + 2.dp.toPx() * signo
                    drawLine(colores.primary, Offset(0f, mitad), Offset(center.x, punta), strokeWidth = 1.5.dp.toPx())
                    drawLine(colores.primary, Offset(center.x, punta), Offset(size.width, mitad), strokeWidth = 1.5.dp.toPx())
                }
            }
        }
        if (desplegado && recibo != null) {
            Column(
                Modifier.fillMaxWidth().background(colores.surfaceVariant).padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                recibo.totalMes?.let { Text("Cargo del mes · ${soles(it)}") }
                recibo.deuda?.let { Text("Deuda · ${soles(it)}") }
                recibo.litrosPorDia?.let {
                    Text("${decimal(it, maximo = 1)} L/día · ${recibo.diasFacturados} días facturados")
                }
                Text(
                    if (recibo.leidoDeEps) "Obtenido de EPS Tacna" else "Añadido por foto",
                    style = MaterialTheme.typography.bodySmall
                )
                if (recibo.verificado(hoy)) Text("Verificado", color = colores.primary, style = MaterialTheme.typography.labelMedium)
            }
        }
        if (separador) HorizontalDivider(color = colores.surfaceVariant)
    }
}

internal fun MesRecibo.nombreCompleto(): String =
    Month.of(mes).getDisplayName(TextStyle.FULL, localeRecibos)
        .replaceFirstChar { it.titlecase(localeRecibos) } + " $anio"

internal fun describirCambio(valor: Double, prefijo: String = "", unidad: String = "", decimales: Int = 2): String {
    val magnitud = decimal(abs(valor), minimo = if (prefijo.isEmpty()) 0 else decimales, maximo = decimales)
    if (abs(valor) < 0.5 / Math.pow(10.0, decimales.toDouble())) return "Igual"
    return "$prefijo$magnitud$unidad ${if (valor > 0) "más" else "menos"}"
}

private val localeRecibos = Locale.forLanguageTag("es-PE")

internal fun decimal(valor: Double, minimo: Int = 0, maximo: Int = 2): String =
    NumberFormat.getNumberInstance(localeRecibos).apply {
        minimumFractionDigits = minimo
        maximumFractionDigits = maximo
    }.format(valor)
