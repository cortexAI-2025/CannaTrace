package com.cannatrace.data.local.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "trace_metadata", foreignKeys = [ForeignKey(
    entity = BatchEntity::class, parentColumns = ["id"], childColumns = ["batchId"],
    onDelete = ForeignKey.RESTRICT
)])
data class TraceMetadata(
    @PrimaryKey val batchId: String,
    val category: String,
    val description: String,
    val origin: String,
    val supplier: String,
    val unit: String,
    val revision: Long = 1
)

@Entity(tableName = "trace_events", foreignKeys = [ForeignKey(
    entity = BatchEntity::class, parentColumns = ["id"], childColumns = ["batchId"],
    onDelete = ForeignKey.RESTRICT
)], indices = [androidx.room.Index(value = ["batchId"])])
data class TraceEvent(
    @PrimaryKey val operationId: String,
    val batchId: String,
    val action: String,
    val timestamp: Long,
    val actor: String,
    val beforeValue: String,
    val afterValue: String,
    val reason: String,
    val fingerprint: String,
    val previousHash: String,
    val hash: String
)
