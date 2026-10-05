package com.example.core.fetchers

import com.example.core.models.HostStatus

/**
 * Shared context between fetchers during a single host scan,
 * preventing duplicate network requests.
 */
data class FetchContext(
    var isAlive: Boolean = false,
    var responseTimeMs: Long? = null,
    var hostname: String? = null,
    var ttl: Int? = null,
    var macAddress: String? = null,
    var vendor: String? = null,
    var openPorts: List<Int> = emptyList(),
    val timeoutMs: Int = 800
)

/**
 * Interface for modular host attribute extractors, directly inspired by Angry IP Scanner's fetcher plugin model.
 */
interface HostFetcher {
    val id: FetcherId
    val displayName: String

    /**
     * Executes the fetch logic for [ip], reading and populating [context].
     */
    suspend fun fetch(ip: String, context: FetchContext)
}
