package com.example.core.scanner

import com.example.core.models.DiscoveredHost
import com.example.core.models.ScanProgress
import com.example.core.models.ScanTarget
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Core Scanner Engine contract. Decoupled from UI and Android Jetpack Compose.
 * Uses Kotlin Coroutines and Flows for non-blocking asynchronous network scanning.
 */
interface ScannerEngine {
    val progress: StateFlow<ScanProgress>
    val results: StateFlow<List<DiscoveredHost>>

    /**
     * Initiates a scan across the targets specified in [target].
     * Emits discovered hosts as they complete their probes.
     */
    fun startScan(target: ScanTarget): Flow<DiscoveredHost>

    /**
     * Gracefully halts any running scan coroutines.
     */
    fun stopScan()

    /**
     * Probes an individual host and updates result if present.
     */
    suspend fun probeSingleHost(ip: String, target: ScanTarget): DiscoveredHost
}
