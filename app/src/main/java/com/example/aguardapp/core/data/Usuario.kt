package com.example.aguardapp.core.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "usuario")
data class UsuarioEntity(
    @PrimaryKey val id: String,
    val bienvenidaCompletada: Boolean = false
)

@Dao
interface UsuarioDao {
    @Query("SELECT * FROM usuario LIMIT 1")
    suspend fun obtener(): UsuarioEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(usuario: UsuarioEntity)

    @Query("SELECT bienvenidaCompletada FROM usuario LIMIT 1")
    fun observarBienvenidaCompletada(): Flow<Boolean?>

    @Query("UPDATE usuario SET bienvenidaCompletada = 1")
    suspend fun completarBienvenida()
}
