package com.example.core.models

/**
 * Real-time progress metric during an active scan.
 */
data class ScanProgress(
    val scannedCount: Int = 0,
    val totalCount: Int = 0,
    val aliveCount: Int = 0,
    val deadCount: Int = 0,
    val isScanning: Boolean = false,
    val currentIp: String? = null
) {
    val progressFraction: Float
        get() = if (totalCount > 0) scannedCount.toFloat() / totalCount else 0f
}
