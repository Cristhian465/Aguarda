package com.example.aguardapp.core.di

import com.google.gson.GsonBuilder
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.example.aguardapp.feature.liga.data.PreferenciasLiga
import com.example.aguardapp.feature.liga.data.remote.URL_SUPABASE
import com.example.aguardapp.feature.mapa.data.ReportesRepositoryImpl
import com.example.aguardapp.feature.mapa.data.remote.CisternasApi
import com.example.aguardapp.feature.mapa.data.remote.ReportesApi
import com.example.aguardapp.feature.mapa.domain.repository.ReportesRepository

internal fun moduloReportes() = module {
    single<ReportesApi> { retrofitSupabase().create(ReportesApi::class.java) }
    single<CisternasApi> { retrofitSupabase().create(CisternasApi::class.java) }
    single<ReportesRepository> { ReportesRepositoryImpl(get(), get<PreferenciasLiga>()::token) }
}

private fun retrofitSupabase(): Retrofit =
    Retrofit.Builder()
        .baseUrl(URL_SUPABASE)
        .addConverterFactory(GsonConverterFactory.create(GsonBuilder().serializeNulls().create()))
        .build()
