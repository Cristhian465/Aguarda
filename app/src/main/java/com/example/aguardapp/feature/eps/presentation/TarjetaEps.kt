package com.example.aguardapp.feature.eps.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.aguardapp.core.ui.theme.Coral
import com.example.aguardapp.feature.deposito.presentation.componentes.BotonPrincipal
import com.example.aguardapp.feature.deposito.presentation.componentes.TarjetaBlanca
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun TarjetaEps(viewModel: EpsViewModel = koinViewModel()) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    TarjetaBlanca {
        Text("Tu recibo desde EPS Tacna", fontWeight = FontWeight.Bold)
        Text("Consulta tu recibo sin tomar fotos ni descargar archivos manualmente.")
        if (estado.cuenta != null) {
            Text("Cuenta vinculada: ${estado.cuenta!!.usuario}")
            BotonPrincipal("Actualizar recibo", viewModel::actualizar, habilitado = !estado.trabajando)
            TextButton(onClick = viewModel::desvincular, enabled = !estado.trabajando) { Text("Desvincular cuenta") }
        } else if (estado.formulario) {
            FormularioEps(estado.trabajando, viewModel::vincular) { viewModel.mostrarFormulario(false) }
        } else {
            BotonPrincipal("Vincular cuenta EPS", { viewModel.mostrarFormulario(true) }, habilitado = !estado.trabajando)
        }
        if (estado.trabajando) {
            CircularProgressIndicator()
            Text("Consultando EPS Tacna…")
        }
        estado.mensaje?.let { Text(it) }
        estado.error?.let { Text(it, color = Coral) }
    }
}

@Composable
private fun FormularioEps(trabajando: Boolean, onVincular: (String, String) -> Unit, onCancelar: () -> Unit) {
    var usuario by remember { mutableStateOf("") }
    var clave by remember { mutableStateOf("") }
    Column {
        Text("Usa el usuario y la clave que aparecen en tu recibo de EPS Tacna.")
        OutlinedTextField(usuario, { usuario = it }, label = { Text("Usuario EPS") },
            singleLine = true, enabled = !trabajando, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(clave, { clave = it }, label = { Text("Clave EPS") },
            singleLine = true, enabled = !trabajando, modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
        Text("EPS Tacna usa HTTP: tu usuario, clave y sesión viajan sin cifrado y podrían ser interceptados. Al vincular, aceptas enviarlos a su oficina virtual. Aguarda no guarda tu clave; conserva la sesión cifrada en el teléfono.")
        BotonPrincipal("Vincular cuenta", {
            onVincular(usuario, clave)
            clave = ""
        }, habilitado = !trabajando && usuario.isNotBlank() && clave.isNotBlank())
        TextButton(onClick = onCancelar, enabled = !trabajando) { Text("Cancelar") }
    }
}
