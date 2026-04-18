package com.cannatrace.domain.repository

import com.cannatrace.domain.model.DivergenceReport
import com.cannatrace.domain.model.StockEntry
import kotlinx.coroutines.flow.Flow

interface StockRepository {
    fun getStockEntriesByBatch(batchId: String): Flow<List<StockEntry>>
    fun getAllStockEntries(): Flow<List<StockEntry>>
    suspend fun createStockEntry(entry: StockEntry): Result<StockEntry>
    suspend fun computeCurrentStock(batchId: String): Double
    suspend fun checkStockDivergence(batchId: String, declaredStock: Double): DivergenceReport?
    suspend fun createDivergenceReport(report: DivergenceReport): Result<DivergenceReport>
    fun getDivergenceReports(batchId: String): Flow<List<DivergenceReport>>
    suspend fun resolveDivergenceReport(reportId: String, notes: String): Result<Unit>
    suspend fun getLowStockBatches(threshold: Double): List<String>
    suspend fun syncStockEntries(): Result<Unit>
}
