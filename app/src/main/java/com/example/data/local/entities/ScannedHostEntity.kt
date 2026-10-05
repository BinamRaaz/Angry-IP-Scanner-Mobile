package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "scanned_hosts",
    foreignKeys = [
        ForeignKey(
            entity = ScanSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["sessionId"])]
)
data class ScannedHostEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val sessionId: Long,
    val ip: String,
    val hostname: String? = null,
    val status: String,
    val responseTimeMs: Long? = null,
    val ttl: Int? = null,
    val macAddress: String? = null,
    val vendor: String? = null,
    val openPorts: String = "",
    val comment: String? = null,
    val scannedAt: Long = System.currentTimeMillis()
)
