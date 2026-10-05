package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entities.SpeedTestEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SpeedTestDao {

    @Query("SELECT * FROM speed_test_history ORDER BY timestamp DESC")
    fun getAllSpeedTests(): Flow<List<SpeedTestEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpeedTest(speedTest: SpeedTestEntity): Long

    @Query("DELETE FROM speed_test_history WHERE id = :id")
    suspend fun deleteSpeedTestById(id: Long)

    @Query("DELETE FROM speed_test_history")
    suspend fun clearAllSpeedTests()

    @Query("SELECT COUNT(*) FROM speed_test_history")
    suspend fun getCount(): Int
}
