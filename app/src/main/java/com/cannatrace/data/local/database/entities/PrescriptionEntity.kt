package com.cannatrace.data.local.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "prescriptions",
    indices = [
        Index(value = ["anonymizedPatientId"]),
        Index(value = ["prescriberId"]),
        Index(value = ["batchId"]),
        Index(value = ["lastDispensationDate"])
    ]
)
data class PrescriptionEntity(
    @PrimaryKey val id: String,
    val anonymizedPatientId: String,
    val prescriberId: String,
    val batchId: String,
    val dosage: String,
    val frequency: String,
    val startDate: Long,
    val endDate: Long,
    val adverseEffects: String = "",
    val isAnonymized: Boolean = false,
    val lastDispensationDate: Long? = null,
    val createdAt: Long
)
