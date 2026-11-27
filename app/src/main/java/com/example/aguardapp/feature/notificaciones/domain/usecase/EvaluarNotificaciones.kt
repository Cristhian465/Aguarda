package com.example.aguardapp.feature.notificaciones.domain.usecase

import kotlin.math.roundToInt
import com.example.aguardapp.feature.notificaciones.domain.model.EstadoNotificaciones
import com.example.aguardapp.feature.notificaciones.domain.model.LecturaNivel
import com.example.aguardapp.feature.notificaciones.domain.model.Notificacion
import com.example.aguardapp.feature.notificaciones.domain.model.ResultadoRevision
import com.example.aguardapp.feature.notificaciones.domain.model.Revision
import com.example.aguardapp.feature.notificaciones.domain.model.TipoNotificacion

const val NIVEL_LLENO = 95.0
const val NIVEL_BAJO = 20.0
const val SUBIDA_MINIMA = 5.0
const val HORA_RECORDAR_RACHA = 19

fun evaluarNotificaciones(revision: Revision, estado: EstadoNotificaciones): ResultadoRevision {
    val notificaciones = mutableListOf<Notificacion>()
    var nuevo = estado

    revision.nivel?.let { nivel ->
        val (avisosNivel, llenandose) = revisarNivel(nivel, estado)
        notificaciones += avisosNivel
        nuevo = nuevo.copy(ultimoPorcentaje = nivel.porcentaje, ultimoDelSensor = nivel.delSensor, llenandose = llenandose)
    }

    revision.aviso?.let { aviso ->
        if (estado.ultimoAvisoId != null && aviso.id != estado.ultimoAvisoId) {
            notificaciones += Notificacion(TipoNotificacion.NUEVO_AVISO, "Nuevo aviso · ${aviso.fuente}", aviso.titulo)
        }
        nuevo = nuevo.copy(ultimoAvisoId = aviso.id)
    }

    val hoy = revision.ahora.date
    val racha = revision.racha
    if (racha.dias > 0 && !racha.cumplidaHoy && revision.ahora.hour >= HORA_RECORDAR_RACHA && estado.rachaRecordadaEl != hoy) {
        val dias = if (racha.dias == 1) "1 día" else "${racha.dias} días"
        notificaciones += Notificacion(
            TipoNotificacion.RACHA,
            "No pierdas tu racha de $dias",
            "Marca hoy al menos un reto de ahorro para continuarla."
        )
        nuevo = nuevo.copy(rachaRecordadaEl = hoy)
    }

    return ResultadoRevision(notificaciones, nuevo)
}

private fun revisarNivel(nivel: LecturaNivel, estado: EstadoNotificaciones): Pair<List<Notificacion>, Boolean> {
    val anterior = estado.ultimoPorcentaje ?: return emptyList<Notificacion>() to false
    val porcentaje = nivel.porcentaje.roundToInt()
    val ambosDelSensor = nivel.delSensor && estado.ultimoDelSensor
    val cambio = nivel.porcentaje - anterior
    val subiendo = ambosDelSensor && cambio >= SUBIDA_MINIMA
    val avisos = buildList {
        if (subiendo && !estado.llenandose) {
            add(Notificacion(TipoNotificacion.AGUA_REGRESO, "Volvió el agua", "Tu tanque se está llenando: va en $porcentaje %."))
        }
        if (nivel.delSensor && anterior < NIVEL_LLENO && nivel.porcentaje >= NIVEL_LLENO) {
            add(Notificacion(TipoNotificacion.TANQUE_LLENO, "Tanque lleno", "Tu tanque llegó al $porcentaje %. Ya puedes cerrar la llave."))
        }
        if (anterior > NIVEL_BAJO && nivel.porcentaje <= NIVEL_BAJO) {
            add(Notificacion(TipoNotificacion.TANQUE_BAJO, "Tu tanque está al $porcentaje %", "Queda poca agua: cuida el consumo hasta el próximo llenado."))
        }
    }
    val llenandose = when {
        subiendo -> true
        cambio <= 0 -> false
        else -> estado.llenandose
    }
    return avisos to llenandose
}
