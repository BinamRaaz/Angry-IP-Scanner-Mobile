package com.example.core.speedtest

enum class SpeedTestPhase(val label: String) {
    IDLE("Ready to Test"),
    PING("Testing Latency & Jitter…"),
    DOWNLOAD("Testing Download Speed…"),
    UPLOAD("Testing Upload Speed…"),
    COMPLETED("Test Complete"),
    CANCELLED("Test Cancelled"),
    ERROR("Test Failed")
}

data class SpeedSample(
    val timestampMs: Long,
    val speedMbps: Double
)

data class SpeedTestProgress(
    val phase: SpeedTestPhase = SpeedTestPhase.IDLE,
    val progressFraction: Float = 0f,
    val isRunning: Boolean = false,
    val currentPingMs: Double? = null,
    val minPingMs: Double? = null,
    val avgPingMs: Double? = null,
    val maxPingMs: Double? = null,
    val jitterMs: Double? = null,
    val currentDownloadMbps: Double = 0.0,
    val peakDownloadMbps: Double = 0.0,
    val currentUploadMbps: Double = 0.0,
    val peakUploadMbps: Double = 0.0,
    val totalDownloadBytes: Long = 0L,
    val totalUploadBytes: Long = 0L,
    val serverName: String = "Global CDN (Edge)",
    val clientIp: String? = null,
    val downloadSamples: List<SpeedSample> = emptyList(),
    val uploadSamples: List<SpeedSample> = emptyList(),
    val errorMessage: String? = null
)

data class SpeedTestResult(
    val id: Long = 0L,
    val timestamp: Long = System.currentTimeMillis(),
    val pingMs: Double,
    val jitterMs: Double,
    val downloadMbps: Double,
    val uploadMbps: Double,
    val serverName: String,
    val clientIp: String? = null
)
