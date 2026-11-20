package com.example.aguardapp.core.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.aguardapp.feature.ahorro.presentation.AhorroScreen
import com.example.aguardapp.feature.bienvenida.presentation.BienvenidaScreen
import com.example.aguardapp.feature.deposito.presentation.AvisosScreen
import com.example.aguardapp.feature.deposito.presentation.ConfiguracionScreen
import com.example.aguardapp.feature.deposito.presentation.DepositoScreen
import com.example.aguardapp.feature.deposito.presentation.QueRecortarScreen
import com.example.aguardapp.feature.deposito.presentation.RegistrarLlenadoScreen
import com.example.aguardapp.feature.deposito.presentation.SinAguaScreen
import com.example.aguardapp.feature.deposito.presentation.TipoLlenado
import com.example.aguardapp.feature.eps.presentation.RecibosEpsScreen
import com.example.aguardapp.feature.liga.presentation.LigaScreen
import com.example.aguardapp.feature.mapa.presentation.MapaScreen
import com.example.aguardapp.feature.represas.presentation.RepresasScreen

@Composable
fun AppNavHost(rutaInicial: String, hayConfiguracion: Boolean) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = rutaInicial) {
        composable(Rutas.BIENVENIDA) {
            BienvenidaScreen(onListo = { irDespuesDeLaBienvenida(navController, hayConfiguracion) })
        }

        composable(Rutas.CONFIGURAR_HOGAR) {
            val editando = navController.previousBackStackEntry != null
            ConBarraDelSistema {
                ConfiguracionScreen(
                    onListo = { irDespuesDeConfigurar(navController, editando) },
                    onVolver = if (editando) ({ navController.popBackStack() }) else null
                )
            }
        }

        composable(Rutas.INICIO) {
            ConBarraDelSistema {
                InicioScreen(
                    onAbrir = { ruta -> navController.navigate(ruta) { launchSingleTop = true } },
                    onLlenado = { tipo -> navController.navigate(Rutas.registrarLlenado(tipo.ruta)) }
                )
            }
        }

        composable(Rutas.RECIBOS_EPS) {
            ConBarraDelSistema { RecibosEpsScreen(onVolver = { navController.popBackStack() }) }
        }

        composable(Rutas.MI_DEPOSITO) {
            ConBarraDelSistema {
                DepositoScreen(
                    onVolver = { navController.popBackStack() },
                    onRegistrarLlenado = { tipo -> navController.navigate(Rutas.registrarLlenado(tipo.ruta)) },
                    onQueRecortar = { navController.navigate(Rutas.QUE_RECORTAR) },
                    onSinAgua = { navController.navigate(Rutas.ME_QUEDE_SIN_AGUA) },
                    onAvisos = { navController.navigate(Rutas.AVISOS) },
                    onEditarHogar = { navController.navigate(Rutas.CONFIGURAR_HOGAR) }
                )
            }
        }

        composable(Rutas.MAPA) {
            ConBarraDelSistema {
                MapaScreen(onVolver = { navController.popBackStack() })
            }
        }

        composable(Rutas.LIGA) {
            ConBarraDelSistema {
                LigaScreen(onVolver = { navController.popBackStack() })
            }
        }

        composable(Rutas.REPRESAS) {
            ConBarraDelSistema {
                RepresasScreen(onVolver = { navController.popBackStack() })
            }
        }

        composable(Rutas.AHORRO) {
            ConBarraDelSistema {
                AhorroScreen(onVolver = { navController.popBackStack() })
            }
        }

        composable(
            Rutas.REGISTRAR_LLENADO,
            arguments = listOf(navArgument(Rutas.ARG_TIPO) { type = NavType.StringType })
        ) { entrada ->
            val tipo = TipoLlenado.desdeRuta(entrada.arguments?.getString(Rutas.ARG_TIPO))
            ConBarraDelSistema {
                RegistrarLlenadoScreen(
                    tipo = tipo,
                    onListo = { navController.popBackStack() },
                    onVolver = { navController.popBackStack() }
                )
            }
        }

        composable(Rutas.QUE_RECORTAR) {
            ConBarraDelSistema {
                QueRecortarScreen(onVolver = { navController.popBackStack() })
            }
        }

        composable(Rutas.ME_QUEDE_SIN_AGUA) {
            ConBarraDelSistema {
                SinAguaScreen(onVolver = { navController.popBackStack() })
            }
        }

        composable(Rutas.AVISOS) {
            ConBarraDelSistema {
                AvisosScreen(
                    onVolver = { navController.popBackStack() },
                    onRegistrarLlenado = { navController.navigate(Rutas.registrarLlenado(TipoLlenado.COMPLETO.ruta)) },
                    onQueRecortar = { navController.navigate(Rutas.QUE_RECORTAR) }
                )
            }
        }
    }
}

private fun irDespuesDeLaBienvenida(navController: NavHostController, hayConfiguracion: Boolean) {
    val destino = if (hayConfiguracion) Rutas.INICIO else Rutas.CONFIGURAR_HOGAR
    navController.navigate(destino) { popUpTo(Rutas.BIENVENIDA) { inclusive = true } }
}

private fun irDespuesDeConfigurar(navController: NavHostController, editando: Boolean) {
    if (editando) {
        navController.popBackStack()
    } else {
        navController.navigate(Rutas.INICIO) { popUpTo(Rutas.CONFIGURAR_HOGAR) { inclusive = true } }
    }
}

@Composable
private fun ConBarraDelSistema(contenido: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).navigationBarsPadding()) {
        contenido()
    }
}
