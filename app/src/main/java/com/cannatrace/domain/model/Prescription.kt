package com.cannatrace.domain.model

data class Prescription(
    val id: String,
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
    val createdAt: Long = System.currentTimeMillis()
)
