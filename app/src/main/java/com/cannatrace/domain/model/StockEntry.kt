package com.cannatrace.domain.model

enum class MovementType(val displayName: String) {
    ENTREE("Entrée"),
    SORTIE_DISPENSATION("Sortie - Dispensation"),
    SORTIE_DESTRUCTION("Sortie - Destruction"),
    SORTIE_PERTE("Sortie - Perte"),
    RETOUR("Retour")
}

data class StockEntry(
    val id: String,
    val batchId: String,
    val movementType: MovementType,
    val quantity: Double,
    val unit: String,
    val reason: String,
    val operatorId: String,
    val timestamp: Long,
    val notes: String = ""
)

data class DivergenceReport(
    val id: String,
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
