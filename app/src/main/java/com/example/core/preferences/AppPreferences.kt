package com.example.core.preferences

enum class PingMethod(val displayName: String, val description: String) {
    AUTO("Auto (Smart Ping)", "Attempts ICMP system ping with fallback to TCP/Socket"),
    ICMP("ICMP Echo", "Invokes system ping utility for low-level ICMP echo packets"),
    IS_REACHABLE("Java isReachable", "Uses standard Java network stack reachability check"),
    TCP_PING("TCP Port Echo", "Attempts rapid connection to standard ports (80/443)")
}

enum class AppThemeMode(val displayName: String) {
    SYSTEM("Follow System"),
    LIGHT("Light Mode"),
    DARK("Dark Mode")
}

data class ScannerPreferences(
    val pingTimeoutMs: Int = 800,
    val concurrency: Int = 24,
    val pingMethod: PingMethod = PingMethod.AUTO,
    val portTimeoutMs: Int = 300,
    val defaultPorts: String = "22, 80, 443, 8080",
    val autoSaveHistory: Boolean = true,
    val defaultShowAliveOnly: Boolean = false,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val resolveHostnames: Boolean = true,
    val fetchTtl: Boolean = true,
    val fetchMacVendor: Boolean = true,
    val scanPorts: Boolean = true
)
