package com.example.persistence

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface TweakDao {
    @Query("SELECT * FROM tweak_configs WHERE id = :configId LIMIT 1")
    fun getConfigFlow(configId: String = "vivot2x_default"): Flow<TweakConfigs?>

    @Query("SELECT * FROM tweak_configs WHERE id = :configId LIMIT 1")
    suspend fun getConfigDirect(configId: String = "vivot2x_default"): TweakConfigs?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveConfig(configs: TweakConfigs)

    @Query("SELECT * FROM ping_logs ORDER BY timestamp DESC LIMIT 40")
    fun getRecentPingLogs(): Flow<List<PingLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPingLog(log: PingLog)

    @Query("DELETE FROM ping_logs")
    suspend fun clearPingLogs()
}

@Database(entities = [TweakConfigs::class, PingLog::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tweakDao(): TweakDao
}
