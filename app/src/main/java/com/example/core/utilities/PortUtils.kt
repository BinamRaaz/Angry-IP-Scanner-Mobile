package com.example.core.utilities

/**
 * Utility functions for parsing, validating, and identifying TCP network ports.
 */
object PortUtils {

    val COMMON_PORTS = listOf(21, 22, 53, 80, 443, 445, 3306, 3389, 5432, 8080, 8443)

    private val WELL_KNOWN_SERVICES = mapOf(
        21 to "FTP",
        22 to "SSH",
        23 to "Telnet",
        25 to "SMTP",
        53 to "DNS",
        80 to "HTTP",
        110 to "POP3",
        139 to "NetBIOS",
        143 to "IMAP",
        443 to "HTTPS",
        445 to "SMB",
        1433 to "MSSQL",
        3306 to "MySQL",
        3389 to "RDP",
        5432 to "PostgreSQL",
        6379 to "Redis",
        8000 to "HTTP-Alt",
        8080 to "HTTP-Proxy",
        8443 to "HTTPS-Alt",
        9000 to "SonarQube/Portainer",
        27017 to "MongoDB"
    )

    /**
     * Resolves a standard service name for a given port number.
     */
    fun getServiceName(port: Int): String {
        return WELL_KNOWN_SERVICES[port] ?: "Port $port"
    }

    /**
     * Parses a string of ports and ranges (e.g. "22, 80, 443, 8080-8085") into a sorted list of unique valid ports.
     */
    fun parsePortString(input: String, maxPorts: Int = 128): List<Int> {
        val result = mutableSetOf<Int>()
        val parts = input.split(",", ";", " ")

        for (part in parts) {
            val trimmed = part.trim()
            if (trimmed.isEmpty()) continue

            if (trimmed.contains("-")) {
                val rangeParts = trimmed.split("-")
                if (rangeParts.size == 2) {
                    val start = rangeParts[0].trim().toIntOrNull()
                    val end = rangeParts[1].trim().toIntOrNull()
                    if (start != null && end != null && start in 1..65535 && end in 1..65535 && start <= end) {
                        for (p in start..end) {
                            result.add(p)
                            if (result.size >= maxPorts) break
                        }
                    }
                }
            } else {
                val port = trimmed.toIntOrNull()
                if (port != null && port in 1..65535) {
                    result.add(port)
                }
            }

            if (result.size >= maxPorts) break
        }

        return result.sorted()
    }
}
