package com.example.core.models

import com.example.core.fetchers.FetcherId

/**
 * Parameters defining the range and configuration of an IP scan.
 */
data class ScanTarget(
    val startIp: String,
    val endIp: String,
    val cidr: String? = null,
    val timeoutMs: Int = 1000,
    val maxConcurrency: Int = 32,
    val scanPorts: Boolean = false,
    val portsToScan: List<Int> = listOf(80, 443, 22, 8080),
    val enabledFetchers: Set<FetcherId> = setOf(FetcherId.PING, FetcherId.HOSTNAME)
)
