package com.example.aguardapp.core.di

import android.content.Context
import android.os.Build
import com.example.aguardapp.BuildConfig
import com.example.aguardapp.feature.cortes.data.CortesRepositoryImpl
import com.example.aguardapp.feature.cortes.data.PreferenciasCortes
import com.example.aguardapp.feature.cortes.data.remote.CortesApi
import com.example.aguardapp.feature.cortes.domain.CatalogoSectores
import com.example.aguardapp.feature.cortes.domain.CortesRepository
import com.example.aguardapp.feature.cortes.presentation.CortesViewModel
import com.google.gson.Gson
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

internal fun moduloCortes(context: Context) = module {
    single { PreferenciasCortes(context.getSharedPreferences("cortes", Context.MODE_PRIVATE)) }
    single<CortesRepository> { CortesRepositoryImpl(crearApiCortes()) }
    viewModel {
        val catalogo = context.assets.open("sectores_eps_tacna.json").bufferedReader().use {
            Gson().fromJson(it, CatalogoSectores::class.java)
        }
        CortesViewModel(get(), get(), catalogo)
    }
}

private fun crearApiCortes(): CortesApi? {
    val direccion = BuildConfig.CORTES_API_URL.trim().ifEmpty { urlLocal() }
    val url = direccion.trimEnd('/').plus("/").toHttpUrlOrNull() ?: return null
    if (!url.isHttps && (!BuildConfig.DEBUG || url.host !in setOf("127.0.0.1", "10.0.2.2"))) return null

    return Retrofit.Builder()
        .baseUrl(url)
        .client(OkHttpClient.Builder().callTimeout(15, TimeUnit.SECONDS).build())
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(CortesApi::class.java)
}

private fun urlLocal(): String {
    if (!BuildConfig.DEBUG) return ""
    val emulador = Build.HARDWARE in setOf("ranchu", "goldfish")
    return if (emulador) "http://10.0.2.2:8000/" else "http://127.0.0.1:8000/"
}
