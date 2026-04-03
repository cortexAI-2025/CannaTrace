package com.cannatrace.domain.repository

import com.cannatrace.domain.model.Batch
import com.cannatrace.domain.model.BatchStatus
import com.cannatrace.domain.model.CorrectionNote
import kotlinx.coroutines.flow.Flow

interface BatchRepository {
    fun getAllBatches(): Flow<List<Batch>>
    fun getBatchesByStatus(status: BatchStatus): Flow<List<Batch>>
    suspend fun getBatchById(id: String): Batch?
    suspend fun getBatchByNumber(batchNumber: String): Batch?
    suspend fun createBatch(batch: Batch): Result<Batch>
    suspend fun updateBatch(batch: Batch): Result<Batch>
    suspend fun closeBatch(batchId: String): Result<Unit>
    suspend fun addCorrectionNote(note: CorrectionNote): Result<CorrectionNote>
    fun getCorrectionNotes(batchId: String): Flow<List<CorrectionNote>>
    suspend fun getExpiringBatches(thresholdDays: Int): List<Batch>
    suspend fun syncBatches(): Result<Unit>
}
