package com.cannatrace.domain.model

enum class LabAnalysisStatus {
    PENDING, IN_PROGRESS, PASSED, FAILED, NOT_REQUIRED
}

data class Harvest(
    val id: String,
    val batchId: String,
    val weight: Double,
    val samplingWeight: Double,
    val containerId: String,
    val harvestedAt: Long,
    val operatorId: String,
    val labAnalysisStatus: LabAnalysisStatus = LabAnalysisStatus.PENDING,
    val labAnalysisDate: Long? = null,
    val labAnalysisReport: String? = null,
    val notes: String = ""
)
