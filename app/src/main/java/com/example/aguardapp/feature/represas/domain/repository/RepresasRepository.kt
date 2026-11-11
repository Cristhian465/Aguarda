package com.example.aguardapp.feature.represas.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDateTime
import com.example.aguardapp.feature.mapa.domain.model.Coordenada
import com.example.aguardapp.feature.represas.domain.model.Distrito
import com.example.aguardapp.feature.represas.domain.model.DistritoDelHogar
import com.example.aguardapp.feature.represas.domain.model.Presa

interface RepresasRepository {
    /** Las represas guardadas en el teléfono, de mayor a menor volumen útil. Funciona sin internet. */
    fun observarPresas(): Flow<List<Presa>>

    /** Cuándo se descargaron por última vez de la ANA; null si nunca. */
    fun observarDescarga(): Flow<LocalDateTime?>

    /** Descarga el inventario de la ANA si lo guardado está vencido (o siempre, con [forzar]). */
    suspend fun actualizarPresas(forzar: Boolean = false): Result<Unit>

    fun observarDistrito(): Flow<DistritoDelHogar?>

    suspend fun elegirDistrito(distrito: Distrito)

    /** Averigua el distrito de [ubicacion] con el buscador de direcciones y lo guarda. */
    suspend fun detectarDistrito(ubicacion: Coordenada): Result<Distrito>
}
