package com.cannatrace.domain.usecase

import com.cannatrace.domain.model.AuditLog
import com.cannatrace.domain.model.Batch
import com.cannatrace.domain.repository.AuditLogRepository
import com.cannatrace.domain.repository.BatchRepository
import java.util.UUID
import javax.inject.Inject

class CreateBatchUseCase @Inject constructor(
    private val batchRepository: BatchRepository,
    private val auditLogRepository: AuditLogRepository,
    private val generateAuditHashUseCase: GenerateAuditHashUseCase
) {

    suspend operator fun invoke(batch: Batch, operatorId: String): Result<Batch> {
        // Validation
        if (batch.batchNumber.isBlank()) {
            return Result.failure(IllegalArgumentException("Le numéro de lot est obligatoire"))
        }
        if (batch.strainName.isBlank()) {
            return Result.failure(IllegalArgumentException("Le nom de la souche est obligatoire"))
        }
        if (batch.ownerId.isBlank()) {
            return Result.failure(IllegalArgumentException("Le propriétaire est obligatoire"))
        }
        if (batch.locationId.isBlank()) {
            return Result.failure(IllegalArgumentException("La localisation est obligatoire"))
        }

        // Check for duplicate batch number
        val existing = batchRepository.getBatchByNumber(batch.batchNumber)
        if (existing != null) {
            return Result.failure(IllegalStateException("Un lot avec ce numéro existe déjà: ${batch.batchNumber}"))
        }

        // Create the batch
        val batchResult = batchRepository.createBatch(batch)
        if (batchResult.isFailure) return batchResult

        val createdBatch = batchResult.getOrThrow()

        // Generate audit log with hash chain
        val latestLog = auditLogRepository.getLatestAuditLog()
        val previousHash = latestLog?.hash ?: "GENESIS"
        val timestamp = System.currentTimeMillis()
        val action = "CREATE_BATCH"
        val data = "batchNumber=${createdBatch.batchNumber};status=${createdBatch.status};strain=${createdBatch.strainName}"

        val hash = generateAuditHashUseCase.computeHash(
            previousHash = previousHash,
            timestamp = timestamp,
            action = action,
            entityId = createdBatch.id,
            data = data
        )

        val auditLog = AuditLog(
            id = UUID.randomUUID().toString(),
            action = action,
            entityType = "BATCH",
            entityId = createdBatch.id,
            userId = operatorId,
            timestamp = timestamp,
            hash = hash,
            previousHash = previousHash,
            data = data
        )

        auditLogRepository.createAuditLog(auditLog)

        return Result.success(createdBatch)
    }
}
