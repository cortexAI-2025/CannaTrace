package com.cannatrace.data.local.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transformations",
    indices = [
        Index(value = ["batchId"]),
        Index(value = ["startedAt"])
    ]
)
data class TransformationEntity(
    @PrimaryKey val id: String,
    val batchId: String,
    val recipeId: String,
    val inputWeight: Double,
    val outputWeight: Double,
    val transformationType: String,
    val startedAt: Long,
    val completedAt: Long? = null,
    val yieldPercentage: Double,
    val operatorId: String,
    val notes: String = ""
)
