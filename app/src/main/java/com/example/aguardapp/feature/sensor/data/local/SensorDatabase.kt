package com.example.aguardapp.feature.sensor.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [LecturaSensorEntity::class], version = 1, exportSchema = false)
abstract class SensorDatabase : RoomDatabase() {
    abstract fun lecturaDao(): LecturaSensorDao

    companion object {
        fun crear(context: Context): SensorDatabase =
            Room.databaseBuilder(context.applicationContext, SensorDatabase::class.java, "sensor.db")
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
    }
}
