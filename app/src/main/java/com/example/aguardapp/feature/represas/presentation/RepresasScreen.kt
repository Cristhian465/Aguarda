package com.example.aguardapp.feature.represas.presentation

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import com.example.aguardapp.R
import com.example.aguardapp.core.ui.theme.Agua
import com.example.aguardapp.core.ui.theme.AguaMedia
import com.example.aguardapp.core.ui.theme.Blanco
import com.example.aguardapp.core.ui.theme.Divisor
import com.example.aguardapp.core.ui.theme.FuenteTexto
import com.example.aguardapp.core.ui.theme.IconosClarosEnBarraDeEstado
import com.example.aguardapp.core.ui.theme.Ocre
import com.example.aguardapp.core.ui.theme.Tenue
import com.example.aguardapp.core.ui.theme.Tinta
import com.example.aguardapp.core.ui.theme.TintaSuave
import com.example.aguardapp.core.ui.theme.TintaTenue
import com.example.aguardapp.feature.deposito.presentation.componentes.AvisoDeError
import com.example.aguardapp.feature.deposito.presentation.componentes.BarraSuperior
import com.example.aguardapp.feature.deposito.presentation.componentes.BotonPrincipal
import com.example.aguardapp.feature.deposito.presentation.componentes.BotonSecundario
import com.example.aguardapp.feature.deposito.presentation.componentes.TarjetaBlanca
import com.example.aguardapp.feature.deposito.presentation.formatearMiles
import com.example.aguardapp.feature.represas.domain.model.Distrito
import com.example.aguardapp.feature.represas.domain.model.DistritoDelHogar
import com.example.aguardapp.feature.represas.domain.model.Presa
import com.example.aguardapp.feature.represas.domain.model.RecorridoDelAgua
import com.example.aguardapp.feature.represas.domain.model.TramoDelRecorrido
import com.example.aguardapp.feature.represas.domain.model.UsoPresa

private val MARGEN = Modifier.padding(horizontal = 24.dp)
private const val PRESAS_SIN_EXPANDIR = 4
private val PERMISOS_UBICACION = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

/** Pestaña Represas: arriba el recorrido del agua del distrito del hogar; abajo, todas las represas de Tacna. */
@Composable
fun RepresasScreen(onAbrirPresa: (String) -> Unit, viewModel: RepresasViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var eligiendoDistrito by rememberSaveable { mutableStateOf(false) }
    val pedirPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { resultado ->
        if (resultado.values.any { it }) viewModel.onUsarMiUbicacion() else viewModel.onPermisoDenegado()
    }
    val usarMiUbicacion = {
        eligiendoDistrito = false
        pedirPermiso.launch(PERMISOS_UBICACION)
    }

    IconosClarosEnBarraDeEstado(claros = false)
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        BarraSuperior("¿De dónde viene tu agua?", "Represas de Tacna · datos de la ANA", onVolver = null)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            uiState.mensaje?.let { AvisoDeError(it, viewModel::onCerrarMensaje, MARGEN) }
            val error = uiState.errorDeDescarga
            if (error != null && uiState.presas.isNotEmpty()) {
                AvisoDatosGuardados(error, uiState.actualizado, viewModel::onReintentar)
            }

            val distrito = uiState.distrito
            val recorrido = uiState.recorrido
            if (distrito == null || recorrido == null) {
                PedirDistrito(uiState.buscandoUbicacion, usarMiUbicacion) { eligiendoDistrito = true }
            } else {
                LineaDelDistrito(distrito, uiState.buscandoUbicacion) { eligiendoDistrito = true }
                Recorrido(recorrido, uiState.presas, uiState.capacidadTanqueLitros, onAbrirPresa)
            }

            Titulo("Todas las represas de Tacna")
            Filtros(uiState.filtro, viewModel::onFiltrar)
            when {
                uiState.cargando -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                uiState.presas.isEmpty() -> SinRepresas(error, viewModel::onReintentar)
                else -> {
                    MapaDeRepresas(uiState.presasVisibles, recorrido, onAbrirPresa, MARGEN)
                    Leyenda(conDistrito = recorrido?.presas?.isNotEmpty() == true)
                    ListaDePresas(uiState.presasVisibles, recorrido, expandirSiempre = uiState.filtro != FiltroUso.TODAS, onAbrirPresa)
                }
            }
            Text(
                "Recorrido aproximado según EPS Tacna y prensa. Represas: Autoridad Nacional del Agua (ANA)" +
                    (uiState.actualizado?.let { ", datos de $it." } ?: "."),
                MARGEN, fontFamily = FuenteTexto, fontSize = 11.sp, color = TintaTenue
            )
        }
    }

    if (eligiendoDistrito) {
        ElegirDistrito(
            onElegir = { eligiendoDistrito = false; viewModel.onElegirDistrito(it) },
            onUsarMiUbicacion = usarMiUbicacion,
            onCerrar = { eligiendoDistrito = false }
        )
    }
}

@Composable
private fun PedirDistrito(buscando: Boolean, onUsarMiUbicacion: () -> Unit, onElegir: () -> Unit) {
    TarjetaBlanca(MARGEN) {
        Text("¿En qué distrito vives?", fontFamily = FuenteTexto, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Tinta)
        Text(
            "Con tu distrito te mostramos de qué represa o pozo viene tu agua.",
            fontFamily = FuenteTexto, fontSize = 13.sp, color = TintaSuave
        )
        Spacer(Modifier.size(8.dp))
        BotonPrincipal(if (buscando) "Buscando tu ubicación…" else "Usar mi ubicación", onUsarMiUbicacion, habilitado = !buscando)
        Spacer(Modifier.size(4.dp))
        BotonSecundario("Elegir de la lista", onElegir, color = AguaMedia)
    }
}

@Composable
private fun LineaDelDistrito(distrito: DistritoDelHogar, buscando: Boolean, onCambiar: () -> Unit) {
    Row(MARGEN.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(painterResource(R.drawable.ic_mapa), contentDescription = null, Modifier.size(16.dp), tint = TintaSuave)
        val origen = when {
            buscando -> "buscando tu ubicación…"
            distrito.detectadoPorGps -> "detectado por GPS"
            else -> "elegido por ti"
        }
        Text(
            "${distrito.distrito.nombre} · $origen",
            Modifier.weight(1f).padding(start = 6.dp),
            fontFamily = FuenteTexto, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TintaSuave
        )
        TextButton(onCambiar) { Text("Cambiar", fontFamily = FuenteTexto, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AguaMedia) }
    }
}

@Composable
private fun Recorrido(recorrido: RecorridoDelAgua, presas: List<Presa>, capacidadTanque: Double?, onAbrirPresa: (String) -> Unit) {
    val tanque = capacidadTanque?.let { TramoDelRecorrido("Tu tanque", "${formatearMiles(it.toInt())} L de capacidad") }
    val tramos = recorrido.tramos + listOfNotNull(tanque)
    TarjetaBlanca(MARGEN) {
        Text("Tu recorrido", Modifier.padding(bottom = 10.dp), fontFamily = FuenteTexto, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Tinta)
        tramos.forEachIndexed { i, tramo ->
            Tramo(tramo, ultimo = i == tramos.lastIndex) {
                val conDatos = tramo.presas.map { nombre -> nombre to presas.firstOrNull { it.nombre.equals(nombre, ignoreCase = true) } }
                if (conDatos.isNotEmpty()) {
                    Row(Modifier.padding(top = 6.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        conDatos.forEach { (nombre, presa) ->
                            val volumen = presa?.volumenUtilHm3?.let { " · ${formatearDecimal(it)} hm³" } ?: ""
                            Etiqueta("$nombre$volumen", fondo = Tenue, colorTexto = AguaMedia, onClick = { onAbrirPresa(presa?.nombre ?: nombre) })
                        }
                    }
                }
            }
        }
    }
    val sinRepresas = recorrido.presas.isEmpty() && recorrido.confirmado
    Text(
        recorrido.aviso,
        MARGEN.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(if (sinRepresas) Tenue else Ocre.copy(alpha = 0.16f)).padding(14.dp),
        fontFamily = FuenteTexto, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = if (sinRepresas) AguaMedia else Tinta
    )
}

// Un punto de la línea del recorrido. La línea que lo une con el siguiente se dibuja detrás.
@Composable
private fun Tramo(tramo: TramoDelRecorrido, ultimo: Boolean, extra: @Composable () -> Unit) {
    val colorDelPunto = if (tramo.porConfirmar) Ocre else Agua
    Row(
        Modifier.fillMaxWidth().drawBehind {
            if (!ultimo) {
                val x = 7.dp.toPx()
                drawLine(Divisor, Offset(x, 16.dp.toPx()), Offset(x, size.height + 2.dp.toPx()), strokeWidth = 2.dp.toPx())
            }
        }
    ) {
        Box(Modifier.padding(top = 3.dp).size(14.dp).clip(CircleShape).background(Blanco).border(3.dp, colorDelPunto, CircleShape))
        Column(Modifier.weight(1f).padding(start = 12.dp, bottom = if (ultimo) 0.dp else 14.dp)) {
            Text(tramo.titulo, fontFamily = FuenteTexto, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Tinta)
            tramo.detalle?.let {
                Text(it, fontFamily = FuenteTexto, fontSize = 12.5.sp, fontWeight = FontWeight.Medium, color = if (tramo.porConfirmar) Ocre else TintaSuave)
            }
            extra()
        }
    }
}

@Composable
private fun AvisoDatosGuardados(error: String, actualizado: String?, onReintentar: () -> Unit) {
    Row(
        MARGEN.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Ocre.copy(alpha = 0.16f)).padding(start = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "$error. Mostramos los datos guardados" + (actualizado?.let { " ($it)" } ?: "") + ".",
            Modifier.weight(1f).padding(vertical = 10.dp),
            fontFamily = FuenteTexto, fontSize = 12.5.sp, fontWeight = FontWeight.Medium, color = Tinta
        )
        TextButton(onReintentar) { Text("Reintentar", fontFamily = FuenteTexto, fontWeight = FontWeight.Bold, color = AguaMedia) }
    }
}

@Composable
private fun SinRepresas(error: String?, onReintentar: () -> Unit) {
    TarjetaBlanca(MARGEN) {
        Text(error ?: "Aún no hay datos de represas", fontFamily = FuenteTexto, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Tinta)
        Text("Se necesita internet la primera vez; luego se ven sin conexión.", fontFamily = FuenteTexto, fontSize = 12.sp, color = TintaSuave)
        TextButton(onReintentar) { Text("Reintentar", fontFamily = FuenteTexto, fontWeight = FontWeight.Bold, color = AguaMedia) }
    }
}

@Composable
private fun Titulo(texto: String) {
    Text(texto, MARGEN.padding(top = 8.dp), fontFamily = FuenteTexto, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Tinta)
}

@Composable
private fun Filtros(actual: FiltroUso, onFiltrar: (FiltroUso) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FiltroUso.entries.forEach { filtro ->
            FilterChip(
                selected = filtro == actual,
                onClick = { onFiltrar(filtro) },
                label = { Text(filtro.etiqueta, fontFamily = FuenteTexto, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Blanco,
                    labelColor = TintaSuave,
                    selectedContainerColor = Tenue,
                    selectedLabelColor = AguaMedia
                )
            )
        }
    }
}

@Composable
private fun Leyenda(conDistrito: Boolean) {
    Row(MARGEN.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        ElementoDeLeyenda("Agua potable", Agua)
        ElementoDeLeyenda("Riego", Ocre)
        ElementoDeLeyenda("Minería", colorDelUso(UsoPresa.MINERIA))
        if (conDistrito) ElementoDeLeyenda("Abastece a tu distrito", Blanco, borde = Tinta)
    }
}

@Composable
private fun ElementoDeLeyenda(texto: String, color: Color, borde: Color? = null) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(10.dp).clip(CircleShape).background(color)
                .then(if (borde != null) Modifier.border(2.dp, borde, CircleShape) else Modifier)
        )
        Text(texto, Modifier.padding(start = 5.dp), fontFamily = FuenteTexto, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = TintaSuave)
    }
}

@Composable
private fun ListaDePresas(presas: List<Presa>, recorrido: RecorridoDelAgua?, expandirSiempre: Boolean, onAbrirPresa: (String) -> Unit) {
    var expandida by rememberSaveable { mutableStateOf(false) }
    val visibles = if (expandirSiempre || expandida) presas else presas.take(PRESAS_SIN_EXPANDIR)
    TarjetaBlanca(MARGEN, padding = 6) {
        visibles.forEachIndexed { i, presa ->
            if (i > 0) HorizontalDivider(color = Divisor)
            FilaDePresa(presa, recorrido?.abasteceDesde(presa.nombre) == true) { onAbrirPresa(presa.nombre) }
        }
        if (presas.isEmpty()) {
            Text("Ninguna represa con este uso", Modifier.padding(12.dp), fontFamily = FuenteTexto, fontSize = 13.sp, color = TintaSuave)
        }
        if (!expandirSiempre && presas.size > PRESAS_SIN_EXPANDIR) {
            TextButton({ expandida = !expandida }, Modifier.fillMaxWidth()) {
                Text(
                    if (expandida) "Ver menos" else "Ver las ${presas.size} represas",
                    fontFamily = FuenteTexto, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AguaMedia
                )
            }
        }
    }
}

@Composable
private fun FilaDePresa(presa: Presa, deTuDistrito: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(role = Role.Button, onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(colorDelUso(presa.usoPrincipal)))
        Column(Modifier.weight(1f).padding(start = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(presa.nombre, fontFamily = FuenteTexto, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Tinta)
                if (deTuDistrito) {
                    Spacer(Modifier.width(6.dp))
                    Etiqueta("Tu distrito", fondo = Tenue, colorTexto = AguaMedia)
                }
            }
            val lugar = listOfNotNull(presa.rio, presa.distrito).joinToString(" · ")
            if (lugar.isNotEmpty()) Text(lugar, fontFamily = FuenteTexto, fontSize = 12.sp, color = TintaSuave)
        }
        Column(horizontalAlignment = Alignment.End) {
            val volumen = presa.volumenUtilHm3
            Text(
                volumen?.let { "${formatearDecimal(it)} hm³" } ?: "Sin dato",
                fontFamily = FuenteTexto, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (volumen == null) TintaTenue else Tinta
            )
            if (volumen != null) Text("útil", fontFamily = FuenteTexto, fontSize = 11.sp, color = TintaTenue)
        }
    }
}

/** Texto en una píldora de color; con [onClick] se puede tocar. También la usa el detalle de la represa. */
@Composable
internal fun Etiqueta(texto: String, fondo: Color, colorTexto: Color, onClick: (() -> Unit)? = null) {
    Text(
        texto,
        Modifier.clip(RoundedCornerShape(50)).background(fondo)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        fontFamily = FuenteTexto, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colorTexto
    )
}

@Composable
private fun ElegirDistrito(onElegir: (Distrito) -> Unit, onUsarMiUbicacion: () -> Unit, onCerrar: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCerrar,
        confirmButton = { TextButton(onCerrar) { Text("Cancelar", fontFamily = FuenteTexto, color = AguaMedia) } },
        title = { Text("¿En qué distrito vives?", fontFamily = FuenteTexto, fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                TextButton(onUsarMiUbicacion) {
                    Text("Usar mi ubicación", fontFamily = FuenteTexto, fontWeight = FontWeight.Bold, color = AguaMedia)
                }
                Distrito.entries.forEach { distrito ->
                    Text(
                        distrito.nombre,
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable(role = Role.Button) { onElegir(distrito) }
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        fontFamily = FuenteTexto, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Tinta
                    )
                }
            }
        }
    )
}
