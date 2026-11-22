package com.example.aguardapp.feature.eps.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.aguardapp.core.ui.theme.Coral
import com.example.aguardapp.feature.deposito.presentation.componentes.BotonPrincipal
import com.example.aguardapp.feature.deposito.presentation.componentes.TarjetaBlanca
import com.example.aguardapp.feature.eps.domain.ReciboGuardado

private val AzulRecibo = Color(0xFF0A1931)
private val AzulMeta = Color(0xFF102947)
private val AzulClaro = Color(0xFFB3CFE5)

@Composable
internal fun TarjetaEps(
    recibo: ReciboGuardado?,
    historial: List<ReciboGuardado>,
    meta: Double?,
    guardarMeta: (Double?) -> Unit,
    estado: EpsUiState,
    viewModel: EpsViewModel
) {
    var editarMeta by rememberSaveable { mutableStateOf(false) }
    if (editarMeta && recibo != null) {
        DialogoMeta(meta, historial.map { it.metrosCubicos }.average(), { editarMeta = false }) {
            guardarMeta(it)
            editarMeta = false
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(AzulRecibo).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                recibo?.let { "Último recibo · ${it.mes.nombreCompleto()}" } ?: "Último recibo",
                color = AzulClaro,
                style = MaterialTheme.typography.labelLarge
            )
            if (recibo != null) {
                Text("Total a pagar", color = AzulClaro, style = MaterialTheme.typography.bodySmall)
                Text(
                    recibo.totalPagar?.let(::soles) ?: "No registrado",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                MetaIntegrada(recibo.metrosCubicos, meta, onEditar = { editarMeta = true })
            } else {
                Text("Aún no tienes recibos guardados.", color = Color.White)
            }
            if (!estado.formulario) {
                Button(
                    onClick = {
                        if (estado.cuenta != null) viewModel.actualizar() else viewModel.mostrarFormulario(true)
                    },
                    enabled = !estado.trabajando,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AzulClaro, contentColor = AzulRecibo)
                ) {
                    Text(when {
                        estado.cuenta != null -> "Actualizar recibo EPS"
                        estado.reconectar != null -> "Volver a vincular"
                        else -> "Vincular cuenta EPS"
                    })
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val habilitado = !estado.trabajando && !estado.elegirDestinoPdf
                    TextButton(onClick = viewModel::verPdf, enabled = habilitado, modifier = Modifier.weight(1f)) {
                        Text(AccionRecibos.VER_PDF.boton, color = AzulClaro)
                    }
                    TextButton(onClick = viewModel::descargarPdf, enabled = habilitado, modifier = Modifier.weight(1f)) {
                        Text(AccionRecibos.PDF.boton, color = AzulClaro)
                    }
                }
            }
        }
        if (estado.formulario || estado.trabajando || estado.mensaje != null || estado.error != null) {
            TarjetaBlanca {
                estado.error?.let { Text(it, color = Coral) }
                if (estado.formulario) {
                    FormularioEps(
                        usuarioInicial = estado.reconectar?.usuario ?: estado.usuarioRecordado,
                        accionPendiente = estado.accionPendiente,
                        trabajando = estado.trabajando,
                        onVincular = viewModel::vincular,
                        onCancelar = { viewModel.mostrarFormulario(false) }
                    )
                }
                if (estado.trabajando) {
                    CircularProgressIndicator()
                    Text("Consultando EPS Tacna…")
                }
                estado.mensaje?.let { Text(it) }
            }
        }
    }
}

@Composable
private fun MetaIntegrada(consumo: Double, meta: Double?, onEditar: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(AzulMeta).padding(horizontal = 12.dp, vertical = 5.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Meta de consumo", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            TextButton(onClick = onEditar) { Text(if (meta == null) "Definir" else "Editar", color = AzulClaro) }
        }
        Text(
            if (meta == null) "${decimal(consumo)} m³ consumidos" else "${decimal(consumo)} de ${decimal(meta)} m³",
            color = Color.White,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        if (meta != null) {
            LinearProgressIndicator(
                progress = { (consumo / meta).coerceIn(0.0, 1.0).toFloat() },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(4.dp)),
                color = AzulClaro,
                trackColor = Color(0xFF355375)
            )
            Text(estadoMeta(consumo, meta), color = AzulClaro, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun FormularioEps(
    usuarioInicial: String,
    accionPendiente: AccionRecibos?,
    trabajando: Boolean,
    onVincular: (String, String) -> Unit,
    onCancelar: () -> Unit
) {
    var usuario by remember(usuarioInicial) { mutableStateOf(usuarioInicial) }
    var clave by remember { mutableStateOf("") }
    Column {
        if (usuarioInicial.isBlank()) {
            Text("Usa el usuario y la clave que aparecen en tu recibo de EPS Tacna.")
        }
        OutlinedTextField(usuario, { usuario = it }, label = { Text("Usuario EPS") },
            singleLine = true, enabled = !trabajando, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(clave, { clave = it }, label = { Text("Clave EPS") },
            singleLine = true, enabled = !trabajando, modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
        Text("EPS Tacna usa HTTP: tu usuario, clave y sesión viajan sin cifrado y podrían ser interceptados. Al vincular, aceptas enviarlos a su oficina virtual. Aguarda no guarda tu clave; conserva la sesión cifrada en el teléfono.")
        BotonPrincipal(accionPendiente?.alReconectar ?: "Vincular cuenta", {
            onVincular(usuario, clave)
            clave = ""
        }, habilitado = !trabajando && usuario.isNotBlank() && clave.isNotBlank())
        TextButton(onClick = onCancelar, enabled = !trabajando) { Text("Cancelar") }
    }
}
