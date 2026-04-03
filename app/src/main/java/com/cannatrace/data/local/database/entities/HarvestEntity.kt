package com.cannatrace.data.local.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "harvests",
    indices = [
        Index(value = ["batchId"]),
        Index(value = ["harvestedAt"])
    ]
)
data class HarvestEntity(
    @PrimaryKey val id: String,
    val batchId: String,
    val weight: Double,
    val samplingWeight: Double,
    val containerId: String,
    val harvestedAt: Long,
    val operatorId: String,
    val labAnalysisStatus: String,
    val labAnalysisDate: Long? = null,
    val labAnalysisReport: String? = null,
    val notes: String = ""
)
