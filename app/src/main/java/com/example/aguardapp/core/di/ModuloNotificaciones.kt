package com.example.aguardapp.core.di

import android.content.Context
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import com.example.aguardapp.core.util.Reloj
import com.example.aguardapp.feature.notificaciones.data.FuentesNotificacionesImpl
import com.example.aguardapp.feature.notificaciones.data.NotificadorAndroid
import com.example.aguardapp.feature.notificaciones.data.PreferenciasNotificaciones
import com.example.aguardapp.feature.notificaciones.domain.repository.EstadoNotificacionesRepository
import com.example.aguardapp.feature.notificaciones.domain.repository.FuentesNotificaciones
import com.example.aguardapp.feature.notificaciones.domain.repository.Notificador
import com.example.aguardapp.feature.notificaciones.domain.usecase.RevisarNotificaciones
import com.example.aguardapp.feature.notificaciones.presentation.NotificacionesViewModel

internal fun moduloNotificaciones(context: Context) = module {
    single { PreferenciasNotificaciones(context.getSharedPreferences("notificaciones", Context.MODE_PRIVATE)) }
    single<EstadoNotificacionesRepository> { get<PreferenciasNotificaciones>() }
    single<Notificador> { NotificadorAndroid(context) }
    single<FuentesNotificaciones> { FuentesNotificacionesImpl(get(), get(), get(), get(), get(), get<Reloj>()::ahora) }
    single { RevisarNotificaciones(get(), get(), get(), get<Reloj>()::ahora) }
    viewModel { NotificacionesViewModel(get(), get()) }
}
