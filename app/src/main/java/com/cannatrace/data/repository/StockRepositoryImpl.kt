package com.cannatrace.data.repository

import com.cannatrace.data.local.database.dao.StockEntryDao
import com.cannatrace.data.local.database.entities.DivergenceReportEntity
import com.cannatrace.data.local.database.entities.StockEntryEntity
import com.cannatrace.data.remote.CannaTraceApi
import com.cannatrace.domain.model.DivergenceReport
import com.cannatrace.domain.model.MovementType
import com.cannatrace.domain.model.StockEntry
import com.cannatrace.domain.repository.StockRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StockRepositoryImpl @Inject constructor(
    private val stockEntryDao: StockEntryDao,
    private val api: CannaTraceApi
) : StockRepository {

    override fun getStockEntriesByBatch(batchId: String): Flow<List<StockEntry>> =
        stockEntryDao.getStockEntriesByBatch(batchId).map { entities -> entities.map { it.toDomain() } }

    override fun getAllStockEntries(): Flow<List<StockEntry>> =
        stockEntryDao.getAllStockEntries().map { entities -> entities.map { it.toDomain() } }

    override suspend fun createStockEntry(entry: StockEntry): Result<StockEntry> = runCatching {
        stockEntryDao.insertStockEntry(entry.toEntity())
        entry
    }

    override suspend fun computeCurrentStock(batchId: String): Double =
        stockEntryDao.computeCurrentStock(batchId) ?: 0.0

    /**
     * Vérifie si le stock calculé diffère du stock déclaré.
     * Obligation réglementaire : tout écart doit être signalé.
     */
    override suspend fun checkStockDivergence(batchId: String, declaredStock: Double): DivergenceReport? {
        val computed = computeCurrentStock(batchId)
        val divergence = computed - declaredStock
        return if (kotlin.math.abs(divergence) > 0.001) {
            DivergenceReport(
                id = java.util.UUID.randomUUID().toString(),
                batchId = batchId,
                computedStock = computed,
                declaredStock = declaredStock,
                divergence = divergence,
                unit = "g",
                reportedAt = System.currentTimeMillis(),
                operatorId = "SYSTEM"
            )
        } else null
    }

    override suspend fun createDivergenceReport(report: DivergenceReport): Result<DivergenceReport> = runCatching {
        stockEntryDao.insertDivergenceReport(report.toEntity())
        report
    }

    override fun getDivergenceReports(batchId: String): Flow<List<DivergenceReport>> =
        stockEntryDao.getDivergenceReportsByBatch(batchId).map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun resolveDivergenceReport(reportId: String, notes: String): Result<Unit> = runCatching {
        stockEntryDao.resolveDivergenceReport(reportId, notes)
    }

    override suspend fun getLowStockBatches(threshold: Double): List<String> =
        stockEntryDao.getLowStockBatchIds(threshold)

    // ─── Mappers internes ────────────────────────────────────────────────

    private fun StockEntryEntity.toDomain() = StockEntry(
        id = id,
        batchId = batchId,
        movementType = MovementType.valueOf(movementType),
        quantity = quantity,
        unit = unit,
        reason = reason,
        operatorId = operatorId,
        timestamp = timestamp,
        notes = notes
    )

    private fun StockEntry.toEntity() = StockEntryEntity(
        id = id,
        batchId = batchId,
        movementType = movementType.name,
        quantity = quantity,
        unit = unit,
        reason = reason,
        operatorId = operatorId,
        timestamp = timestamp,
        notes = notes
    )

    private fun DivergenceReportEntity.toDomain() = DivergenceReport(
        id = id,
        batchId = batchId,
        computedStock = computedStock,
        declaredStock = declaredStock,
        divergence = divergence,
        unit = unit,
        reportedAt = reportedAt,
        operatorId = operatorId,
        resolved = resolved,
        resolutionNotes = resolutionNotes
    )

    private fun DivergenceReport.toEntity() = DivergenceReportEntity(
        id = id,
        batchId = batchId,
        computedStock = computedStock,
        declaredStock = declaredStock,
        divergence = divergence,
        unit = unit,
        reportedAt = reportedAt,
        operatorId = operatorId,
        resolved = resolved,
        resolutionNotes = resolutionNotes
    )
}
