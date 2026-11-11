package com.example.aguardapp.core.di

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.example.aguardapp.BuildConfig
import org.koin.mp.KoinPlatform
import com.example.aguardapp.core.data.UsuarioEntity
import com.example.aguardapp.core.db.AguardAppDatabase
import com.example.aguardapp.core.navigation.InicioViewModel
import com.example.aguardapp.core.util.Reloj
import com.example.aguardapp.core.util.RelojDelSistema
import com.example.aguardapp.core.util.nuevoUuid
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
import com.example.aguardapp.feature.mapa.data.MapaRepositoryImpl
import com.example.aguardapp.feature.mapa.domain.repository.MapaRepository
import com.example.aguardapp.feature.mapa.presentation.MapaViewModel
import com.example.aguardapp.feature.sensor.data.SensorRepositoryImpl
import com.example.aguardapp.feature.sensor.data.remote.ThingSpeakApi
import com.example.aguardapp.feature.sensor.domain.repository.SensorRepository
import com.example.aguardapp.feature.sensor.presentation.SensorViewModel
import com.example.aguardapp.feature.represas.data.RepresasRepositoryImpl
import com.example.aguardapp.feature.represas.data.UbicacionRepositoryImpl
import com.example.aguardapp.feature.represas.data.remote.AnaApi
import com.example.aguardapp.feature.represas.data.remote.NominatimApi
import com.example.aguardapp.feature.represas.data.remote.URL_ANA
import com.example.aguardapp.feature.represas.data.remote.URL_NOMINATIM
import com.example.aguardapp.feature.represas.domain.repository.RepresasRepository
import com.example.aguardapp.feature.represas.domain.repository.UbicacionRepository
import com.example.aguardapp.feature.represas.presentation.DetallePresaViewModel
import com.example.aguardapp.feature.represas.presentation.RepresasViewModel
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient

/** Se llama una sola vez desde la clase `Application`, antes de mostrar ninguna pantalla. */
fun iniciarAplicacion(context: Context) {
    if (KoinPlatform.getKoinOrNull() != null) return
    val base = AguardAppDatabase.crear(context)
    // El usuario debe existir antes de la primera pantalla; es una lectura local y rápida.
    val usuarioId = runBlocking(Dispatchers.IO) { obtenerOCrearUsuario(base) }
    startKoin { modules(moduloApp(base, usuarioId), moduloRed, moduloRepresas(context.applicationContext, base, usuarioId), moduloViewModels) }
}

// Lee el usuario local o lo crea la primera vez, con un UUID que ya no cambia.
private suspend fun obtenerOCrearUsuario(base: AguardAppDatabase): String {
    val dao = base.usuarioDao()
    val existente = dao.obtener()
    if (existente != null) return existente.id
    val nuevo = UsuarioEntity(id = nuevoUuid())
    dao.guardar(nuevo)
    return nuevo.id
}

/** Todo lo que se inyecta en la app: la base de datos, el reloj y los repositorios del depósito y del mapa. */
private fun moduloApp(base: AguardAppDatabase, usuarioId: String) = module {
    single<Reloj> { RelojDelSistema() }
    single { base.usuarioDao() }
    single<DepositoRepository> {
        DepositoRepositoryImpl(base.depositoDao(), usuarioId, get<Reloj>()::ahora, ::nuevoUuid)
    }
    single<MapaRepository> { MapaRepositoryImpl() }
}

/** Cliente HTTP (Retrofit) para leer el sensor IoT del tanque publicado en ThingSpeak. */
private val moduloRed = module {
    single {
        Retrofit.Builder()
            .baseUrl("https://api.thingspeak.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    single<ThingSpeakApi> { get<Retrofit>().create(ThingSpeakApi::class.java) }
    single<SensorRepository> {
        SensorRepositoryImpl(get(), BuildConfig.THINGSPEAK_CHANNEL_ID, BuildConfig.THINGSPEAK_READ_KEY)
    }
}

// Nominatim exige que cada app se identifique (solo caracteres ASCII). Los servicios del Estado a veces tardan.
private const val AGENTE_DE_USUARIO = "AguardApp/1.0 (proyecto academico UPT, Tacna)"
private const val ESPERA_DE_RED_SEGUNDOS = 20L

/** Represas de Tacna (API de la ANA), distrito del hogar (GPS y Nominatim) y su copia en Room. */
private fun moduloRepresas(context: Context, base: AguardAppDatabase, usuarioId: String) = module {
    single {
        OkHttpClient.Builder()
            .connectTimeout(ESPERA_DE_RED_SEGUNDOS, TimeUnit.SECONDS)
            .readTimeout(ESPERA_DE_RED_SEGUNDOS, TimeUnit.SECONDS)
            .addInterceptor { cadena ->
                cadena.proceed(cadena.request().newBuilder().header("User-Agent", AGENTE_DE_USUARIO).build())
            }
            .build()
    }
    single<AnaApi> { retrofit(URL_ANA, get()).create(AnaApi::class.java) }
    single<NominatimApi> { retrofit(URL_NOMINATIM, get()).create(NominatimApi::class.java) }
    single<UbicacionRepository> { UbicacionRepositoryImpl(context) }
    single<RepresasRepository> {
        RepresasRepositoryImpl(get(), get(), base.represasDao(), usuarioId, get<Reloj>()::ahora)
    }
}

private fun retrofit(urlBase: String, cliente: OkHttpClient): Retrofit =
    Retrofit.Builder()
        .baseUrl(urlBase)
        .client(cliente)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

/** ViewModels de cada pantalla: Koin los crea y les entrega sus dependencias. */
private val moduloViewModels = module {
    viewModel { InicioViewModel(get(), get()) }
    viewModel { BienvenidaViewModel(get()) }
    viewModel { ConfiguracionViewModel(get()) }
    viewModel { DepositoViewModel(get(), get<Reloj>()::ahora) }
    viewModel { AvisosViewModel(get(), get<Reloj>()::ahora) }
    viewModel { QueRecortarViewModel(get(), get<Reloj>()::ahora) }
    viewModel { SinAguaViewModel(get(), get<Reloj>()::ahora) }
    // El tipo de llenado (completo o parcial) llega desde la pantalla con parametersOf(tipo).
    viewModel { (tipo: TipoLlenado) -> RegistrarLlenadoViewModel(get(), get<Reloj>()::ahora, tipo) }
    viewModel { MapaViewModel(get()) }
    viewModel { SensorViewModel(get(), get(), get<Reloj>()::ahora) }
    viewModel { RepresasViewModel(get(), get(), get(), get<Reloj>()::ahora) }
    // El nombre de la represa llega desde la pantalla con parametersOf(nombre).
    viewModel { (nombre: String) -> DetallePresaViewModel(get(), get(), nombre) }
}
