package com.example.aguardapp.feature.familia.presentation

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import org.koin.compose.viewmodel.koinViewModel
import com.example.aguardapp.core.ui.theme.AguaMedia
import com.example.aguardapp.core.ui.theme.Coral
import com.example.aguardapp.core.ui.theme.IconosClarosEnBarraDeEstado
import com.example.aguardapp.core.ui.theme.Tinta
import com.example.aguardapp.core.ui.theme.TintaSuave
import com.example.aguardapp.feature.deposito.presentation.componentes.BarraSuperior
import com.example.aguardapp.feature.deposito.presentation.componentes.BotonPrincipal
import com.example.aguardapp.feature.deposito.presentation.componentes.TarjetaBlanca
import com.example.aguardapp.feature.familia.domain.model.Invitacion
import com.example.aguardapp.feature.familia.domain.usecase.PREFIJO_QR
import com.example.aguardapp.feature.familia.domain.usecase.formatearCodigo

@Composable
fun FamiliaScreen(onVolver: () -> Unit, viewModel: FamiliaViewModel = koinViewModel()) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val contexto = LocalContext.current
    var confirmarUnion by rememberSaveable { mutableStateOf(false) }
    var confirmarSalida by rememberSaveable { mutableStateOf(false) }
    var confirmarReinicio by rememberSaveable { mutableStateOf(false) }

    IconosClarosEnBarraDeEstado(claros = false)
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        BarraSuperior("Mi familia", "Un tanque para todos los teléfonos de la casa", onVolver)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (estado.enFamilia) {
                TarjetaMiembros(estado, viewModel::sincronizar)
                TarjetaInvitar(estado, viewModel::invitar) { compartirCodigo(contexto, it) }
                TarjetaOpciones(
                    trabajando = estado.trabajando,
                    onSalir = { confirmarSalida = true },
                    onReiniciar = { confirmarReinicio = true }
                )
            } else {
                TarjetaQueEs()
                OutlinedTextField(
                    value = estado.apodo,
                    onValueChange = viewModel::onApodo,
                    label = { Text("Tu nombre en la familia") },
                    placeholder = { Text("Mamá, Luis, Abuela…") },
                    singleLine = true,
                    enabled = !estado.trabajando,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth()
                )
                TarjetaCrear(estado, viewModel::crear)
                TarjetaUnirse(
                    estado = estado,
                    onCodigo = viewModel::onCodigo,
                    onEscanear = { escanearQr(contexto, viewModel::onCodigoEscaneado, viewModel::mostrarError) },
                    onUnirse = { confirmarUnion = true }
                )
            }
            if (estado.trabajando) Trabajando()
            estado.mensaje?.let { Text(it, color = AguaMedia, fontWeight = FontWeight.SemiBold) }
            estado.error?.let { Text(it, color = Coral) }
        }
    }

    if (confirmarUnion) {
        Confirmacion(
            titulo = "¿Unirte a esta familia?",
            texto = "Los llenados y el perfil del depósito de este teléfono se reemplazarán por los de tu familia. No se puede deshacer.",
            accion = "Unirme",
            onConfirmar = { confirmarUnion = false; viewModel.unirse() },
            onCancelar = { confirmarUnion = false }
        )
    }
    if (confirmarSalida) {
        Confirmacion(
            titulo = "¿Salir de la familia?",
            texto = "Este teléfono dejará de compartir el tanque. Los datos del depósito se quedan en el teléfono.",
            accion = "Salir",
            onConfirmar = { confirmarSalida = false; viewModel.salir() },
            onCancelar = { confirmarSalida = false }
        )
    }
    if (confirmarReinicio) {
        Confirmacion(
            titulo = "¿Sacar a todos?",
            texto = "Los demás teléfonos saldrán de la familia y los códigos anteriores dejarán de servir. " +
                "El historial se conserva y tendrás que invitar de nuevo a quien corresponda.",
            accion = "Sacar a todos",
            onConfirmar = { confirmarReinicio = false; viewModel.reiniciar() },
            onCancelar = { confirmarReinicio = false }
        )
    }
}

@Composable
private fun TarjetaQueEs() {
    TarjetaBlanca {
        Titulo("Un tanque, toda la casa")
        Detalle(
            "Con el modo familia, cualquiera de la casa puede registrar un llenado o avisar que se acabó el agua, " +
                "y los demás teléfonos lo ven al sincronizar. No necesitas crear una cuenta."
        )
    }
}

@Composable
private fun TarjetaCrear(estado: FamiliaUiState, onCrear: () -> Unit) {
    TarjetaBlanca {
        Titulo("Crear la familia")
        Detalle("Se usan los datos del depósito de este teléfono. Luego invitas a los demás con un código.")
        BotonPrincipal("Crear mi familia", onCrear, Modifier.padding(top = 8.dp), habilitado = estado.puedeCrear)
    }
}

@Composable
private fun TarjetaUnirse(
    estado: FamiliaUiState,
    onCodigo: (String) -> Unit,
    onEscanear: () -> Unit,
    onUnirse: () -> Unit
) {
    TarjetaBlanca {
        Titulo("Unirme a una familia")
        Detalle("Pide el código a alguien de la familia: en su teléfono, Mi familia > Invitar a alguien.")
        OutlinedTextField(
            value = estado.codigo,
            onValueChange = onCodigo,
            label = { Text("Código de invitación") },
            placeholder = { Text("3F9A1-C0B7E") },
            singleLine = true,
            enabled = !estado.trabajando,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
        )
        OutlinedButton(
            onClick = onEscanear,
            enabled = !estado.trabajando,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(top = 6.dp)
        ) { Text("Escanear QR") }
        BotonPrincipal("Unirme", onUnirse, Modifier.padding(top = 8.dp), habilitado = estado.puedeUnirse)
        Detalle("Al unirte, los datos del depósito de este teléfono se reemplazan por los de tu familia.")
    }
}

@Composable
private fun TarjetaMiembros(estado: FamiliaUiState, onSincronizar: () -> Unit) {
    TarjetaBlanca {
        Titulo("Tu familia")
        if (estado.miembros.isEmpty()) {
            Detalle("Sincroniza para ver quiénes están.")
        } else {
            estado.miembros.forEach { miembro ->
                Text(
                    if (miembro.esPropio) "${miembro.apodo} (tú)" else miembro.apodo,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Tinta,
                    fontWeight = if (miembro.esPropio) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
        Detalle(describirSincronizacion(estado.ultimaSincronizacion, System.currentTimeMillis()))
        TextButton(onClick = onSincronizar, enabled = !estado.trabajando) { Text("Sincronizar ahora") }
    }
}

@Composable
private fun TarjetaInvitar(estado: FamiliaUiState, onInvitar: () -> Unit, onCompartir: (Invitacion) -> Unit) {
    val invitacion = estado.invitacion
    TarjetaBlanca {
        if (invitacion == null) {
            Titulo("Invitar a alguien")
            Detalle("Genera un código para otro teléfono de la casa. Vence en 15 minutos y sirve para un solo teléfono.")
            BotonPrincipal("Invitar a alguien", onInvitar, Modifier.padding(top = 8.dp), habilitado = !estado.trabajando)
        } else {
            Titulo("Código de invitación")
            Text(
                formatearCodigo(invitacion.codigo),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = AguaMedia
            )
            Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                CodigoQr(PREFIJO_QR + invitacion.codigo, Modifier.size(200.dp))
            }
            Detalle(
                "En el otro teléfono: Mi familia > Escanear QR, o escribe el código. " +
                    "Vence en ${invitacion.minutos} minutos y sirve para un solo teléfono."
            )
            BotonPrincipal("Compartir código", { onCompartir(invitacion) }, Modifier.padding(top = 8.dp))
            TextButton(onClick = onInvitar, enabled = !estado.trabajando) { Text("Generar otro código") }
        }
    }
}

@Composable
private fun TarjetaOpciones(trabajando: Boolean, onSalir: () -> Unit, onReiniciar: () -> Unit) {
    TarjetaBlanca {
        Titulo("Opciones")
        TextButton(onClick = onSalir, enabled = !trabajando) { Text("Salir de la familia") }
        TextButton(onClick = onReiniciar, enabled = !trabajando) { Text("Sacar a todos y crear un código nuevo", color = Coral) }
        Detalle("Úsalo si alguien dejó de vivir en la casa o perdió su teléfono.")
    }
}

@Composable
private fun Trabajando() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
        Text("Conectando con tu familia…", color = TintaSuave)
    }
}

@Composable
private fun Confirmacion(titulo: String, texto: String, accion: String, onConfirmar: () -> Unit, onCancelar: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(titulo) },
        text = { Text(texto) },
        confirmButton = { TextButton(onClick = onConfirmar) { Text(accion) } },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } }
    )
}

@Composable
private fun Titulo(texto: String) {
    Text(texto, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = Tinta)
}

@Composable
private fun Detalle(texto: String, color: Color = TintaSuave) {
    Text(texto, style = MaterialTheme.typography.bodyMedium, color = color)
}

/** Lector de QR de Google Play services: no necesita permiso de cámara. */
private fun escanearQr(contexto: Context, onCodigo: (String) -> Unit, onError: (String) -> Unit) {
    val opciones = GmsBarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
    GmsBarcodeScanning.getClient(contexto, opciones).startScan()
        .addOnSuccessListener { leido ->
            val texto = leido.rawValue
            if (texto != null) onCodigo(texto) else onError("No se pudo leer el QR. Escribe el código a mano")
        }
        .addOnFailureListener { onError("No se pudo abrir el lector de QR. Escribe el código a mano") }
}

private fun compartirCodigo(contexto: Context, invitacion: Invitacion) {
    val texto = "Únete a mi familia en Aguarda para compartir el tanque. Abre Aguarda > Mi familia y escribe el código " +
        "${formatearCodigo(invitacion.codigo)}. Vence en ${invitacion.minutos} minutos."
    val enviar = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, texto)
    }
    try {
        contexto.startActivity(Intent.createChooser(enviar, "Compartir código"))
    } catch (_: ActivityNotFoundException) {
        // Sin apps para compartir: el código sigue visible en pantalla.
    }
}
