package com.example.data.repository

import com.example.core.models.DiscoveredHost
import com.example.core.models.HostStatus
import com.example.data.local.dao.ScanHistoryDao
import com.example.data.local.entities.ScanSessionEntity
import com.example.data.local.entities.ScannedHostEntity
import com.example.data.local.models.ScanSessionWithHosts
import kotlinx.coroutines.flow.Flow

interface ScanHistoryRepository {
    fun getAllSessions(): Flow<List<ScanSessionEntity>>
    suspend fun getSessionWithHosts(sessionId: Long): ScanSessionWithHosts?
    suspend fun saveScan(
        startIp: String,
        endIp: String,
        cidr: String?,
        durationMs: Long,
        hosts: List<DiscoveredHost>
    ): Long
    suspend fun deleteSession(sessionId: Long)
    suspend fun clearHistory()
}

class DefaultScanHistoryRepository(
    private val dao: ScanHistoryDao
) : ScanHistoryRepository {

    override fun getAllSessions(): Flow<List<ScanSessionEntity>> = dao.getAllSessions()

    override suspend fun getSessionWithHosts(sessionId: Long): ScanSessionWithHosts? =
        dao.getSessionWithHosts(sessionId)

    override suspend fun saveScan(
        startIp: String,
        endIp: String,
        cidr: String?,
        durationMs: Long,
        hosts: List<DiscoveredHost>
    ): Long {
        if (hosts.isEmpty()) return -1L
        val aliveCount = hosts.count { it.status == HostStatus.ALIVE }
        val session = ScanSessionEntity(
            startIp = startIp,
            endIp = endIp,
            cidr = cidr,
            totalScanned = hosts.size,
            aliveCount = aliveCount,
            durationMs = durationMs
        )
        val sessionId = dao.insertSession(session)

        val hostEntities = hosts.map { host ->
            ScannedHostEntity(
                sessionId = sessionId,
                ip = host.ip,
                hostname = host.hostname,
                status = host.status.name,
                responseTimeMs = host.responseTimeMs,
                ttl = host.ttl,
                macAddress = host.macAddress,
                vendor = host.vendor,
                openPorts = host.openPorts.joinToString(","),
                comment = host.comment,
                scannedAt = host.scannedAt
            )
        }
        dao.insertHosts(hostEntities)
        return sessionId
    }

    override suspend fun deleteSession(sessionId: Long) = dao.deleteSession(sessionId)

    override suspend fun clearHistory() = dao.clearAllSessions()
}

fun ScannedHostEntity.toDiscoveredHost(): DiscoveredHost {
    val statusEnum = try {
        HostStatus.valueOf(status)
    } catch (_: Exception) {
        HostStatus.UNKNOWN
    }

    val portsList = if (openPorts.isNotBlank()) {
        openPorts.split(",").mapNotNull { it.trim().toIntOrNull() }
    } else {
        emptyList()
    }

    return DiscoveredHost(
        ip = ip,
        hostname = hostname,
        status = statusEnum,
        responseTimeMs = responseTimeMs,
        ttl = ttl,
        macAddress = macAddress,
        vendor = vendor,
        openPorts = portsList,
        comment = comment,
        scannedAt = scannedAt
    )
}
