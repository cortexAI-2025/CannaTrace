package com.cannatrace.domain.model

enum class BatchStatus(val displayName: String, val order: Int) {
    GRAINE("Graine", 0),
    GERMINATION("Germination", 1),
    CROISSANCE("Croissance", 2),
    RECOLTE("Récolte", 3),
    SECHAGE("Séchage", 4),
    TRANSFORMATION("Transformation", 5),
    CONDITIONNEMENT("Conditionnement", 6),
    CONTROLE_QUALITE("Contrôle Qualité", 7),
    STOCKAGE("Stockage", 8),
    DISPENSATION("Dispensation", 9),
    DETRUIT("Détruit", 10),
    CLOTURE("Clôturé", 11)
}

data class Batch(
    val id: String,
    val batchNumber: String,
    val strainName: String,
    val status: BatchStatus,
    val weight: Double,
    val thcContent: Double,
    val cbdContent: Double,
    val createdAt: Long,
    val updatedAt: Long = System.currentTimeMillis(),
    val ownerId: String,
    val locationId: String,
    val parentBatchId: String? = null,
    val isClosed: Boolean = false,
    val hashChain: String = "",
    val notes: String = ""
)

data class CorrectionNote(
    val id: String,
    val batchId: String,
    val operatorId: String,
    val justification: String,
    val createdAt: Long = System.currentTimeMillis(),
    val correctionType: String
)
