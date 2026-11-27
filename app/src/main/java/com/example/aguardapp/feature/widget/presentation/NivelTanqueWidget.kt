package com.example.aguardapp.feature.widget.presentation

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import com.example.aguardapp.feature.widget.domain.usecase.ObtenerEstadoTanque

class NivelTanqueWidget : GlanceAppWidget(), KoinComponent {
    override val sizeMode = SizeMode.Exact

    private val obtenerEstado: ObtenerEstadoTanque by inject()

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val estado = obtenerEstado()
        provideContent { ContenidoWidget(estado) }
    }
}
