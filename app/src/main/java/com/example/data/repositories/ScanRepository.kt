package com.example.data.repositories

import com.example.data.models.ScanSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repository interface for managing scan history and cached scan results.
 */
interface ScanRepository {
    fun getScanHistory(): Flow<List<ScanSession>>
    suspend fun saveSession(session: ScanSession)
    suspend fun deleteSession(sessionId: String)
    suspend fun clearHistory()
}

/**
 * In-memory repository implementation for initial foundation.
 */
class InMemoryScanRepository : ScanRepository {
    private val sessions = MutableStateFlow<List<ScanSession>>(emptyList())

    override fun getScanHistory(): Flow<List<ScanSession>> = sessions.asStateFlow()

    override suspend fun saveSession(session: ScanSession) {
        sessions.value = listOf(session) + sessions.value
    }

    override suspend fun deleteSession(sessionId: String) {
        sessions.value = sessions.value.filterNot { it.id == sessionId }
    }

    override suspend fun clearHistory() {
        sessions.value = emptyList()
    }
}
