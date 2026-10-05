package com.example.data.models

import com.example.core.models.DiscoveredHost

/**
 * Model representing a completed or historical scan session.
 */
data class ScanSession(
    val id: String,
    val timestamp: Long,
    val rangeDescription: String,
    val totalHostsScanned: Int,
    val aliveCount: Int,
    val durationMs: Long,
    val hosts: List<DiscoveredHost> = emptyList()
)
