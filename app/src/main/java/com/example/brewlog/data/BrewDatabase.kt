package com.example.brewlog.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [BrewLog::class, EspressoMachine::class, ShotLog::class], version = 12, exportSchema = false)
@TypeConverters(Converters::class)
abstract class BrewDatabase : RoomDatabase() {
    abstract fun brewLogDao(): BrewLogDao

    companion object {
        @Volatile
        private var Instance: BrewDatabase? = null

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE brew_logs ADD COLUMN roastDate TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getDatabase(context: Context): BrewDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, BrewDatabase::class.java, "brew_database")
                    .addMigrations(MIGRATION_11_12)
                    .fallbackToDestructiveMigrationOnDowngrade(true)
                    .build()
                    .also { Instance = it }
            }
        }
    }
}
