package com.example.aguardapp.feature.represas.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

// Clase abstracta (y no interfaz) para poder escribir reemplazarPresas dentro de una transacción.
@Dao
abstract class RepresasDao {

    @Query("SELECT * FROM presa ORDER BY volumenUtilHm3 IS NULL, volumenUtilHm3 DESC, nombre")
    abstract fun observarPresas(): Flow<List<PresaEntity>>

    @Query("SELECT MAX(descargadaEn) FROM presa")
    abstract fun observarDescarga(): Flow<String?>

    @Query("SELECT MAX(descargadaEn) FROM presa")
    abstract suspend fun fechaDeDescarga(): String?

    /** Cambia el inventario guardado por el recién descargado; si algo falla, queda el anterior. */
    @Transaction
    open suspend fun reemplazarPresas(presas: List<PresaEntity>) {
        borrarPresas()
        guardarPresas(presas)
    }

    @Query("DELETE FROM presa")
    abstract suspend fun borrarPresas()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun guardarPresas(presas: List<PresaEntity>)

    @Query("SELECT * FROM distrito_hogar WHERE usuarioId = :usuarioId")
    abstract fun observarDistrito(usuarioId: String): Flow<DistritoHogarEntity?>

    @Upsert
    abstract suspend fun guardarDistrito(distrito: DistritoHogarEntity)
}
