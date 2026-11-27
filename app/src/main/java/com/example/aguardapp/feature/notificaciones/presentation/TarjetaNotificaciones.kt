package com.example.aguardapp.feature.notificaciones.presentation

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import org.koin.compose.viewmodel.koinViewModel
import com.example.aguardapp.R
import com.example.aguardapp.core.ui.theme.AguaClara
import com.example.aguardapp.core.ui.theme.AguaMedia
import com.example.aguardapp.core.ui.theme.Coral
import com.example.aguardapp.core.ui.theme.TintaSuave
import com.example.aguardapp.feature.deposito.presentation.componentes.BotonPrincipal
import com.example.aguardapp.feature.deposito.presentation.componentes.BotonSecundario
import com.example.aguardapp.feature.deposito.presentation.componentes.TarjetaBlanca

@Composable
fun TarjetaNotificaciones(viewModel: NotificacionesViewModel = koinViewModel()) {
    val context = LocalContext.current
    var estado by remember { mutableStateOf(evaluarPermiso(context, viewModel.permisoSolicitado)) }
    LifecycleResumeEffect(Unit) {
        estado = evaluarPermiso(context, viewModel.permisoSolicitado)
        onPauseOrDispose { }
    }
    val solicitar = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        viewModel.onPermisoSolicitado()
        estado = evaluarPermiso(context, solicitadoAntes = true)
    }

    TarjetaBlanca {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(AguaClara), contentAlignment = Alignment.Center) {
                    Icon(painterResource(R.drawable.ic_campana), null, tint = AguaMedia)
                }
                Column {
                    Text("Avisos en tu teléfono", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        estado.etiqueta,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (estado == EstadoPermiso.CONCEDIDO) AguaMedia else Coral
                    )
                }
            }
            Text(
                "Te avisamos aunque la app esté cerrada: cuando vuelve el agua, cuando tu tanque se llena o baja al 20 %, cuando sale un aviso nuevo y para que no pierdas tu racha.",
                style = MaterialTheme.typography.bodyMedium,
                color = TintaSuave
            )
            when (estado) {
                EstadoPermiso.CONCEDIDO -> BotonSecundario("Probar notificación", viewModel::onProbar, color = AguaMedia)
                EstadoPermiso.SIN_SOLICITAR, EstadoPermiso.EXPLICACION -> {
                    if (estado == EstadoPermiso.EXPLICACION) {
                        Text(
                            "Sin este permiso Aguarda no puede avisarte de un corte o de un tanque casi vacío fuera de la app.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Coral
                        )
                    }
                    BotonPrincipal("Activar notificaciones", {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) solicitar.launch(Manifest.permission.POST_NOTIFICATIONS)
                    })
                }
                EstadoPermiso.BLOQUEADO -> BotonPrincipal("Abrir ajustes", { abrirAjustes(context) })
            }
        }
    }
}

private fun evaluarPermiso(context: Context, solicitadoAntes: Boolean): EstadoPermiso {
    val pideEnTiempoDeEjecucion = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    val permisoOtorgado = !pideEnTiempoDeEjecucion ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    val concedido = permisoOtorgado && NotificationManagerCompat.from(context).areNotificationsEnabled()
    val debeExplicar = pideEnTiempoDeEjecucion && context.actividad()?.let {
        ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.POST_NOTIFICATIONS)
    } == true
    return estadoPermiso(concedido, debeExplicar, solicitadoAntes, puedeSolicitar = pideEnTiempoDeEjecucion && !permisoOtorgado)
}

private fun abrirAjustes(context: Context) {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
    }
    context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

private fun Context.actividad(): Activity? {
    var actual: Context? = this
    while (actual is ContextWrapper) {
        if (actual is Activity) return actual
        actual = actual.baseContext
    }
    return null
}
