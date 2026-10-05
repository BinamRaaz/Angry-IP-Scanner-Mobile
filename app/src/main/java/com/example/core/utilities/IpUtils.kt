package com.example.core.utilities

/**
 * Utility functions for validating, converting, and calculating IPv4 addresses, ranges, and CIDR blocks.
 */
object IpUtils {

    private val IPV4_REGEX = Regex(
        "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
    )

    private val CIDR_REGEX = Regex(
        "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)/(3[0-2]|[12]?[0-9])$"
    )

    fun isValidIpv4(ip: String): Boolean {
        return IPV4_REGEX.matches(ip.trim())
    }

    fun isValidCidr(cidr: String): Boolean {
        return CIDR_REGEX.matches(cidr.trim())
    }

    /**
     * Converts a dotted-decimal IPv4 address into a 32-bit unsigned long.
     */
    fun ipv4ToLong(ip: String): Long {
        val parts = ip.trim().split(".")
        require(parts.size == 4) { "Invalid IPv4 format: $ip" }
        var result = 0L
        for (part in parts) {
            result = (result shl 8) or (part.toInt() and 0xFF).toLong()
        }
        return result
    }

    /**
     * Converts a 32-bit unsigned long into a dotted-decimal IPv4 address string.
     */
    fun longToIpv4(value: Long): String {
        return String.format(
            "%d.%d.%d.%d",
            (value shr 24) and 0xFF,
            (value shr 16) and 0xFF,
            (value shr 8) and 0xFF,
            value and 0xFF
        )
    }

    /**
     * Parses a CIDR string (e.g., "192.168.1.0/24") into a pair of usable host boundaries:
     * First: First host address (e.g. 192.168.1.1)
     * Second: Last host address (e.g. 192.168.1.254)
     * For /31 or /32, returns the exact subnet boundaries.
     */
    fun parseCidr(cidr: String): Pair<String, String>? {
        if (!isValidCidr(cidr)) return null
        val parts = cidr.trim().split("/")
        val baseIp = parts[0]
        val prefixLength = parts[1].toInt()

        val baseLong = ipv4ToLong(baseIp)
        val mask = if (prefixLength == 0) 0L else (0xFFFFFFFFL shl (32 - prefixLength)) and 0xFFFFFFFFL
        val networkLong = baseLong and mask
        val broadcastLong = networkLong or (mask.inv() and 0xFFFFFFFFL)

        val firstHostLong = if (prefixLength >= 31) networkLong else networkLong + 1
        val lastHostLong = if (prefixLength >= 31) broadcastLong else broadcastLong - 1

        return Pair(longToIpv4(firstHostLong), longToIpv4(lastHostLong))
    }

    /**
     * Generates all IP addresses between [startIp] and [endIp] inclusive.
     * Enforces a safe upper bound on total addresses to safeguard mobile memory.
     */
    fun generateRange(startIp: String, endIp: String, maxLimit: Long = 65536L): List<String> {
        if (!isValidIpv4(startIp) || !isValidIpv4(endIp)) return emptyList()
        val start = ipv4ToLong(startIp)
        val end = ipv4ToLong(endIp)
        if (start > end) return emptyList()

        val list = mutableListOf<String>()
        var current = start
        val total = (end - start) + 1
        val limit = if (total > maxLimit) start + maxLimit - 1 else end

        while (current <= limit) {
            list.add(longToIpv4(current))
            current++
        }
        return list
    }

    /**
     * Calculates the count of hosts between start and end IP inclusive.
     */
    fun calculateHostCount(startIp: String, endIp: String): Long {
        if (!isValidIpv4(startIp) || !isValidIpv4(endIp)) return 0L
        val start = ipv4ToLong(startIp)
        val end = ipv4ToLong(endIp)
        return if (end >= start) (end - start) + 1 else 0L
    }

    /**
     * Converts CIDR prefix length (e.g. 24) to dotted subnet mask (e.g. "255.255.255.0").
     */
    fun prefixLengthToMask(prefixLength: Int): String {
        val clamped = prefixLength.coerceIn(0, 32)
        val mask = if (clamped == 0) 0L else (0xFFFFFFFFL shl (32 - clamped)) and 0xFFFFFFFFL
        return longToIpv4(mask)
    }
}
