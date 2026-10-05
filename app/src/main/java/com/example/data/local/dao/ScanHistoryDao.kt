package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.data.local.entities.ScanSessionEntity
import com.example.data.local.entities.ScannedHostEntity
import com.example.data.local.models.ScanSessionWithHosts
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ScanSessionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHosts(hosts: List<ScannedHostEntity>)

    @Query("SELECT * FROM scan_sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<ScanSessionEntity>>

    @Transaction
    @Query("SELECT * FROM scan_sessions WHERE id = :sessionId")
    suspend fun getSessionWithHosts(sessionId: Long): ScanSessionWithHosts?

    @Transaction
    @Query("SELECT * FROM scan_sessions WHERE id = :sessionId")
    fun observeSessionWithHosts(sessionId: Long): Flow<ScanSessionWithHosts?>

    @Query("DELETE FROM scan_sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: Long)

    @Query("DELETE FROM scan_sessions")
    suspend fun clearAllSessions()
}
