package com.example.aguardapp.core.di

import android.content.Context
import com.google.gson.GsonBuilder
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.example.aguardapp.core.db.AguardAppDatabase
import com.example.aguardapp.core.util.Reloj
import com.example.aguardapp.feature.familia.data.FamiliaRepositoryImpl
import com.example.aguardapp.feature.familia.data.PreferenciasFamilia
import com.example.aguardapp.feature.familia.data.remote.FamiliaApi
import com.example.aguardapp.feature.familia.domain.repository.FamiliaRepository
import com.example.aguardapp.feature.familia.presentation.FamiliaViewModel
import com.example.aguardapp.feature.liga.data.remote.URL_SUPABASE

/** Modo familia: el tanque compartido entre los teléfonos del hogar, sincronizado con Supabase. */
internal fun moduloFamilia(context: Context, base: AguardAppDatabase, usuarioId: String) = module {
    single { PreferenciasFamilia(context.getSharedPreferences("familia", Context.MODE_PRIVATE)) }
    single<FamiliaApi> {
        Retrofit.Builder()
            .baseUrl(URL_SUPABASE)
            // Los nulos se envían explícitos: Supabase necesita recibir todos los parámetros de la función.
            .addConverterFactory(GsonConverterFactory.create(GsonBuilder().serializeNulls().create()))
            .build()
            .create(FamiliaApi::class.java)
    }
    single<FamiliaRepository> {
        val reloj = get<Reloj>()
        FamiliaRepositoryImpl(get(), base.familiaDao(), get(), usuarioId, { reloj.ahora().date })
    }
    viewModel { FamiliaViewModel(get()) }
}
