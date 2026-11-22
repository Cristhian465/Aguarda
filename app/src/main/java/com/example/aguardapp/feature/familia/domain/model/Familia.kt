package com.example.aguardapp.feature.familia.domain.model

enum class TipoEventoFamiliar(val clave: String) {
    LLENADO("llenado"),
    SIN_AGUA("sin_agua");

    companion object {
        fun deClave(clave: String?): TipoEventoFamiliar? = entries.firstOrNull { it.clave == clave }
    }
}

/** Un llenado o un "me quedé sin agua" compartido con la familia. Los eventos solo se agregan, nunca se editan. */
data class EventoCompartido(
    val id: String,
    val tipo: TipoEventoFamiliar,
    val momento: String,
    val litros: Double,
    val inicioObservado: String? = null,
    val autor: String? = null,
    val propio: Boolean = true
)

/** Perfil del hogar tal como se comparte: el mismo para todos los teléfonos de la familia. */
data class PerfilCompartido(
    val tipoReservorio: String,
    val capacidadLitros: Double,
    val habitantes: Int,
    val duchasPorDia: Int,
    val usaLavadora: Boolean,
    val riegaJardin: Boolean,
    val horaProximoLlenado: String,
    val consumoPorHabitosLitrosHora: Double?,
    val consumoVigenteLitrosHora: Double?
)

data class MiembroFamilia(val apodo: String, val esPropio: Boolean)

data class EstadoSincronizacion(
    val miembros: List<MiembroFamilia>,
    val nuevosDeOtros: List<EventoCompartido>
)

data class Invitacion(val codigo: String, val minutos: Int)

class FamiliaExpulsadaException :
    IllegalStateException("Ya no formas parte de esta familia. Los datos del tanque siguen en este teléfono.")
