package com.example.ui.speedtest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.speedtest.DefaultSpeedTestEngine
import com.example.core.speedtest.SpeedTestEngine
import com.example.core.speedtest.SpeedTestPhase
import com.example.core.speedtest.SpeedTestProgress
import com.example.core.speedtest.SpeedTestResult
import com.example.data.repository.SpeedTestRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class SpeedTestViewModel(
    private val speedTestEngine: SpeedTestEngine = DefaultSpeedTestEngine()
) : ViewModel() {

    private var speedTestRepository: SpeedTestRepository? = null
    private var activeTestJob: Job? = null

    private val _progress = MutableStateFlow(SpeedTestProgress())
    val progress: StateFlow<SpeedTestProgress> = _progress.asStateFlow()

    private val _history = MutableStateFlow<List<SpeedTestResult>>(emptyList())
    val history: StateFlow<List<SpeedTestResult>> = _history.asStateFlow()

    private val _lastCompletedResult = MutableStateFlow<SpeedTestResult?>(null)
    val lastCompletedResult: StateFlow<SpeedTestResult?> = _lastCompletedResult.asStateFlow()

    fun setRepository(repo: SpeedTestRepository) {
        speedTestRepository = repo
        viewModelScope.launch {
            repo.getAllSpeedTests().collect { list ->
                _history.value = list
            }
        }
    }

    fun startSpeedTest() {
        if (_progress.value.isRunning) return

        activeTestJob?.cancel()
        activeTestJob = viewModelScope.launch {
            _lastCompletedResult.value = null
            _progress.value = SpeedTestProgress(
                phase = SpeedTestPhase.PING,
                isRunning = true,
                progressFraction = 0.05f
            )

            speedTestEngine.runSpeedTest()
                .catch { e ->
                    _progress.value = _progress.value.copy(
                        phase = SpeedTestPhase.ERROR,
                        isRunning = false,
                        errorMessage = e.message ?: "Speed test error"
                    )
                }
                .collect { state ->
                    _progress.value = state
                    if (state.phase == SpeedTestPhase.COMPLETED) {
                        val result = SpeedTestResult(
                            pingMs = state.avgPingMs ?: state.minPingMs ?: 20.0,
                            jitterMs = state.jitterMs ?: 1.5,
                            downloadMbps = state.peakDownloadMbps.coerceAtLeast(state.currentDownloadMbps),
                            uploadMbps = state.peakUploadMbps.coerceAtLeast(state.currentUploadMbps),
                            serverName = state.serverName,
                            clientIp = state.clientIp
                        )
                        _lastCompletedResult.value = result

                        // Auto-save to repository if available
                        speedTestRepository?.let { repo ->
                            launch {
                                repo.saveSpeedTest(result)
                            }
                        }
                    }
                }
        }
    }

    fun cancelSpeedTest() {
        speedTestEngine.cancel()
        activeTestJob?.cancel()
        activeTestJob = null
        _progress.value = _progress.value.copy(
            phase = SpeedTestPhase.CANCELLED,
            isRunning = false
        )
    }

    fun deleteHistoryItem(id: Long) {
        viewModelScope.launch {
            speedTestRepository?.deleteSpeedTest(id)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            speedTestRepository?.clearAll()
        }
    }

    fun resetState() {
        if (!_progress.value.isRunning) {
            _progress.value = SpeedTestProgress()
            _lastCompletedResult.value = null
        }
    }

    override fun onCleared() {
        super.onCleared()
        speedTestEngine.cancel()
        activeTestJob?.cancel()
    }
}
