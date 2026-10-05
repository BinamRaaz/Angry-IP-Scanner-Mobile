package com.example.core.models

/**
 * Model representing a discovered network host and its fetched attributes.
 */
data class DiscoveredHost(
    val ip: String,
    val hostname: String? = null,
    val status: HostStatus = HostStatus.UNKNOWN,
    val responseTimeMs: Long? = null,
    val ttl: Int? = null,
    val macAddress: String? = null,
    val vendor: String? = null,
    val openPorts: List<Int> = emptyList(),
    val comment: String? = null,
    val scannedAt: Long = System.currentTimeMillis()
)
