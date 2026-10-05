package com.example.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.preferences.AppThemeMode
import com.example.core.preferences.DefaultPreferencesRepository
import com.example.core.preferences.PingMethod
import com.example.core.preferences.PreferencesRepository
import com.example.core.preferences.ScannerPreferences
import com.example.core.preferences.scannerDataStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    application: Application,
    private val repository: PreferencesRepository = DefaultPreferencesRepository(application.scannerDataStore)
) : AndroidViewModel(application) {

    val preferences: StateFlow<ScannerPreferences> = repository.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ScannerPreferences())

    fun setPingTimeout(timeoutMs: Int) {
        viewModelScope.launch {
            repository.setPingTimeoutMs(timeoutMs)
        }
    }

    fun setConcurrency(threads: Int) {
        viewModelScope.launch {
            repository.setConcurrency(threads)
        }
    }

    fun setPingMethod(method: PingMethod) {
        viewModelScope.launch {
            repository.setPingMethod(method)
        }
    }

    fun setPortTimeout(timeoutMs: Int) {
        viewModelScope.launch {
            repository.setPortTimeoutMs(timeoutMs)
        }
    }

    fun setDefaultPorts(ports: String) {
        viewModelScope.launch {
            repository.setDefaultPorts(ports)
        }
    }

    fun setAutoSaveHistory(enabled: Boolean) {
        viewModelScope.launch {
            repository.setAutoSaveHistory(enabled)
        }
    }

    fun setDefaultShowAliveOnly(enabled: Boolean) {
        viewModelScope.launch {
            repository.setDefaultShowAliveOnly(enabled)
        }
    }

    fun setThemeMode(mode: AppThemeMode) {
        viewModelScope.launch {
            repository.setThemeMode(mode)
        }
    }

    fun setResolveHostnames(enabled: Boolean) {
        viewModelScope.launch {
            repository.setResolveHostnames(enabled)
        }
    }

    fun setFetchTtl(enabled: Boolean) {
        viewModelScope.launch {
            repository.setFetchTtl(enabled)
        }
    }

    fun setFetchMacVendor(enabled: Boolean) {
        viewModelScope.launch {
            repository.setFetchMacVendor(enabled)
        }
    }

    fun setScanPorts(enabled: Boolean) {
        viewModelScope.launch {
            repository.setScanPorts(enabled)
        }
    }

    fun resetToDefaults() {
        viewModelScope.launch {
            repository.resetToDefaults()
        }
    }
}
