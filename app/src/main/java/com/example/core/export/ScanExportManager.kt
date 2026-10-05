package com.example.core.export

import com.example.core.models.DiscoveredHost
import com.example.core.models.HostStatus
import com.example.core.utilities.PortUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ExportFormat(
    val extension: String,
    val mimeType: String,
    val displayName: String
) {
    CSV("csv", "text/csv", "CSV (Spreadsheet)"),
    JSON("json", "application/json", "JSON (Data)"),
    TXT("txt", "text/plain", "Text (Report)")
}

data class ExportConfig(
    val format: ExportFormat = ExportFormat.CSV,
    val aliveOnly: Boolean = false,
    val includeHostname: Boolean = true,
    val includePing: Boolean = true,
    val includeTtl: Boolean = true,
    val includeMacVendor: Boolean = true,
    val includePorts: Boolean = true,
    val includeComments: Boolean = true
)

object ScanExportManager {

    fun generateDefaultFileName(format: ExportFormat): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return "ip_scan_$timestamp.${format.extension}"
    }

    fun generateContent(hosts: List<DiscoveredHost>, config: ExportConfig): String {
        val targetHosts = if (config.aliveOnly) {
            hosts.filter { it.status == HostStatus.ALIVE }
        } else {
            hosts
        }

        return when (config.format) {
            ExportFormat.CSV -> generateCsv(targetHosts, config)
            ExportFormat.JSON -> generateJson(targetHosts, config)
            ExportFormat.TXT -> generateTxt(targetHosts, config)
        }
    }

    private fun generateCsv(hosts: List<DiscoveredHost>, config: ExportConfig): String {
        val sb = StringBuilder()
        val headers = mutableListOf<String>()
        headers.add("IP Address")
        headers.add("Status")
        if (config.includePing) headers.add("Ping (ms)")
        if (config.includeHostname) headers.add("Hostname")
        if (config.includeTtl) headers.add("TTL")
        if (config.includeMacVendor) {
            headers.add("MAC Address")
            headers.add("Vendor")
        }
        if (config.includePorts) headers.add("Open Ports")
        if (config.includeComments) headers.add("Notes")

        sb.appendLine(headers.joinToString(",") { escapeCsv(it) })

        for (host in hosts) {
            val row = mutableListOf<String>()
            row.add(host.ip)
            row.add(host.status.name)
            if (config.includePing) row.add(host.responseTimeMs?.toString() ?: "")
            if (config.includeHostname) row.add(host.hostname ?: "")
            if (config.includeTtl) row.add(host.ttl?.toString() ?: "")
            if (config.includeMacVendor) {
                row.add(host.macAddress ?: "")
                row.add(host.vendor ?: "")
            }
            if (config.includePorts) {
                val portsStr = host.openPorts.joinToString(";") { "$it(${PortUtils.getServiceName(it)})" }
                row.add(portsStr)
            }
            if (config.includeComments) row.add(host.comment ?: "")

            sb.appendLine(row.joinToString(",") { escapeCsv(it) })
        }

        return sb.toString()
    }

    private fun generateJson(hosts: List<DiscoveredHost>, config: ExportConfig): String {
        val items = hosts.map { host ->
            val fields = mutableListOf<String>()
            fields.add("\"ip\": \"${escapeJson(host.ip)}\"")
            fields.add("\"status\": \"${host.status.name}\"")
            if (config.includePing) {
                fields.add("\"pingMs\": ${host.responseTimeMs ?: "null"}")
            }
            if (config.includeHostname) {
                fields.add("\"hostname\": ${host.hostname?.let { "\"${escapeJson(it)}\"" } ?: "null"}")
            }
            if (config.includeTtl) {
                fields.add("\"ttl\": ${host.ttl ?: "null"}")
            }
            if (config.includeMacVendor) {
                fields.add("\"macAddress\": ${host.macAddress?.let { "\"${escapeJson(it)}\"" } ?: "null"}")
                fields.add("\"vendor\": ${host.vendor?.let { "\"${escapeJson(it)}\"" } ?: "null"}")
            }
            if (config.includePorts) {
                fields.add("\"openPorts\": [${host.openPorts.joinToString(", ")}]")
            }
            if (config.includeComments) {
                fields.add("\"notes\": ${host.comment?.let { "\"${escapeJson(it)}\"" } ?: "null"}")
            }
            fields.add("\"scannedAt\": ${host.scannedAt}")
            "  {\n" + fields.joinToString(",\n") { "    $it" } + "\n  }"
        }
        return "[\n" + items.joinToString(",\n") + "\n]"
    }

    private fun generateTxt(hosts: List<DiscoveredHost>, config: ExportConfig): String {
        val sb = StringBuilder()
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        sb.appendLine("==================================================")
        sb.appendLine("Angry IP Scanner Mobile - Scan Report")
        sb.appendLine("Generated: $timestamp")
        sb.appendLine("Total Hosts: ${hosts.size} (${hosts.count { it.status == HostStatus.ALIVE }} Alive)")
        sb.appendLine("==================================================")
        sb.appendLine()

        for (host in hosts) {
            sb.appendLine("Host: ${host.ip} [${host.status}]")
            if (config.includePing && host.responseTimeMs != null) {
                sb.appendLine("  Ping: ${host.responseTimeMs} ms")
            }
            if (config.includeHostname && !host.hostname.isNullOrBlank()) {
                sb.appendLine("  Hostname: ${host.hostname}")
            }
            if (config.includeTtl && host.ttl != null) {
                sb.appendLine("  TTL: ${host.ttl}")
            }
            if (config.includeMacVendor) {
                if (!host.macAddress.isNullOrBlank()) sb.appendLine("  MAC: ${host.macAddress}")
                if (!host.vendor.isNullOrBlank()) sb.appendLine("  Vendor: ${host.vendor}")
            }
            if (config.includePorts && host.openPorts.isNotEmpty()) {
                val portsStr = host.openPorts.joinToString(", ") { "$it (${PortUtils.getServiceName(it)})" }
                sb.appendLine("  Open Ports: $portsStr")
            }
            if (config.includeComments && !host.comment.isNullOrBlank()) {
                sb.appendLine("  Notes: ${host.comment}")
            }
            sb.appendLine()
        }

        return sb.toString()
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }

    private fun escapeJson(value: String): String {
        return value.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
}
