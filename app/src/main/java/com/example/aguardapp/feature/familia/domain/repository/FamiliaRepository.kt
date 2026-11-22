package com.example.aguardapp.feature.familia.domain.repository

import kotlinx.coroutines.flow.Flow
import com.example.aguardapp.feature.familia.domain.model.EstadoSincronizacion
import com.example.aguardapp.feature.familia.domain.model.Invitacion

interface FamiliaRepository {
    val enFamilia: Flow<Boolean>

    fun apodo(): String?

    /** Momento de la última sincronización correcta, en milisegundos, o null si nunca se sincronizó. */
    fun ultimaSincronizacion(): Long?

    suspend fun crear(apodo: String): Result<Unit>

    /** Al unirse, los datos del depósito del teléfono se reemplazan por los de la familia en la siguiente sincronización. */
    suspend fun unirse(codigo: String, apodo: String): Result<Unit>

    suspend fun invitar(): Result<Invitacion>

    suspend fun sincronizar(): Result<EstadoSincronizacion>

    suspend fun salir(): Result<Unit>

    /** Saca a todos los demás y cambia el secreto del hogar. */
    suspend fun reiniciar(): Result<Unit>
}
