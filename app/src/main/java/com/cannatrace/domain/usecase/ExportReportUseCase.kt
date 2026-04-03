package com.cannatrace.domain.usecase

import com.cannatrace.domain.model.AuditLog
import com.cannatrace.domain.model.Batch
import com.cannatrace.domain.model.StockEntry
import com.cannatrace.domain.repository.AuditLogRepository
import com.cannatrace.domain.repository.BatchRepository
import com.cannatrace.domain.repository.StockRepository
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

enum class ReportType { PDF, CSV }
enum class ReportScope { ALL_BATCHES, SINGLE_BATCH, AUDIT_LOG, STOCK_MOVEMENTS }

data class ReportRequest(
    val type: ReportType,
    val scope: ReportScope,
    val batchId: String? = null,
    val startDate: Long? = null,
    val endDate: Long? = null,
    val outputPath: String
)

data class ReportData(
    val batches: List<Batch> = emptyList(),
    val stockEntries: List<StockEntry> = emptyList(),
    val auditLogs: List<AuditLog> = emptyList(),
    val generatedAt: Long = System.currentTimeMillis()
)

class ExportReportUseCase @Inject constructor(
    private val batchRepository: BatchRepository,
    private val stockRepository: StockRepository,
    private val auditLogRepository: AuditLogRepository
) {

    suspend operator fun invoke(request: ReportRequest): Result<ReportData> {
        return try {
            val batches = when (request.scope) {
                ReportScope.SINGLE_BATCH -> {
                    val batch = request.batchId?.let { batchRepository.getBatchById(it) }
                    listOfNotNull(batch)
                }
                ReportScope.ALL_BATCHES, ReportScope.AUDIT_LOG, ReportScope.STOCK_MOVEMENTS -> {
                    batchRepository.getAllBatches().firstOrNull() ?: emptyList()
                }
            }

            val stockEntries = when (request.scope) {
                ReportScope.STOCK_MOVEMENTS, ReportScope.ALL_BATCHES -> {
                    if (request.batchId != null) {
                        stockRepository.getStockEntriesByBatch(request.batchId).firstOrNull() ?: emptyList()
                    } else {
                        stockRepository.getAllStockEntries().firstOrNull() ?: emptyList()
                    }
                }
                else -> emptyList()
            }

            val auditLogs = when (request.scope) {
                ReportScope.AUDIT_LOG -> {
                    auditLogRepository.getAllAuditLogs().firstOrNull() ?: emptyList()
                }
                else -> emptyList()
            }

            Result.success(
                ReportData(
                    batches = batches,
                    stockEntries = stockEntries,
                    auditLogs = auditLogs
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
