package com.example.aguardapp.core.di

import android.content.Context
import com.example.aguardapp.core.util.Reloj
import com.example.aguardapp.feature.eps.data.ConsultaEpsPortal
import com.example.aguardapp.feature.eps.data.LectorDeRecibo
import com.example.aguardapp.feature.eps.data.RecibosLocales
import com.example.aguardapp.feature.eps.domain.ConsultaEps
import com.example.aguardapp.feature.eps.presentation.EpsViewModel
import com.example.aguardapp.feature.eps.presentation.ReciboViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

internal fun moduloRecibos(context: Context) = module {
    // Mantiene el recibo de quienes ya lo guardaron antes de separar las pantallas.
    single { RecibosLocales(context.getSharedPreferences("liga", Context.MODE_PRIVATE)) }
    single { LectorDeRecibo(context) }
    single<ConsultaEps> { ConsultaEpsPortal(context) }
    viewModel { EpsViewModel(get(), get(), get<Reloj>()::ahora) }
    viewModel { ReciboViewModel(get(), get(), get<Reloj>()::ahora) }
}
