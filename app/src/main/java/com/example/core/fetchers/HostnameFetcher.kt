package com.example.core.fetchers

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress

class HostnameFetcher : HostFetcher {
    override val id: FetcherId = FetcherId.HOSTNAME
    override val displayName: String = "Hostname"

    override suspend fun fetch(ip: String, context: FetchContext): Unit = withContext(Dispatchers.IO) {
        if (!context.isAlive) return@withContext
        try {
            val addr = InetAddress.getByName(ip)
            val canonical = addr.canonicalHostName
            if (canonical != ip) {
                context.hostname = canonical
            }
        } catch (_: Exception) {
            // DNS resolution failure
        }
    }
}
