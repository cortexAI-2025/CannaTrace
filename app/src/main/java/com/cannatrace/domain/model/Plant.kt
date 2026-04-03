package com.cannatrace.domain.model

enum class PlantStatus {
    ACTIVE, HARVESTED, DESTROYED, TRANSFERRED
}

data class Plant(
    val id: String,
    val qrCode: String,
    val rfidTag: String? = null,
    val batchId: String,
    val locationId: String,
    val strain: String,
    val plantedAt: Long,
    val status: PlantStatus = PlantStatus.ACTIVE,
    val notes: String = ""
)
