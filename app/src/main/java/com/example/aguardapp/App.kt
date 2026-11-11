package com.example.aguardapp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import com.example.aguardapp.core.navigation.AppNavHost
import com.example.aguardapp.core.navigation.InicioViewModel
import com.example.aguardapp.core.ui.theme.AguardAppTheme

@Composable
fun App(inicio: InicioViewModel = koinViewModel()) {
    AguardAppTheme {
        val uiState by inicio.uiState.collectAsStateWithLifecycle()
        // Mientras se decide la primera pantalla no se muestra nada (es una lectura local muy rápida).
        val rutaInicial = uiState.rutaInicial
        if (rutaInicial != null) {
            AppNavHost(rutaInicial, uiState.hayConfiguracion)
        }
    }
}
