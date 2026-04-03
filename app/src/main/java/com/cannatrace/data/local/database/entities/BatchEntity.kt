package com.cannatrace.data.local.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "batches",
    indices = [
        Index(value = ["batchNumber"], unique = true),
        Index(value = ["ownerId"]),
        Index(value = ["status"]),
        Index(value = ["locationId"])
    ]
)
data class BatchEntity(
    @PrimaryKey val id: String,
    val batchNumber: String,
    val strainName: String,
    val status: String,
    val weight: Double,
    val thcContent: Double,
    val cbdContent: Double,
    val createdAt: Long,
    val updatedAt: Long,
    val ownerId: String,
    val locationId: String,
    val parentBatchId: String? = null,
    val isClosed: Boolean = false,
    val hashChain: String = "",
    val notes: String = ""
)

@Entity(
    tableName = "correction_notes",
    indices = [Index(value = ["batchId"])]
)
data class CorrectionNoteEntity(
    @PrimaryKey val id: String,
    val batchId: String,
    val operatorId: String,
    val justification: String,
    val createdAt: Long,
    val correctionType: String
)
