package com.example.aguardapp.feature.familia.data

import kotlinx.datetime.LocalDateTime
import com.example.aguardapp.feature.deposito.data.local.EventoLlenadoEntity
import com.example.aguardapp.feature.deposito.data.local.NovedadDepositoEntity
import com.example.aguardapp.feature.deposito.data.local.PerfilHogarEntity
import com.example.aguardapp.feature.deposito.data.mapper.aDominio
import com.example.aguardapp.feature.familia.data.remote.EventoFamiliaDto
import com.example.aguardapp.feature.familia.data.remote.PerfilFamiliaDto
import com.example.aguardapp.feature.familia.domain.model.EventoCompartido
import com.example.aguardapp.feature.familia.domain.model.PerfilCompartido
import com.example.aguardapp.feature.familia.domain.model.TipoEventoFamiliar

internal fun PerfilHogarEntity.aCompartido() = PerfilCompartido(
    tipoReservorio, capacidadLitros, habitantes, duchasPorDia, usaLavadora, riegaJardin,
    horaProximoLlenado, consumoPorHabitosLitrosHora, consumoVigenteLitrosHora
)

internal fun PerfilCompartido.aEntidad(usuarioId: String) = PerfilHogarEntity(
    usuarioId, tipoReservorio, capacidadLitros, habitantes, duchasPorDia, usaLavadora, riegaJardin,
    horaProximoLlenado, consumoPorHabitosLitrosHora, consumoVigenteLitrosHora
)

/** Un perfil que llega de otro teléfono solo se guarda si la app puede leerlo. */
internal fun PerfilCompartido.esValido(): Boolean = runCatching { aEntidad("validacion").aDominio() }.isSuccess

internal fun PerfilCompartido.aDto() = PerfilFamiliaDto(
    tipoReservorio, capacidadLitros, habitantes, duchasPorDia, usaLavadora, riegaJardin,
    horaProximoLlenado, consumoPorHabitosLitrosHora, consumoVigenteLitrosHora
)

internal fun PerfilFamiliaDto.aCompartido(): PerfilCompartido? {
    return PerfilCompartido(
        tipoReservorio = tipoReservorio ?: return null,
        capacidadLitros = capacidadLitros ?: return null,
        habitantes = habitantes ?: return null,
        duchasPorDia = duchasPorDia ?: return null,
        usaLavadora = usaLavadora ?: return null,
        riegaJardin = riegaJardin ?: return null,
        horaProximoLlenado = horaProximoLlenado ?: return null,
        consumoPorHabitosLitrosHora = consumoPorHabitosLitrosHora,
        consumoVigenteLitrosHora = consumoVigenteLitrosHora
    )
}

internal fun EventoCompartido.aDto() = EventoFamiliaDto(id, tipo.clave, momento, litros, inicioObservado)

/** Descarta los eventos que llegan incompletos o con fechas que la app no puede leer. */
internal fun EventoFamiliaDto.aCompartido(): EventoCompartido? {
    val tipo = TipoEventoFamiliar.deClave(tipo) ?: return null
    val id = id?.takeIf { it.isNotBlank() } ?: return null
    val momento = momento?.takeIf(::esMomento) ?: return null
    val litros = litros?.takeIf { it.isFinite() && it >= 0 } ?: return null
    val inicio = inicioObservado?.takeIf(::esMomento)
    if (tipo == TipoEventoFamiliar.SIN_AGUA && inicio == null) return null
    return EventoCompartido(id, tipo, momento, litros, inicio, autor, propio == true)
}

internal fun EventoLlenadoEntity.aCompartido() = EventoCompartido(id, TipoEventoFamiliar.LLENADO, momento, litros)

internal fun NovedadDepositoEntity.aCompartido() =
    EventoCompartido(id, TipoEventoFamiliar.SIN_AGUA, momento, litrosObservados, inicioObservado)

internal fun EventoCompartido.aLlenado(usuarioId: String) = EventoLlenadoEntity(id, usuarioId, momento, litros)

internal fun EventoCompartido.aNovedad(usuarioId: String): NovedadDepositoEntity? =
    inicioObservado?.let { NovedadDepositoEntity(id, usuarioId, momento, it, litros) }

private fun esMomento(texto: String): Boolean = runCatching { LocalDateTime.parse(texto) }.isSuccess
