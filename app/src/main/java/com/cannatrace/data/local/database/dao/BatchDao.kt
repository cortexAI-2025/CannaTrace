package com.cannatrace.data.local.database.dao

import androidx.room.*
import com.cannatrace.data.local.database.entities.BatchEntity
import com.cannatrace.data.local.database.entities.CorrectionNoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BatchDao {

    @Query("SELECT * FROM batches ORDER BY createdAt DESC")
    fun getAllBatches(): Flow<List<BatchEntity>>

    @Query("SELECT * FROM batches WHERE status = :status ORDER BY createdAt DESC")
    fun getBatchesByStatus(status: String): Flow<List<BatchEntity>>

    @Query("SELECT * FROM batches WHERE id = :id")
    suspend fun getBatchById(id: String): BatchEntity?

    @Query("SELECT * FROM batches WHERE batchNumber = :batchNumber LIMIT 1")
    suspend fun getBatchByNumber(batchNumber: String): BatchEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertBatch(batch: BatchEntity)

    @Update
    suspend fun updateBatch(batch: BatchEntity)

    @Delete
    suspend fun deleteBatch(batch: BatchEntity)

    @Query("UPDATE batches SET isClosed = 1, status = 'CLOTURE', updatedAt = :updatedAt WHERE id = :batchId")
    suspend fun closeBatch(batchId: String, updatedAt: Long)

    @Query("SELECT * FROM batches WHERE isClosed = 0 ORDER BY createdAt DESC")
    fun getActiveBatches(): Flow<List<BatchEntity>>

    @Query("SELECT COUNT(*) FROM batches WHERE status = :status")
    suspend fun countBatchesByStatus(status: String): Int

    // Correction notes
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCorrectionNote(note: CorrectionNoteEntity)

    @Query("SELECT * FROM correction_notes WHERE batchId = :batchId ORDER BY createdAt DESC")
    fun getCorrectionNotesByBatch(batchId: String): Flow<List<CorrectionNoteEntity>>
}
