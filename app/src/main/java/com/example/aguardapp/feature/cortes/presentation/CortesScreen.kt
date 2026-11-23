package com.example.aguardapp.feature.cortes.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.aguardapp.core.ui.theme.AguaMedia
import com.example.aguardapp.core.ui.theme.Coral
import com.example.aguardapp.core.ui.theme.Fondo
import com.example.aguardapp.core.ui.theme.TintaSuave
import com.example.aguardapp.feature.cortes.domain.CatalogoSectores
import com.example.aguardapp.feature.cortes.domain.CategoriaAviso
import com.example.aguardapp.feature.cortes.domain.CorteAgua
import com.example.aguardapp.feature.cortes.domain.PeriodoAviso
import com.example.aguardapp.feature.cortes.domain.SectorEps
import com.example.aguardapp.feature.deposito.presentation.componentes.BarraSuperior
import com.example.aguardapp.feature.deposito.presentation.componentes.TarjetaBlanca
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CortesScreen(onVolver: () -> Unit, viewModel: CortesViewModel = koinViewModel()) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    var mostrarAyuda by rememberSaveable { mutableStateOf(false) }
    if (mostrarAyuda) GuiaSectores(estado.catalogo) { mostrarAyuda = false }
    Column(Modifier.fillMaxSize().background(Fondo)) {
        BarraSuperior("Avisos EPS", "Avisos de agua y saneamiento", onVolver)
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                FiltrosCortes(
                    estado, viewModel::elegirSector, viewModel::elegirCategoria, viewModel::elegirPeriodo,
                    viewModel::escribirBusqueda, viewModel::actualizar, { mostrarAyuda = true }
                )
            }
            if (!estado.apiConfigurada) {
                item { TarjetaBlanca { Text("El servicio de cortes aún no está configurado.") } }
            } else {
                if (estado.cargando) item {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                }
                if (estado.error != null) item {
                    TarjetaBlanca {
                        Text(estado.error.orEmpty(), color = Coral)
                        if (estado.cortes.isNotEmpty()) DetalleCorte("Se muestran los últimos avisos consultados.")
                        TextButton(onClick = viewModel::actualizar) { Text("Reintentar") }
                    }
                }
                if (!estado.cargando && estado.error == null && estado.cortes.isEmpty()) item {
                    TarjetaBlanca {
                        Text("No se encontraron avisos con estos filtros.")
                    }
                }
                items(estado.cortes, key = { it.id }) { aviso -> TarjetaCorte(aviso, estado.catalogo.sectores) }
                if (estado.siguienteDesde != null) item {
                    TextButton(onClick = viewModel::cargarMas, enabled = !estado.cargando) { Text("Ver más avisos") }
                }
            }
        }
    }
}

@Composable
private fun FiltrosCortes(
    estado: CortesUiState,
    onSector: (String?) -> Unit,
    onCategoria: (CategoriaAviso?) -> Unit,
    onPeriodo: (PeriodoAviso) -> Unit,
    onBuscar: (String) -> Unit,
    onActualizar: () -> Unit,
    onAyuda: () -> Unit
) {
    val filtros = estado.filtros
    val sectores = estado.catalogo.sectores
    TarjetaBlanca {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Sector EPS", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            IconButton(onClick = onAyuda, modifier = Modifier.semantics { contentDescription = "Ver distritos y zonas de cada sector" }) {
                Text("?", color = AguaMedia, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        }
        SelectorAvisos(
            sectores.firstOrNull { it.id == filtros.sector }?.etiqueta ?: "Todos los sectores",
            listOf(null to "Todos los sectores") + sectores.map { it.id to it.etiqueta },
            onSector
        )
        Text("Tipo de aviso", style = MaterialTheme.typography.labelLarge)
        SelectorAvisos(
            filtros.categoria?.etiqueta ?: "Todos los tipos",
            listOf(null to "Todos los tipos") + CategoriaAviso.entries.map { it to it.etiqueta },
            onCategoria
        )
        Text("Periodo", style = MaterialTheme.typography.labelLarge)
        SelectorAvisos(filtros.periodo.etiqueta, PeriodoAviso.entries.map { it to it.etiqueta }, onPeriodo)
        OutlinedTextField(
            value = filtros.buscar,
            onValueChange = onBuscar,
            label = { Text("Buscar en los avisos") },
            placeholder = { Text("Calle, asociación o distrito") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        TextButton(onClick = onActualizar, enabled = estado.apiConfigurada && !estado.cargando) { Text("Actualizar avisos") }
    }
}

@Composable
private fun <T> SelectorAvisos(etiqueta: String, opciones: List<Pair<T, String>>, alElegir: (T) -> Unit) {
    var abierto by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = { abierto = true }, modifier = Modifier.fillMaxWidth()) { Text(etiqueta) }
        DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            opciones.forEach { (valor, nombre) ->
                DropdownMenuItem(text = { Text(nombre) }, onClick = {
                    abierto = false
                    alElegir(valor)
                })
            }
        }
    }
}

@Composable
private fun GuiaSectores(catalogo: CatalogoSectores, onCerrar: () -> Unit) {
    val sinSector = catalogo.distritos.filter { distrito -> catalogo.sectores.none { distrito in it.distritos } }
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Sectores de distribución") },
        text = {
            Column(Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Cada sector abarca zonas de los distritos indicados. Consulta las referencias antes de elegir el tuyo.")
                catalogo.sectores.forEach { sector ->
                    Column {
                        Text(sector.etiqueta, fontWeight = FontWeight.Bold)
                        DetalleCorte(sector.distritos.joinToString())
                        DetalleCorte("Subsectores: ${sector.subsectores.joinToString { it.toString().padStart(2, '0') }}")
                        DetalleCorte("Zonas de referencia: ${sector.zonas.joinToString()}")
                    }
                }
                Text("También puedes buscar por ${sinSector.joinToString()} desde Todos los sectores.")
            }
        },
        confirmButton = { TextButton(onClick = onCerrar) { Text("Cerrar") } }
    )
}

@Composable
private fun TarjetaCorte(corte: CorteAgua, sectores: List<SectorEps>) {
    val navegador = LocalUriHandler.current
    var errorAlAbrir by remember(corte.url) { mutableStateOf(false) }
    var textoCompleto by rememberSaveable(corte.id) { mutableStateOf(false) }
    TarjetaBlanca {
        Text(
            CategoriaAviso.entries.firstOrNull { it.name == corte.categoria }?.etiqueta ?: "Comunicado",
            color = if (corte.tipo == "IMPREVISTO") Coral else AguaMedia,
            fontWeight = FontWeight.Bold
        )
        Text(corte.titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (corte.distritos.isNotEmpty()) DetalleCorte(corte.distritos.joinToString())
        val zonas = sectores.filter { it.id in corte.sectores }.joinToString { it.etiqueta }
        if (zonas.isNotEmpty()) {
            DetalleCorte(if (corte.sectorOrigen == "REFERENCIA") "Sector por zona de referencia: $zonas" else "Sector EPS: $zonas")
        }
        corte.sector?.let { DetalleCorte(it) }
        val detalle = corte.detalle?.takeUnless { it.equals(corte.titulo, ignoreCase = true) }
        if (!detalle.isNullOrBlank()) {
            DetalleCorte(if (textoCompleto || detalle.length <= 300) detalle else detalle.take(300) + "…")
            if (detalle.length > 300) {
                TextButton(onClick = { textoCompleto = !textoCompleto }) {
                    Text(if (textoCompleto) "Ver menos" else "Leer aviso completo")
                }
            }
        }
        if (corte.categoria == "CORTE") {
            when (corte.tipo) {
                "PROGRAMADO" -> DetalleCorte("Corte programado")
                "IMPREVISTO" -> DetalleCorte("Corte imprevisto")
            }
        }
        corte.motivo?.let { DetalleCorte("Motivo: $it") }
        val inicio = formatearFechaDeCorte(corte.inicio)
        val reposicion = formatearFechaDeCorte(corte.reposicionEstimada)
        val restablecido = formatearFechaDeCorte(corte.restablecido)
        if (inicio != null || corte.categoria == "CORTE") DetalleCorte("Inicio: ${inicio ?: "Por confirmar"}")
        if (reposicion != null || (restablecido == null && corte.categoria == "CORTE")) {
            DetalleCorte("Reposición estimada: ${reposicion ?: "Por confirmar"}")
        }
        restablecido?.let { DetalleCorte("Restablecido: $it") }
        val publicado = formatearFechaDeCorte(corte.publicado)
        DetalleCorte(listOfNotNull(corte.fuente, publicado?.let { "Publicado el $it" }).joinToString(" · "))
        TextButton(onClick = {
            errorAlAbrir = runCatching { navegador.openUri(corte.url) }.isFailure
        }) { Text("Ver publicación original") }
        if (errorAlAbrir) DetalleCorte("No se pudo abrir el enlace.")
    }
}

@Composable
private fun DetalleCorte(texto: String) {
    Text(texto, style = MaterialTheme.typography.bodyMedium, color = TintaSuave)
}

internal fun formatearFechaDeCorte(valor: String?): String? {
    val fecha = valor?.take(10)?.split("-") ?: return null
    if (fecha.size != 3 || fecha.any { it.toIntOrNull() == null }) return null
    val dia = "${fecha[2]}/${fecha[1]}/${fecha[0]}"
    val hora = valor.substringAfter('T', "").take(5)
    return if (hora.isEmpty()) dia else "$dia a las $hora"
}
