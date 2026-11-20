package com.example.aguardapp.core.db

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.aguardapp.core.data.UsuarioDao
import com.example.aguardapp.core.data.UsuarioEntity
import com.example.aguardapp.feature.deposito.data.local.DepositoDao
import com.example.aguardapp.feature.deposito.data.local.EventoLlenadoEntity
import com.example.aguardapp.feature.deposito.data.local.NovedadDepositoEntity
import com.example.aguardapp.feature.deposito.data.local.PerfilHogarEntity
import com.example.aguardapp.feature.deposito.data.local.PlanRecortesEntity
import com.example.aguardapp.feature.represas.data.local.PresaEntity
import com.example.aguardapp.feature.represas.data.local.PresaDao

private const val NOMBRE_BASE_DE_DATOS = "aguardapp.db"

@Database(
    entities = [
        UsuarioEntity::class,
        PerfilHogarEntity::class,
        EventoLlenadoEntity::class,
        NovedadDepositoEntity::class,
        PlanRecortesEntity::class,
        PresaEntity::class
    ],
    version = 4,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 2, to = 3), AutoMigration(from = 3, to = 4)]
)
abstract class AguardAppDatabase : RoomDatabase() {
    abstract fun usuarioDao(): UsuarioDao
    abstract fun depositoDao(): DepositoDao
    abstract fun presaDao(): PresaDao

    companion object {
        fun crear(context: Context): AguardAppDatabase =
            Room.databaseBuilder(context.applicationContext, AguardAppDatabase::class.java, NOMBRE_BASE_DE_DATOS)
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
    }
}
