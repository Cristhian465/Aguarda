package com.example.aguardapp.feature.represas.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PresaDao {
    /** Las represas guardadas, de la más grande a la más chica. Se actualiza sola cuando cambia la tabla. */
    @Query("SELECT * FROM presa ORDER BY volumenHm3 DESC")
    fun todas(): Flow<List<Presa>>

    @Upsert
    suspend fun guardar(presas: List<Presa>)
}
