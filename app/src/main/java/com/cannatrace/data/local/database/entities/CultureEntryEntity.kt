package com.cannatrace.data.local.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "culture_entries",
    indices = [
        Index(value = ["batchId"]),
        Index(value = ["recordedAt"]),
        Index(value = ["type"])
    ]
)
data class CultureEntryEntity(
    @PrimaryKey val id: String,
    val batchId: String,
    val type: String,
    val value: Double,
    val unit: String,
    val recordedAt: Long,
    val operatorId: String,
    val notes: String = ""
)
