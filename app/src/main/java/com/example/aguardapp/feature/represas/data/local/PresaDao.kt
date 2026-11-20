package com.example.aguardapp.feature.represas.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PresaDao {
    @Query("SELECT * FROM presa ORDER BY volumenHm3 DESC")
    fun todas(): Flow<List<PresaEntity>>

    @Upsert
    suspend fun guardar(presas: List<PresaEntity>)
}
