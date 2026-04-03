package com.cannatrace.data.local.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "plants",
    indices = [
        Index(value = ["qrCode"], unique = true),
        Index(value = ["rfidTag"]),
        Index(value = ["batchId"]),
        Index(value = ["locationId"])
    ]
)
data class PlantEntity(
    @PrimaryKey val id: String,
    val qrCode: String,
    val rfidTag: String? = null,
    val batchId: String,
    val locationId: String,
    val strain: String,
    val plantedAt: Long,
    val status: String,
    val notes: String = ""
)
