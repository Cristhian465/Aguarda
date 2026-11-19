package com.example.aguardapp.core.di

import android.content.Context
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.example.aguardapp.BuildConfig
import com.example.aguardapp.feature.eps.data.ConsultaEpsPortal
import com.example.aguardapp.feature.eps.domain.ConsultaEps
import com.example.aguardapp.feature.eps.presentation.EpsViewModel
import com.example.aguardapp.core.util.Reloj
import com.example.aguardapp.feature.liga.data.LectorDeRecibo
import com.example.aguardapp.feature.liga.data.LigaRepositoryImpl
import com.example.aguardapp.feature.liga.data.PreferenciasLiga
import com.example.aguardapp.feature.liga.data.RankingRepositoryImpl
import com.example.aguardapp.feature.liga.data.ResumenDelHogar
import com.example.aguardapp.feature.liga.data.local.LigaDatabase
import com.example.aguardapp.feature.liga.data.remote.SupabaseApi
import com.example.aguardapp.feature.liga.domain.LigaRepository
import com.example.aguardapp.feature.liga.domain.RankingRepository
import com.example.aguardapp.feature.liga.presentation.LigaViewModel
import com.example.aguardapp.feature.liga.presentation.RankingLigaViewModel
import com.example.aguardapp.feature.liga.presentation.ReciboViewModel

/** Liga del Agua: datos locales, conexión con Supabase y pantallas. */
internal fun moduloLiga(context: Context) = module {
    single { LigaDatabase.crear(context).ligaDao() }
    single<LigaRepository> { LigaRepositoryImpl(get()) }
    single { PreferenciasLiga(context.getSharedPreferences("liga", Context.MODE_PRIVATE)) }
    single { ResumenDelHogar(get(), get(), get(), get(), get<Reloj>()::ahora) }
    single { LectorDeRecibo(context) }
    single<ConsultaEps> { ConsultaEpsPortal(context) }
    viewModel { EpsViewModel(get(), get(), get<Reloj>()::ahora) }
    single<RankingRepository> { RankingRepositoryImpl(crearSupabaseApi(), BuildConfig.SUPABASE_ANON_KEY) }
    viewModel { LigaViewModel(get(), get(), get(), get<Reloj>()::ahora) }
    viewModel { RankingLigaViewModel(get(), get(), get(), get(), get(), get<Reloj>()::ahora) }
    viewModel { ReciboViewModel(get(), get(), get<Reloj>()::ahora) }
}

// Sin las claves de Supabase la app funciona igual y el ranking queda desconectado.
private fun crearSupabaseApi(): SupabaseApi? {
    if (BuildConfig.SUPABASE_URL.isBlank() || BuildConfig.SUPABASE_ANON_KEY.isBlank()) return null
    return Retrofit.Builder()
        .baseUrl(BuildConfig.SUPABASE_URL.trimEnd('/') + "/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(SupabaseApi::class.java)
}
