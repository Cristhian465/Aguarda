package com.example.aguardapp.feature.notificaciones.domain.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

enum class TipoNotificacion { AGUA_REGRESO, TANQUE_LLENO, TANQUE_BAJO, NUEVO_AVISO, RACHA }

data class Notificacion(val tipo: TipoNotificacion, val titulo: String, val mensaje: String)

data class LecturaNivel(val porcentaje: Double, val delSensor: Boolean)

data class AvisoReciente(val id: String, val titulo: String, val fuente: String)

data class EstadoRacha(val dias: Int, val cumplidaHoy: Boolean)

data class Revision(
    val nivel: LecturaNivel?,
    val aviso: AvisoReciente?,
    val racha: EstadoRacha,
    val ahora: LocalDateTime
)

data class EstadoNotificaciones(
    val ultimoPorcentaje: Double? = null,
    val ultimoDelSensor: Boolean = false,
    val llenandose: Boolean = false,
    val ultimoAvisoId: String? = null,
    val rachaRecordadaEl: LocalDate? = null
)

data class ResultadoRevision(val notificaciones: List<Notificacion>, val estado: EstadoNotificaciones)
