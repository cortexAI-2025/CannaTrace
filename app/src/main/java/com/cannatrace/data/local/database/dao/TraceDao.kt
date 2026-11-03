package com.cannatrace.data.local.database.dao

import androidx.room.*
import com.cannatrace.data.local.database.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TraceDao {
    @Query("SELECT * FROM trace_metadata")
    fun metadata(): Flow<List<TraceMetadata>>
    @Query("SELECT * FROM trace_metadata WHERE batchId = :id")
    suspend fun metadata(id: String): TraceMetadata?
    @Insert suspend fun insertMetadata(meta: TraceMetadata)
    @Update suspend fun updateMetadata(meta: TraceMetadata)
    @Insert suspend fun append(event: TraceEvent)
    @Query("SELECT * FROM trace_events WHERE operationId = :id")
    suspend fun operation(id: String): TraceEvent?
    @Query("SELECT * FROM trace_events ORDER BY timestamp, operationId")
    suspend fun chain(): List<TraceEvent>
    @Query("SELECT * FROM trace_events ORDER BY timestamp DESC, operationId DESC")
    fun events(): Flow<List<TraceEvent>>
}
