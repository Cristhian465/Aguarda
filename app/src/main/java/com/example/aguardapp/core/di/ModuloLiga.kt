package com.example.aguardapp.core.di

import android.content.Context
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import com.example.aguardapp.core.util.Reloj
import com.example.aguardapp.feature.liga.data.LigaRepositoryImpl
import com.example.aguardapp.feature.liga.data.local.LigaDatabase
import com.example.aguardapp.feature.liga.domain.LigaRepository
import com.example.aguardapp.feature.liga.presentation.LigaViewModel

/** Liga del Agua: sus datos locales y su pantalla. */
internal fun moduloLiga(context: Context) = module {
    single { LigaDatabase.crear(context).ligaDao() }
    single<LigaRepository> { LigaRepositoryImpl(get()) }
    viewModel { LigaViewModel(get(), get(), get(), get<Reloj>()::ahora) }
}
