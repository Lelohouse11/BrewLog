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
    suspend fun insertLog(log: BrewLog): Long

    @Update
    suspend fun updateLog(log: BrewLog)

    @Delete
    suspend fun deleteLog(log: BrewLog)

    @Query("SELECT shot_logs.*, brew_logs.coffeeName as beanName FROM shot_logs INNER JOIN brew_logs ON shot_logs.beanId = brew_logs.id ORDER BY shot_logs.timestamp DESC")
    fun getAllShotLogsWithBean(): Flow<List<ShotLogWithBean>>

    @Query("SELECT * FROM shot_logs")
    suspend fun getAllShotLogsRaw(): List<ShotLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShotLog(shotLog: ShotLog)

    @Query("SELECT * FROM shot_logs WHERE beanId = :beanId AND basketType = :basketType AND acidityEval = 0 AND bitternessEval = 0 AND bodyEval = 0 ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestBalancedShot(beanId: Int, basketType: String): ShotLog?

    // Espresso Machine
    @Query("SELECT * FROM espresso_machine WHERE id = 1")
    fun getEspressoMachine(): Flow<EspressoMachine?>

    @Query("SELECT * FROM espresso_machine WHERE id = 1")
    suspend fun getEspressoMachineOnce(): EspressoMachine?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEspressoMachine(machine: EspressoMachine)

    @Query("DELETE FROM espresso_machine")
    suspend fun clearEspressoMachine()
}
