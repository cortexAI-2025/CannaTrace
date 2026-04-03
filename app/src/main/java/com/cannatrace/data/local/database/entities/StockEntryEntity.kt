package com.cannatrace.data.local.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "stock_entries",
    indices = [
        Index(value = ["batchId"]),
        Index(value = ["timestamp"]),
        Index(value = ["movementType"])
    ]
)
data class StockEntryEntity(
    @PrimaryKey val id: String,
    val batchId: String,
    val movementType: String,
    val quantity: Double,
    val unit: String,
    val reason: String,
    val operatorId: String,
    val timestamp: Long,
    val notes: String = ""
)

@Entity(
    tableName = "divergence_reports",
    indices = [Index(value = ["batchId"])]
)
data class DivergenceReportEntity(
    @PrimaryKey val id: String,
    val batchId: String,
    val computedStock: Double,
    val declaredStock: Double,
    val divergence: Double,
    val unit: String,
    val reportedAt: Long,
    val operatorId: String,
    val resolved: Boolean = false,
    val resolutionNotes: String = ""
)
