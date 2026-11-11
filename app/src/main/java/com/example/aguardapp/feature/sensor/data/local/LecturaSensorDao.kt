package com.example.aguardapp.feature.sensor.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LecturaSensorDao {
    @Query("SELECT * FROM lectura_sensor WHERE id = 0")
    fun observar(): Flow<LecturaSensorEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(lectura: LecturaSensorEntity)
}
