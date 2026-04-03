package com.cannatrace.presentation.batch

import com.cannatrace.domain.model.Batch
import com.cannatrace.domain.model.BatchStatus
import com.cannatrace.domain.model.CorrectionNote

data class BatchListState(
    val batches: List<Batch> = emptyList(),
    val filteredBatches: List<Batch> = emptyList(),
    val selectedStatus: BatchStatus? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

data class BatchDetailState(
    val batch: Batch? = null,
    val correctionNotes: List<CorrectionNote> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val showTransitionDialog: Boolean = false,
    val showCorrectionNoteDialog: Boolean = false
)

data class CreateBatchState(
    val batchNumber: String = "",
    val strainName: String = "",
    val weight: String = "",
    val thcContent: String = "",
    val cbdContent: String = "",
    val locationId: String = "",
    val notes: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)
