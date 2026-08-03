package com.example.brewlog.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [BrewLog::class], version = 8, exportSchema = false)
@TypeConverters(Converters::class)
abstract class BrewDatabase : RoomDatabase() {
    abstract fun brewLogDao(): BrewLogDao

    companion object {
        @Volatile
        private var Instance: BrewDatabase? = null

        fun getDatabase(context: Context): BrewDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, BrewDatabase::class.java, "brew_database")
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { Instance = it }
            }
        }
    }
}
