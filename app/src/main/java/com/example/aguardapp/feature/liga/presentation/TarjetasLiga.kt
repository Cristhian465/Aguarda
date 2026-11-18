package com.example.aguardapp.feature.liga.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import com.example.aguardapp.core.ui.theme.AguaMedia
import com.example.aguardapp.core.ui.theme.FuenteTexto
import com.example.aguardapp.core.ui.theme.Tinta
import com.example.aguardapp.core.ui.theme.TintaSuave
import com.example.aguardapp.feature.deposito.presentation.componentes.BotonPrincipal
import com.example.aguardapp.feature.deposito.presentation.componentes.TarjetaBlanca
import com.example.aguardapp.feature.liga.domain.CalcularPuntaje
import com.example.aguardapp.feature.liga.domain.PuntajeHogar
import com.example.aguardapp.feature.liga.domain.ResumenSemanal

@Composable
fun TarjetaSinHogar(modifier: Modifier = Modifier) {
    TarjetaBlanca(modifier) {
        Titulo("Configura tu hogar")
        Detalle("Para participar, primero registra tu tanque y cuántas personas viven en casa.")
    }
}

@Composable
fun TarjetaMarcarNivel(estado: LigaUiState, capacidad: Int, onCambiar: (Int) -> Unit, onGuardar: () -> Unit) {
    TarjetaBlanca {
        Titulo("¿Cuánta agua hay en tu tanque?")
        Detalle("Míralo y márcalo una vez al día. Así medimos cuánto consumes de verdad.")
        Text(
            "${estado.nivelElegido} L de $capacidad L",
            Modifier.padding(top = 8.dp),
            fontFamily = FuenteTexto, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = AguaMedia
        )
        Slider(
            value = estado.nivelElegido.toFloat().coerceIn(0f, capacidad.toFloat()),
            onValueChange = { onCambiar(it.roundToInt()) },
            valueRange = 0f..capacidad.toFloat()
        )
        BotonPrincipal(if (estado.guardando) "Guardando…" else "Guardar nivel", onGuardar, habilitado = !estado.guardando)
        estado.mensaje?.let { Detalle(it) }
    }
}

@Composable
fun TarjetaConsumo(resumen: ResumenSemanal) {
    TarjetaBlanca {
        Titulo("Tu consumo medido")
        val actual = resumen.litrosPorPersonaDia
        if (actual == null) {
            Detalle("Aún no hay datos suficientes: marca el nivel de tu tanque durante al menos 24 horas, sin llenados de por medio.")
        } else {
            Text(
                "${actual.roundToInt()} L por persona al día",
                fontFamily = FuenteTexto, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Tinta
            )
        }
        resumen.lineaBase?.let { Detalle("Tu consumo habitual: ${it.roundToInt()} L por persona al día") }
    }
}

@Composable
fun TarjetaPuntaje(resumen: ResumenSemanal) {
    TarjetaBlanca {
        Titulo("Tu puntaje de la semana")
        val puntaje = resumen.puntaje
        if (puntaje == null) {
            Detalle("Tu puntaje aparecerá cuando tengamos tu consumo medido de esta semana.")
        } else {
            Desglose(puntaje, resumen)
        }
    }
}

@Composable
private fun Desglose(puntaje: PuntajeHogar, resumen: ResumenSemanal) {
    Text("${puntaje.total} / 100", fontFamily = FuenteTexto, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = AguaMedia)
    Fila("Mejora frente a tu consumo habitual", puntaje.puntosMejora, CalcularPuntaje.PESO_MEJORA)
    Fila("Retos cumplidos (${resumen.diasConRetos} de 7 días)", puntaje.puntosRetos, CalcularPuntaje.PESO_RETOS)
    Fila("Constancia (${resumen.diasConMarca} de 7 días)", puntaje.puntosConstancia, CalcularPuntaje.PESO_CONSTANCIA)
    if (resumen.lineaBase == null) {
        Detalle("La mejora empezará a sumar cuando tengas una semana anterior medida.")
    }
}

@Composable
private fun Fila(texto: String, puntos: Int, maximo: Int) {
    Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(texto, Modifier.weight(1f), fontFamily = FuenteTexto, fontSize = 13.sp, color = TintaSuave)
        Text("$puntos / $maximo", fontFamily = FuenteTexto, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Tinta)
    }
}

@Composable
internal fun Titulo(texto: String, color: Color = Tinta) {
    Text(texto, fontFamily = FuenteTexto, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = color)
}

@Composable
internal fun Detalle(texto: String) {
    Text(texto, fontFamily = FuenteTexto, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TintaSuave)
}
