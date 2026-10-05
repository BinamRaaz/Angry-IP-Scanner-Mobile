package com.example.core.fetchers

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileReader

class MacFetcher : HostFetcher {
    override val id: FetcherId = FetcherId.MAC_VENDOR
    override val displayName: String = "MAC / Vendor"

    override suspend fun fetch(ip: String, context: FetchContext): Unit = withContext(Dispatchers.IO) {
        if (!context.isAlive) return@withContext

        try {
            val arpTable = File("/proc/net/arp")
            if (arpTable.exists() && arpTable.canRead()) {
                BufferedReader(FileReader(arpTable)).use { reader ->
                    // Skip header line
                    reader.readLine()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        val tokens = line?.trim()?.split(Regex("\\s+")) ?: continue
                        if (tokens.size >= 4 && tokens[0] == ip) {
                            val mac = tokens[3]
                            if (mac != "00:00:00:00:00:00") {
                                context.macAddress = mac.uppercase()
                                context.vendor = VendorLookup.lookup(mac)
                            }
                            break
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Android 10+ restricts /proc/net/arp access for non-system apps
        }
    }
}
