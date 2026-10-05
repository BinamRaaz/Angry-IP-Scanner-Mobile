package com.example.data.local.models

import androidx.room.Embedded
import androidx.room.Relation
import com.example.data.local.entities.ScanSessionEntity
import com.example.data.local.entities.ScannedHostEntity

data class ScanSessionWithHosts(
    @Embedded
    val session: ScanSessionEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "sessionId"
    )
    val hosts: List<ScannedHostEntity>
)
