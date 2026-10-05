package com.example.ui.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entities.ScanSessionEntity
import com.example.data.local.models.ScanSessionWithHosts
import com.example.data.repository.DefaultScanHistoryRepository
import com.example.data.repository.ScanHistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(
    application: Application,
    private val repository: ScanHistoryRepository = DefaultScanHistoryRepository(
        AppDatabase.getDatabase(application).scanHistoryDao()
    )
) : AndroidViewModel(application) {

    val sessions: StateFlow<List<ScanSessionEntity>> = repository.getAllSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedSessionDetail = MutableStateFlow<ScanSessionWithHosts?>(null)
    val selectedSessionDetail: StateFlow<ScanSessionWithHosts?> = _selectedSessionDetail.asStateFlow()

    fun selectSession(sessionId: Long) {
        viewModelScope.launch {
            _selectedSessionDetail.value = repository.getSessionWithHosts(sessionId)
        }
    }

    fun clearSelectedSession() {
        _selectedSessionDetail.value = null
    }

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
            if (_selectedSessionDetail.value?.session?.id == sessionId) {
                _selectedSessionDetail.value = null
            }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearHistory()
            _selectedSessionDetail.value = null
        }
    }
}
