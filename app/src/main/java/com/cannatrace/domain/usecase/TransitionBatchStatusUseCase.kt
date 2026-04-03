package com.cannatrace.domain.usecase

import com.cannatrace.domain.model.AuditLog
import com.cannatrace.domain.model.Batch
import com.cannatrace.domain.model.BatchStatus
import com.cannatrace.domain.repository.AuditLogRepository
import com.cannatrace.domain.repository.BatchRepository
import java.util.UUID
import javax.inject.Inject

class TransitionBatchStatusUseCase @Inject constructor(
    private val batchRepository: BatchRepository,
    private val auditLogRepository: AuditLogRepository,
    private val generateAuditHashUseCase: GenerateAuditHashUseCase
) {

    companion object {
        // Valid state transitions
        val VALID_TRANSITIONS: Map<BatchStatus, Set<BatchStatus>> = mapOf(
            BatchStatus.GRAINE to setOf(BatchStatus.GERMINATION, BatchStatus.DETRUIT),
            BatchStatus.GERMINATION to setOf(BatchStatus.CROISSANCE, BatchStatus.DETRUIT),
            BatchStatus.CROISSANCE to setOf(BatchStatus.RECOLTE, BatchStatus.DETRUIT),
            BatchStatus.RECOLTE to setOf(BatchStatus.SECHAGE, BatchStatus.DETRUIT),
            BatchStatus.SECHAGE to setOf(BatchStatus.TRANSFORMATION, BatchStatus.DETRUIT),
            BatchStatus.TRANSFORMATION to setOf(BatchStatus.CONDITIONNEMENT, BatchStatus.DETRUIT),
            BatchStatus.CONDITIONNEMENT to setOf(BatchStatus.CONTROLE_QUALITE, BatchStatus.DETRUIT),
            BatchStatus.CONTROLE_QUALITE to setOf(BatchStatus.STOCKAGE, BatchStatus.DETRUIT),
            BatchStatus.STOCKAGE to setOf(BatchStatus.DISPENSATION, BatchStatus.DETRUIT),
            BatchStatus.DISPENSATION to setOf(BatchStatus.CLOTURE, BatchStatus.DETRUIT),
            BatchStatus.DETRUIT to setOf(BatchStatus.CLOTURE)
        )
    }

    suspend operator fun invoke(
        batchId: String,
        newStatus: BatchStatus,
        operatorId: String,
        reason: String = ""
    ): Result<Batch> {
        val batch = batchRepository.getBatchById(batchId)
            ?: return Result.failure(IllegalArgumentException("Lot introuvable: $batchId"))

        // Rule: closed batches cannot be modified
        if (batch.isClosed) {
            return Result.failure(
                IllegalStateException("Le lot ${batch.batchNumber} est clôturé et ne peut plus être modifié. Ajoutez une note de correction si nécessaire.")
            )
        }

        // Rule: DETRUIT requires a mandatory reason
        if (newStatus == BatchStatus.DETRUIT && reason.isBlank()) {
            return Result.failure(
                IllegalArgumentException("Une raison est obligatoire pour détruire un lot.")
            )
        }

        // Validate state machine transition
        val allowedTransitions = VALID_TRANSITIONS[batch.status]
        if (allowedTransitions == null || !allowedTransitions.contains(newStatus)) {
            return Result.failure(
                IllegalStateException(
                    "Transition invalide: ${batch.status.displayName} → ${newStatus.displayName}. " +
                    "Transitions autorisées: ${allowedTransitions?.joinToString { it.displayName } ?: "aucune"}"
                )
            )
        }

        val isClosed = newStatus == BatchStatus.CLOTURE
        val updatedBatch = batch.copy(
            status = newStatus,
            isClosed = isClosed,
            notes = if (reason.isNotBlank()) batch.notes + "\n[$newStatus] $reason" else batch.notes
        )

        val updateResult = batchRepository.updateBatch(updatedBatch)
        if (updateResult.isFailure) return updateResult

        // Generate audit log with hash chain
        val latestLog = auditLogRepository.getLatestAuditLog()
        val previousHash = latestLog?.hash ?: "GENESIS"
        val timestamp = System.currentTimeMillis()
        val action = "TRANSITION_BATCH_STATUS"
        val data = "from=${batch.status};to=${newStatus};reason=${reason.ifBlank { "N/A" }}"

        val hash = generateAuditHashUseCase.computeHash(
            previousHash = previousHash,
            timestamp = timestamp,
            action = action,
            entityId = batchId,
            data = data
        )

        val auditLog = AuditLog(
            id = UUID.randomUUID().toString(),
            action = action,
            entityType = "BATCH",
            entityId = batchId,
            userId = operatorId,
            timestamp = timestamp,
            hash = hash,
            previousHash = previousHash,
            data = data
        )

        auditLogRepository.createAuditLog(auditLog)

        return updateResult
    }
}
