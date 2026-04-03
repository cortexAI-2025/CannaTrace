package com.cannatrace.domain.model

enum class CultureEntryType(val displayName: String) {
    ARROSAGE("Arrosage"),
    NUTRIMENTS("Nutriments"),
    TEMPERATURE("Température"),
    HUMIDITE("Humidité"),
    LUMIERE("Lumière"),
    OBSERVATION("Observation"),
    TRAITEMENT("Traitement")
}

data class CultureEntry(
    val id: String,
    val batchId: String,
    val type: CultureEntryType,
    val value: Double,
    val unit: String,
    val recordedAt: Long,
    val operatorId: String,
    val notes: String = ""
)
