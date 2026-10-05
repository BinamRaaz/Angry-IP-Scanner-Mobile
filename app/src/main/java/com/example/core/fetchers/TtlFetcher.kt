package com.example.core.fetchers

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

class TtlFetcher : HostFetcher {
    override val id: FetcherId = FetcherId.TTL
    override val displayName: String = "TTL (Time to Live)"

    private val ttlRegex = Regex("ttl=(\\d+)", RegexOption.IGNORE_CASE)

    override suspend fun fetch(ip: String, context: FetchContext): Unit = withContext(Dispatchers.IO) {
        if (!context.isAlive) return@withContext
        // Strict input validation to prevent command injection
        if (!com.example.core.utilities.IpUtils.isValidIpv4(ip)) return@withContext
        try {
            val process = ProcessBuilder("/system/bin/ping", "-c", "1", "-W", "1", ip)
                .redirectErrorStream(true)
                .start()

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            var foundTtl: Int? = null

            while (reader.readLine().also { line = it } != null) {
                line?.let {
                    val match = ttlRegex.find(it)
                    if (match != null) {
                        foundTtl = match.groupValues[1].toIntOrNull()
                    }
                }
                if (foundTtl != null) break
            }

            process.waitFor()
            context.ttl = foundTtl
        } catch (_: Exception) {
            // Unprivileged ping execution or binary absent on this ROM
        }
    }
}
