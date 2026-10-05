package com.example.core.fetchers

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

class PingFetcher : HostFetcher {
    override val id: FetcherId = FetcherId.PING
    override val displayName: String = "Ping / Latency"

    override suspend fun fetch(ip: String, context: FetchContext): Unit = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        var alive = false
        var latency: Long? = null

        val testPorts = listOf(80, 443, 22, 53, 8080)
        for (port in testPorts) {
            try {
                Socket().use { socket ->
                    socket.reuseAddress = true
                    socket.setSoLinger(true, 0)
                    socket.connect(InetSocketAddress(ip, port), context.timeoutMs.coerceAtMost(250))
                    alive = true
                    latency = System.currentTimeMillis() - startTime
                }
                if (alive) break
            } catch (e: Exception) {
                val message = e.message?.lowercase() ?: ""
                if (message.contains("refused") || message.contains("reset")) {
                    alive = true
                    latency = System.currentTimeMillis() - startTime
                    break
                }
            }
        }

        if (!alive) {
            try {
                val addr = InetAddress.getByName(ip)
                if (addr.isReachable(context.timeoutMs.coerceAtMost(350))) {
                    alive = true
                    latency = System.currentTimeMillis() - startTime
                }
            } catch (_: Exception) {
            }
        }

        context.isAlive = alive
        context.responseTimeMs = latency
    }
}
