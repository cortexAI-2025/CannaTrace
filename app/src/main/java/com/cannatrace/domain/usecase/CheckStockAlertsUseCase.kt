package com.cannatrace.domain.usecase

import com.cannatrace.domain.model.Batch
import com.cannatrace.domain.repository.BatchRepository
import com.cannatrace.domain.repository.StockRepository
import javax.inject.Inject

data class StockAlert(
    val batchId: String,
    val batchNumber: String,
    val currentStock: Double,
    val threshold: Double,
    val alertType: StockAlertType
)

enum class StockAlertType {
    LOW_STOCK,
    OUT_OF_STOCK,
    EXPIRING_SOON,
    EXPIRED
}

class CheckStockAlertsUseCase @Inject constructor(
    private val stockRepository: StockRepository,
    private val batchRepository: BatchRepository
) {

    companion object {
        const val LOW_STOCK_THRESHOLD_GRAMS = 100.0
        const val EXPIRY_WARNING_DAYS = 30
    }

    suspend operator fun invoke(): Result<List<StockAlert>> {
        return try {
            val alerts = mutableListOf<StockAlert>()

            // Check low stock
            val lowStockBatchIds = stockRepository.getLowStockBatches(LOW_STOCK_THRESHOLD_GRAMS)
            for (batchId in lowStockBatchIds) {
                val batch = batchRepository.getBatchById(batchId) ?: continue
                val stock = stockRepository.computeCurrentStock(batchId)
                val alertType = if (stock <= 0) StockAlertType.OUT_OF_STOCK else StockAlertType.LOW_STOCK
                alerts.add(
                    StockAlert(
                        batchId = batchId,
                        batchNumber = batch.batchNumber,
                        currentStock = stock,
                        threshold = LOW_STOCK_THRESHOLD_GRAMS,
                        alertType = alertType
                    )
                )
            }

            // Check expiring batches
            val expiringBatches = batchRepository.getExpiringBatches(EXPIRY_WARNING_DAYS)
            for (batch in expiringBatches) {
                alerts.add(
                    StockAlert(
                        batchId = batch.id,
                        batchNumber = batch.batchNumber,
                        currentStock = batch.weight,
                        threshold = 0.0,
                        alertType = StockAlertType.EXPIRING_SOON
                    )
                )
            }

            Result.success(alerts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
