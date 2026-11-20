package com.example.aguardapp.core.di

import android.content.Context
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.example.aguardapp.core.util.Reloj
import com.example.aguardapp.feature.liga.data.LigaRepositoryImpl
import com.example.aguardapp.feature.liga.data.PreferenciasLiga
import com.example.aguardapp.feature.liga.data.RankingRepositoryImpl
import com.example.aguardapp.feature.liga.data.ResumenDelHogar
import com.example.aguardapp.feature.liga.data.local.LigaDatabase
import com.example.aguardapp.feature.liga.data.remote.SupabaseApi
import com.example.aguardapp.feature.liga.data.remote.URL_SUPABASE
import com.example.aguardapp.feature.liga.domain.LigaRepository
import com.example.aguardapp.feature.liga.domain.RankingRepository
import com.example.aguardapp.feature.liga.presentation.LigaViewModel
import com.example.aguardapp.feature.liga.presentation.RankingLigaViewModel

internal fun moduloLiga(context: Context) = module {
    single { LigaDatabase.crear(context).ligaDao() }
    single<LigaRepository> { LigaRepositoryImpl(get()) }
    single { PreferenciasLiga(context.getSharedPreferences("liga", Context.MODE_PRIVATE)) }
    single { ResumenDelHogar(get(), get(), get(), get(), get<Reloj>()::ahora) }
    single<RankingRepository> { RankingRepositoryImpl(crearSupabaseApi()) }
    viewModel { LigaViewModel(get(), get(), get(), get<Reloj>()::ahora) }
    viewModel { RankingLigaViewModel(get(), get(), get(), get(), get(), get<Reloj>()::ahora) }
}

private fun crearSupabaseApi(): SupabaseApi =
    Retrofit.Builder()
        .baseUrl(URL_SUPABASE)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(SupabaseApi::class.java)
