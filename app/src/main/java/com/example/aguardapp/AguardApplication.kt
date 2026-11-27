package com.example.aguardapp

import android.app.Application
import com.example.aguardapp.core.di.iniciarAplicacion
import com.example.aguardapp.feature.notificaciones.data.programarRevisionDeNotificaciones

class AguardApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        iniciarAplicacion(this)
        programarRevisionDeNotificaciones(this)
    }
}
