package com.cannatrace.data.local.database.dao

import androidx.room.*
import com.cannatrace.data.local.database.entities.DivergenceReportEntity
import com.cannatrace.data.local.database.entities.StockEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StockEntryDao {

    @Query("SELECT * FROM stock_entries ORDER BY timestamp DESC")
    fun getAllStockEntries(): Flow<List<StockEntryEntity>>

    @Query("SELECT * FROM stock_entries WHERE batchId = :batchId ORDER BY timestamp DESC")
    fun getStockEntriesByBatch(batchId: String): Flow<List<StockEntryEntity>>

    @Query("SELECT * FROM stock_entries WHERE id = :id")
    suspend fun getStockEntryById(id: String): StockEntryEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertStockEntry(entry: StockEntryEntity)

    @Update
    suspend fun updateStockEntry(entry: StockEntryEntity)

    /**
     * Computes current stock by summing all entries:
     * ENTREE and RETOUR add to stock, all SORTIE movements subtract.
     */
    @Query("""
        SELECT SUM(
            CASE
                WHEN movementType IN ('ENTREE', 'RETOUR') THEN quantity
                ELSE -quantity
            END
        ) FROM stock_entries WHERE batchId = :batchId
    """)
    suspend fun computeCurrentStock(batchId: String): Double?

    @Query("SELECT batchId FROM stock_entries GROUP BY batchId HAVING SUM(CASE WHEN movementType IN ('ENTREE', 'RETOUR') THEN quantity ELSE -quantity END) <= :threshold")
    suspend fun getLowStockBatchIds(threshold: Double): List<String>

    // Divergence reports
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDivergenceReport(report: DivergenceReportEntity)

    @Query("SELECT * FROM divergence_reports WHERE batchId = :batchId ORDER BY reportedAt DESC")
    fun getDivergenceReportsByBatch(batchId: String): Flow<List<DivergenceReportEntity>>

    @Query("UPDATE divergence_reports SET resolved = 1, resolutionNotes = :notes WHERE id = :reportId")
    suspend fun resolveDivergenceReport(reportId: String, notes: String)
}
