package com.example.aguardapp.core.di

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import org.koin.mp.KoinPlatform
import com.example.aguardapp.core.data.UsuarioEntity
import com.example.aguardapp.core.db.AguardAppDatabase
import com.example.aguardapp.core.navigation.InicioViewModel
import com.example.aguardapp.core.util.Reloj
import com.example.aguardapp.core.util.RelojDelSistema
import com.example.aguardapp.core.util.nuevoUuid
import com.example.aguardapp.feature.ahorro.data.AhorroLocal
import com.example.aguardapp.feature.ahorro.presentation.AhorroViewModel
import com.example.aguardapp.feature.deposito.data.DepositoRepositoryImpl
import com.example.aguardapp.feature.bienvenida.presentation.BienvenidaViewModel
import com.example.aguardapp.feature.deposito.domain.repository.DepositoRepository
import com.example.aguardapp.feature.deposito.presentation.AvisosViewModel
import com.example.aguardapp.feature.deposito.presentation.ConfiguracionViewModel
import com.example.aguardapp.feature.deposito.presentation.DepositoViewModel
import com.example.aguardapp.feature.deposito.presentation.QueRecortarViewModel
import com.example.aguardapp.feature.deposito.presentation.RegistrarLlenadoViewModel
import com.example.aguardapp.feature.deposito.presentation.SinAguaViewModel
import com.example.aguardapp.feature.deposito.presentation.TipoLlenado
import com.example.aguardapp.feature.mapa.data.CopiaCisternasLocal
import com.example.aguardapp.feature.mapa.data.DistritosRepositoryImpl
import com.example.aguardapp.feature.mapa.data.MapaRepositoryImpl
import com.example.aguardapp.feature.mapa.data.UbicacionRepositoryImpl
import com.example.aguardapp.feature.mapa.domain.repository.DistritosRepository
import com.example.aguardapp.feature.mapa.domain.repository.MapaRepository
import com.example.aguardapp.feature.mapa.domain.repository.UbicacionRepository
import com.example.aguardapp.feature.mapa.presentation.MapaViewModel
import com.example.aguardapp.feature.sensor.data.PreferenciasFuga
import com.example.aguardapp.feature.sensor.data.SensorRepositoryImpl
import com.example.aguardapp.feature.sensor.data.local.SensorDatabase
import com.example.aguardapp.feature.sensor.data.remote.ThingSpeakApi
import com.example.aguardapp.feature.sensor.domain.repository.SensorRepository
import com.example.aguardapp.feature.sensor.presentation.FugaViewModel
import com.example.aguardapp.feature.sensor.presentation.SensorViewModel
import com.example.aguardapp.feature.represas.data.RepresasRepositoryImpl
import com.example.aguardapp.feature.represas.data.remote.AnaApi
import com.example.aguardapp.feature.represas.domain.repository.RepresasRepository
import com.example.aguardapp.feature.represas.data.remote.URL_ANA
import com.example.aguardapp.feature.represas.presentation.RepresasViewModel
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

fun iniciarAplicacion(context: Context) {
    if (KoinPlatform.getKoinOrNull() != null) return
    val base = AguardAppDatabase.crear(context)
    val usuarioId = runBlocking(Dispatchers.IO) { obtenerOCrearUsuario(base) }
    startKoin {
        modules(
            moduloApp(context, base, usuarioId), moduloRed(context), moduloViewModels, moduloLiga(context),
            moduloRecibos(context), moduloReportes(), moduloFamilia(context, base, usuarioId), moduloCortes(context),
            moduloNotificaciones(context), moduloWidget
        )
    }
}

private suspend fun obtenerOCrearUsuario(base: AguardAppDatabase): String {
    val dao = base.usuarioDao()
    val existente = dao.obtener()
    if (existente != null) return existente.id
    val nuevo = UsuarioEntity(id = nuevoUuid())
    dao.guardar(nuevo)
    return nuevo.id
}

private fun moduloApp(context: Context, base: AguardAppDatabase, usuarioId: String) = module {
    single<Reloj> { RelojDelSistema() }
    single { AhorroLocal(context.getSharedPreferences("ahorro", Context.MODE_PRIVATE)) }
    single { base.usuarioDao() }
    single<DepositoRepository> {
        DepositoRepositoryImpl(base.depositoDao(), usuarioId, get<Reloj>()::ahora, ::nuevoUuid)
    }
    single<MapaRepository> { MapaRepositoryImpl(get(), CopiaCisternasLocal(context.getSharedPreferences("mapa", Context.MODE_PRIVATE))) }
    single<UbicacionRepository> { UbicacionRepositoryImpl(context) }
    single<DistritosRepository> { DistritosRepositoryImpl(context) }
    single { base.presaDao() }
}

private fun moduloRed(context: Context) = module {
    single<SensorRepository> { SensorRepositoryImpl(ThingSpeakApi(), SensorDatabase.crear(context).lecturaDao()) }
    single { PreferenciasFuga(context.getSharedPreferences("fugas", Context.MODE_PRIVATE)) }
    single<AnaApi> {
        Retrofit.Builder()
            .baseUrl(URL_ANA)
            .client(OkHttpClient.Builder().readTimeout(30, TimeUnit.SECONDS).build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AnaApi::class.java)
    }
    single<RepresasRepository> { RepresasRepositoryImpl(get(), get()) }
}

private val moduloViewModels = module {
    viewModel { AhorroViewModel(get(), get()) }
    viewModel { InicioViewModel(get(), get()) }
    viewModel { BienvenidaViewModel(get()) }
    viewModel { ConfiguracionViewModel(get()) }
    viewModel { DepositoViewModel(get(), get<Reloj>()::ahora) }
    viewModel { AvisosViewModel(get(), get<Reloj>()::ahora) }
    viewModel { QueRecortarViewModel(get(), get<Reloj>()::ahora) }
    viewModel { SinAguaViewModel(get(), get<Reloj>()::ahora) }
    viewModel { (tipo: TipoLlenado) -> RegistrarLlenadoViewModel(get(), get<Reloj>()::ahora, tipo) }
    viewModel { MapaViewModel(get(), get(), get(), get(), get(), get(), get<Reloj>()::ahora) }
    viewModel { SensorViewModel(get(), get(), get<Reloj>()::ahora) }
    viewModel { FugaViewModel(get(), get(), get(), get<Reloj>()::ahora) }
    viewModel { RepresasViewModel(get()) }
}
