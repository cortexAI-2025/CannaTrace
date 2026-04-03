package com.cannatrace.data.local.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "packagings",
    indices = [
        Index(value = ["batchId"]),
        Index(value = ["labelNumber"], unique = true),
        Index(value = ["expiryDate"])
    ]
)
data class PackagingEntity(
    @PrimaryKey val id: String,
    val batchId: String,
    val labelNumber: String,
    val expiryDate: Long,
    val quantity: Double,
    val unit: String,
    val qrCodeData: String,
    val legalMentions: String,
    val packagedAt: Long,
    val operatorId: String,
    val notes: String = ""
)
