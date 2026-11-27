package com.example.aguardapp.feature.notificaciones.data

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.aguardapp.MainActivity
import com.example.aguardapp.R
import com.example.aguardapp.core.ui.theme.AguaMedia
import com.example.aguardapp.feature.notificaciones.domain.model.Notificacion
import com.example.aguardapp.feature.notificaciones.domain.model.TipoNotificacion
import com.example.aguardapp.feature.notificaciones.domain.repository.Notificador

enum class CanalNotificacion(val id: String, val nombre: String, val descripcion: String, val importancia: Int) {
    TANQUE("tanque", "Mi tanque", "Regreso del agua, tanque lleno y tanque bajo", NotificationManagerCompat.IMPORTANCE_HIGH),
    AVISOS("avisos", "Avisos de agua", "Cortes y comunicados recién publicados", NotificationManagerCompat.IMPORTANCE_DEFAULT),
    AHORRO("ahorro", "Ahorro", "Recordatorio para no perder tu racha", NotificationManagerCompat.IMPORTANCE_DEFAULT)
}

fun canalDe(tipo: TipoNotificacion) = when (tipo) {
    TipoNotificacion.AGUA_REGRESO, TipoNotificacion.TANQUE_LLENO, TipoNotificacion.TANQUE_BAJO -> CanalNotificacion.TANQUE
    TipoNotificacion.NUEVO_AVISO -> CanalNotificacion.AVISOS
    TipoNotificacion.RACHA -> CanalNotificacion.AHORRO
}

class NotificadorAndroid(private val context: Context) : Notificador {
    private val gestor = NotificationManagerCompat.from(context)

    fun permitido(): Boolean {
        val concedido = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        return concedido && gestor.areNotificationsEnabled()
    }

    @SuppressLint("MissingPermission")
    override fun mostrar(notificacion: Notificacion) {
        if (!permitido()) return
        crearCanales()
        val canal = canalDe(notificacion.tipo)
        val abrirApp = PendingIntent.getActivity(
            context,
            notificacion.tipo.ordinal,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val aviso = NotificationCompat.Builder(context, canal.id)
            .setSmallIcon(R.drawable.ic_gota)
            .setColor(AguaMedia.toArgb())
            .setContentTitle(notificacion.titulo)
            .setContentText(notificacion.mensaje)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notificacion.mensaje))
            .setPriority(if (canal == CanalNotificacion.TANQUE) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(abrirApp)
            .setAutoCancel(true)
            .build()
        try {
            gestor.notify(ID_BASE + notificacion.tipo.ordinal, aviso)
        } catch (e: SecurityException) {
            return
        }
    }

    private fun crearCanales() {
        CanalNotificacion.entries.forEach { canal ->
            gestor.createNotificationChannel(
                NotificationChannelCompat.Builder(canal.id, canal.importancia)
                    .setName(canal.nombre)
                    .setDescription(canal.descripcion)
                    .build()
            )
        }
    }

    private companion object {
        const val ID_BASE = 4100
    }
}
