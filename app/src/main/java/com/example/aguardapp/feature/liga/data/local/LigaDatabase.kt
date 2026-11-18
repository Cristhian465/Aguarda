package com.example.aguardapp.feature.liga.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

/** Nivel del tanque marcado por el usuario. Se guarda solo en el teléfono. */
@Entity(tableName = "marca_nivel")
data class MarcaNivelEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val momento: String,
    val litros: Double
)

@Dao
interface LigaDao {
    @Query("SELECT * FROM marca_nivel ORDER BY momento")
    fun observarMarcas(): Flow<List<MarcaNivelEntity>>

    @Insert
    suspend fun guardarMarca(marca: MarcaNivelEntity)
}

@Database(entities = [MarcaNivelEntity::class], version = 1, exportSchema = false)
abstract class LigaDatabase : RoomDatabase() {
    abstract fun ligaDao(): LigaDao

    companion object {
        fun crear(context: Context): LigaDatabase =
            Room.databaseBuilder(context.applicationContext, LigaDatabase::class.java, "liga.db").build()
    }
}
