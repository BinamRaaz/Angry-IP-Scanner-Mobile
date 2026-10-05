package com.example.core.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.scannerDataStore: DataStore<Preferences> by preferencesDataStore(name = "scanner_settings")

interface PreferencesRepository {
    val preferencesFlow: Flow<ScannerPreferences>
    suspend fun setPingTimeoutMs(timeout: Int)
    suspend fun setConcurrency(threads: Int)
    suspend fun setPingMethod(method: PingMethod)
    suspend fun setPortTimeoutMs(timeout: Int)
    suspend fun setDefaultPorts(ports: String)
    suspend fun setAutoSaveHistory(enabled: Boolean)
    suspend fun setDefaultShowAliveOnly(enabled: Boolean)
    suspend fun setThemeMode(mode: AppThemeMode)
    suspend fun setResolveHostnames(enabled: Boolean)
    suspend fun setFetchTtl(enabled: Boolean)
    suspend fun setFetchMacVendor(enabled: Boolean)
    suspend fun setScanPorts(enabled: Boolean)
    suspend fun resetToDefaults()
}

class DefaultPreferencesRepository(
    private val dataStore: DataStore<Preferences>
) : PreferencesRepository {

    private object PreferencesKeys {
        val PING_TIMEOUT_MS = intPreferencesKey("ping_timeout_ms")
        val CONCURRENCY = intPreferencesKey("concurrency")
        val PING_METHOD = stringPreferencesKey("ping_method")
        val PORT_TIMEOUT_MS = intPreferencesKey("port_timeout_ms")
        val DEFAULT_PORTS = stringPreferencesKey("default_ports")
        val AUTO_SAVE_HISTORY = booleanPreferencesKey("auto_save_history")
        val DEFAULT_SHOW_ALIVE_ONLY = booleanPreferencesKey("default_show_alive_only")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val RESOLVE_HOSTNAMES = booleanPreferencesKey("resolve_hostnames")
        val FETCH_TTL = booleanPreferencesKey("fetch_ttl")
        val FETCH_MAC_VENDOR = booleanPreferencesKey("fetch_mac_vendor")
        val SCAN_PORTS = booleanPreferencesKey("scan_ports")
    }

    override val preferencesFlow: Flow<ScannerPreferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val pingTimeout = preferences[PreferencesKeys.PING_TIMEOUT_MS] ?: 800
            val concurrency = preferences[PreferencesKeys.CONCURRENCY] ?: 24
            val pingMethodStr = preferences[PreferencesKeys.PING_METHOD] ?: PingMethod.AUTO.name
            val pingMethod = try { PingMethod.valueOf(pingMethodStr) } catch (_: Exception) { PingMethod.AUTO }
            val portTimeout = preferences[PreferencesKeys.PORT_TIMEOUT_MS] ?: 300
            val defaultPorts = preferences[PreferencesKeys.DEFAULT_PORTS] ?: "22, 80, 443, 8080"
            val autoSave = preferences[PreferencesKeys.AUTO_SAVE_HISTORY] ?: true
            val showAliveOnly = preferences[PreferencesKeys.DEFAULT_SHOW_ALIVE_ONLY] ?: false
            val themeStr = preferences[PreferencesKeys.THEME_MODE] ?: AppThemeMode.SYSTEM.name
            val theme = try { AppThemeMode.valueOf(themeStr) } catch (_: Exception) { AppThemeMode.SYSTEM }
            val resolveHostnames = preferences[PreferencesKeys.RESOLVE_HOSTNAMES] ?: true
            val fetchTtl = preferences[PreferencesKeys.FETCH_TTL] ?: true
            val fetchMac = preferences[PreferencesKeys.FETCH_MAC_VENDOR] ?: true
            val scanPorts = preferences[PreferencesKeys.SCAN_PORTS] ?: true

            ScannerPreferences(
                pingTimeoutMs = pingTimeout,
                concurrency = concurrency,
                pingMethod = pingMethod,
                portTimeoutMs = portTimeout,
                defaultPorts = defaultPorts,
                autoSaveHistory = autoSave,
                defaultShowAliveOnly = showAliveOnly,
                themeMode = theme,
                resolveHostnames = resolveHostnames,
                fetchTtl = fetchTtl,
                fetchMacVendor = fetchMac,
                scanPorts = scanPorts
            )
        }

    override suspend fun setPingTimeoutMs(timeout: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.PING_TIMEOUT_MS] = timeout
        }
    }

    override suspend fun setConcurrency(threads: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.CONCURRENCY] = threads
        }
    }

    override suspend fun setPingMethod(method: PingMethod) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.PING_METHOD] = method.name
        }
    }

    override suspend fun setPortTimeoutMs(timeout: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.PORT_TIMEOUT_MS] = timeout
        }
    }

    override suspend fun setDefaultPorts(ports: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEFAULT_PORTS] = ports
        }
    }

    override suspend fun setAutoSaveHistory(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_SAVE_HISTORY] = enabled
        }
    }

    override suspend fun setDefaultShowAliveOnly(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEFAULT_SHOW_ALIVE_ONLY] = enabled
        }
    }

    override suspend fun setThemeMode(mode: AppThemeMode) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode.name
        }
    }

    override suspend fun setResolveHostnames(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.RESOLVE_HOSTNAMES] = enabled
        }
    }

    override suspend fun setFetchTtl(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.FETCH_TTL] = enabled
        }
    }

    override suspend fun setFetchMacVendor(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.FETCH_MAC_VENDOR] = enabled
        }
    }

    override suspend fun setScanPorts(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SCAN_PORTS] = enabled
        }
    }

    override suspend fun resetToDefaults() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
