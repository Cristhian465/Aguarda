package com.example.aguardapp.core.di

import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import org.koin.dsl.module
import com.example.aguardapp.feature.widget.domain.usecase.ObtenerEstadoTanque

@OptIn(ExperimentalTime::class)
internal val moduloWidget = module {
    single { ObtenerEstadoTanque(get(), get()) { Clock.System.now() } }
}
