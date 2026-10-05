package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_sessions")
data class ScanSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val timestamp: Long = System.currentTimeMillis(),
    val startIp: String,
    val endIp: String,
    val cidr: String? = null,
    val totalScanned: Int,
    val aliveCount: Int,
    val durationMs: Long
)
