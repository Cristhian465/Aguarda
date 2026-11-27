package com.example.aguardapp.feature.notificaciones.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import com.example.aguardapp.feature.notificaciones.domain.usecase.RevisarNotificaciones

private const val TRABAJO = "revisar_notificaciones"
private const val MINUTOS_ENTRE_REVISIONES = 30L

class RevisarNotificacionesWorker(context: Context, parametros: WorkerParameters) :
    CoroutineWorker(context, parametros), KoinComponent {

    private val revisar: RevisarNotificaciones by inject()

    override suspend fun doWork(): Result {
        revisar()
        return Result.success()
    }
}

fun programarRevisionDeNotificaciones(context: Context) {
    val pedido = PeriodicWorkRequestBuilder<RevisarNotificacionesWorker>(MINUTOS_ENTRE_REVISIONES, TimeUnit.MINUTES).build()
    WorkManager.getInstance(context).enqueueUniquePeriodicWork(TRABAJO, ExistingPeriodicWorkPolicy.KEEP, pedido)
}
