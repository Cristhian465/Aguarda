package com.example.aguardapp.feature.familia.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.example.aguardapp.feature.deposito.data.local.EventoLlenadoEntity
import com.example.aguardapp.feature.deposito.data.local.NovedadDepositoEntity
import com.example.aguardapp.feature.deposito.data.local.PerfilHogarEntity

/** Lee y escribe las tablas del depósito para sincronizarlas con la familia. No agrega tablas nuevas. */
@Dao
abstract class FamiliaDao {
    @Query("SELECT * FROM perfil_hogar WHERE usuarioId = :usuarioId")
    abstract suspend fun perfil(usuarioId: String): PerfilHogarEntity?

    @Query("SELECT * FROM evento_llenado WHERE usuarioId = :usuarioId")
    abstract suspend fun llenados(usuarioId: String): List<EventoLlenadoEntity>

    @Query("SELECT * FROM novedad_deposito WHERE usuarioId = :usuarioId")
    abstract suspend fun novedades(usuarioId: String): List<NovedadDepositoEntity>

    @Upsert
    abstract suspend fun guardarPerfil(perfil: PerfilHogarEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertarLlenados(llenados: List<EventoLlenadoEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertarNovedades(novedades: List<NovedadDepositoEntity>)

    @Query("DELETE FROM evento_llenado WHERE usuarioId = :usuarioId")
    abstract suspend fun borrarLlenados(usuarioId: String)

    @Query("DELETE FROM novedad_deposito WHERE usuarioId = :usuarioId")
    abstract suspend fun borrarNovedades(usuarioId: String)

    /** Aplica lo que llegó de la familia en una sola transacción. Con reemplazar, primero borra el historial local. */
    @Transaction
    open suspend fun aplicar(
        usuarioId: String,
        reemplazar: Boolean,
        perfil: PerfilHogarEntity?,
        llenados: List<EventoLlenadoEntity>,
        novedades: List<NovedadDepositoEntity>
    ) {
        if (reemplazar) {
            borrarLlenados(usuarioId)
            borrarNovedades(usuarioId)
        }
        perfil?.let { guardarPerfil(it) }
        if (llenados.isNotEmpty()) insertarLlenados(llenados)
        if (novedades.isNotEmpty()) insertarNovedades(novedades)
    }
}
