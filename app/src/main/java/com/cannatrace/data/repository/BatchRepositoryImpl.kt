package com.cannatrace.data.repository

import com.cannatrace.data.local.database.dao.BatchDao
import com.cannatrace.data.local.mapper.toDomain
import com.cannatrace.data.local.mapper.toEntity
import com.cannatrace.data.remote.CannaTraceApi
import com.cannatrace.domain.model.Batch
import com.cannatrace.domain.model.BatchStatus
import com.cannatrace.domain.model.CorrectionNote
import com.cannatrace.domain.repository.BatchRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BatchRepositoryImpl @Inject constructor(
    private val batchDao: BatchDao,
    private val api: CannaTraceApi
) : BatchRepository {

    override fun getAllBatches(): Flow<List<Batch>> =
        batchDao.getAllBatches().map { entities -> entities.map { it.toDomain() } }

    override fun getBatchesByStatus(status: BatchStatus): Flow<List<Batch>> =
        batchDao.getBatchesByStatus(status.name).map { entities -> entities.map { it.toDomain() } }

    override suspend fun getBatchById(id: String): Batch? =
        batchDao.getBatchById(id)?.toDomain()

    override suspend fun getBatchByNumber(batchNumber: String): Batch? =
        batchDao.getBatchByNumber(batchNumber)?.toDomain()

    override suspend fun createBatch(batch: Batch): Result<Batch> = runCatching {
        batchDao.insertBatch(batch.toEntity())
        batch
    }

    override suspend fun updateBatch(batch: Batch): Result<Batch> = runCatching {
        val entity = batch.copy(updatedAt = System.currentTimeMillis()).toEntity()
        batchDao.updateBatch(entity)
        batch
    }

    override suspend fun closeBatch(batchId: String): Result<Unit> = runCatching {
        batchDao.closeBatch(batchId, System.currentTimeMillis())
    }

    override suspend fun addCorrectionNote(note: CorrectionNote): Result<CorrectionNote> = runCatching {
        batchDao.insertCorrectionNote(note.toEntity())
        note
    }

    override fun getCorrectionNotes(batchId: String): Flow<List<CorrectionNote>> =
        batchDao.getCorrectionNotesByBatch(batchId).map { entities -> entities.map { it.toDomain() } }

    override suspend fun getExpiringBatches(thresholdDays: Int): List<Batch> {
        // Dans une implémentation réelle, on filtrerait par date d'expiration des conditionnements
        // Pour l'instant, on retourne les lots en STOCKAGE depuis plus de X jours
        val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(thresholdDays.toLong())
        return batchDao.getAllBatches()
            .map { entities ->
                entities
                    .filter { it.status == BatchStatus.STOCKAGE.name && it.createdAt < cutoff }
                    .map { it.toDomain() }
            }
            .let { flow ->
                var result = emptyList<Batch>()
                flow.collect { result = it }
                result
            }
    }

    override suspend fun syncBatches(): Result<Unit> = runCatching {
        val response = api.getBatches()
        if (response.isSuccessful) {
            response.body()?.content?.forEach { entity ->
                batchDao.insertBatch(entity)
            }
        }
    }
}
