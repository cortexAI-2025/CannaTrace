package com.cannatrace.data.local.database.dao

import androidx.room.*
import com.cannatrace.data.local.database.entities.CultureEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CultureEntryDao {

    @Query("SELECT * FROM culture_entries WHERE batchId = :batchId ORDER BY recordedAt DESC")
    fun getCultureEntriesByBatch(batchId: String): Flow<List<CultureEntryEntity>>

    @Query("SELECT * FROM culture_entries WHERE id = :id")
    suspend fun getCultureEntryById(id: String): CultureEntryEntity?

    @Query("SELECT * FROM culture_entries WHERE type = :type ORDER BY recordedAt DESC")
    fun getCultureEntriesByType(type: String): Flow<List<CultureEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCultureEntry(entry: CultureEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<CultureEntryEntity>)

    @Update
    suspend fun updateCultureEntry(entry: CultureEntryEntity)

    @Delete
    suspend fun deleteCultureEntry(entry: CultureEntryEntity)

    @Query("SELECT * FROM culture_entries WHERE batchId = :batchId AND type = :type ORDER BY recordedAt DESC LIMIT 1")
    suspend fun getLatestEntryForType(batchId: String, type: String): CultureEntryEntity?
}
