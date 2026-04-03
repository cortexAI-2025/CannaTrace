package com.cannatrace.data.local.database.dao

import androidx.room.*
import com.cannatrace.data.local.database.entities.AuditLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AuditLogDao {

    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC")
    fun getAllAuditLogs(): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_logs WHERE entityType = :entityType AND entityId = :entityId ORDER BY timestamp DESC")
    fun getAuditLogsByEntity(entityType: String, entityId: String): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_logs WHERE userId = :userId ORDER BY timestamp DESC")
    fun getAuditLogsByUser(userId: String): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestAuditLog(): AuditLogEntity?

    @Query("SELECT * FROM audit_logs WHERE id = :id")
    suspend fun getAuditLogById(id: String): AuditLogEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAuditLog(log: AuditLogEntity)

    @Query("SELECT * FROM audit_logs ORDER BY timestamp ASC")
    suspend fun getAllAuditLogsForVerification(): List<AuditLogEntity>

    @Query("SELECT COUNT(*) FROM audit_logs")
    suspend fun getAuditLogCount(): Int
}
