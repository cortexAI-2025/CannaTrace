package com.cannatrace.domain.model

enum class TransformationType(val displayName: String) {
    EXTRACTION("Extraction"),
    HUILE("Huile"),
    GELULES("Gélules"),
    TEINTURE("Teinture")
}

data class Transformation(
    val id: String,
    val batchId: String,
    val recipeId: String,
    val inputWeight: Double,
    val outputWeight: Double,
    val transformationType: TransformationType,
    val startedAt: Long,
    val completedAt: Long? = null,
    val yield: Double = if (inputWeight > 0) (outputWeight / inputWeight) * 100 else 0.0,
    val operatorId: String,
    val notes: String = ""
)
