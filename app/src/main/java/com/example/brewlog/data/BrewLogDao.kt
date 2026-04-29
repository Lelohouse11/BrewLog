package com.example.brewlog.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BrewLogDao {
    @Query("SELECT * FROM brew_logs ORDER BY id DESC")
    fun getAllLogs(): Flow<List<BrewLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: BrewLog)

    @Update
    suspend fun updateLog(log: BrewLog)

    @Delete
    suspend fun deleteLog(log: BrewLog)
}
