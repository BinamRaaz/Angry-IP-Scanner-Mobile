package com.example.core.fetchers

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Modular fetcher that scans specified TCP ports on responsive hosts.
 * Optimized with immediate soLinger reset to prevent socket descriptor exhaustion.
 */
class PortFetcher(
    val portsToScan: List<Int> = listOf(22, 80, 443, 8080),
    val portTimeoutMs: Int = 300
) : HostFetcher {
    override val id: FetcherId = FetcherId.PORTS
    override val displayName: String = "Open Ports"

    override suspend fun fetch(ip: String, context: FetchContext): Unit = withContext(Dispatchers.IO) {
        if (!context.isAlive) return@withContext

        val discoveredPorts = mutableListOf<Int>()
        val timeout = portTimeoutMs.coerceIn(50, 2000)

        for (port in portsToScan) {
            try {
                Socket().use { socket ->
                    socket.reuseAddress = true
                    socket.setSoLinger(true, 0)
                    socket.connect(InetSocketAddress(ip, port), timeout)
                    discoveredPorts.add(port)
                }
            } catch (_: Exception) {
                // Closed port, timed out, or connection refused
            }
        }

        context.openPorts = discoveredPorts
    }
}
