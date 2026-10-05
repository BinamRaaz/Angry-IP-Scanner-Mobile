package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.core.speedtest.SpeedTestResult

@Entity(tableName = "speed_test_history")
data class SpeedTestEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val timestamp: Long = System.currentTimeMillis(),
    val pingMs: Double,
    val jitterMs: Double,
    val downloadMbps: Double,
    val uploadMbps: Double,
    val serverName: String,
    val clientIp: String? = null
)

fun SpeedTestEntity.toDomainModel(): SpeedTestResult = SpeedTestResult(
    id = id,
    timestamp = timestamp,
    pingMs = pingMs,
    jitterMs = jitterMs,
    downloadMbps = downloadMbps,
    uploadMbps = uploadMbps,
    serverName = serverName,
    clientIp = clientIp
)

fun SpeedTestResult.toEntity(): SpeedTestEntity = SpeedTestEntity(
    id = id,
    timestamp = timestamp,
    pingMs = pingMs,
    jitterMs = jitterMs,
    downloadMbps = downloadMbps,
    uploadMbps = uploadMbps,
    serverName = serverName,
    clientIp = clientIp
)
