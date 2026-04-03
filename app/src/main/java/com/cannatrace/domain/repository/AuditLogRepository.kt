package com.cannatrace.domain.repository

import com.cannatrace.domain.model.AuditLog
import kotlinx.coroutines.flow.Flow

interface AuditLogRepository {
    fun getAllAuditLogs(): Flow<List<AuditLog>>
    fun getAuditLogsByEntity(entityType: String, entityId: String): Flow<List<AuditLog>>
    fun getAuditLogsByUser(userId: String): Flow<List<AuditLog>>
    suspend fun getLatestAuditLog(): AuditLog?
    suspend fun createAuditLog(auditLog: AuditLog): Result<AuditLog>
    suspend fun verifyHashChain(): Boolean
    suspend fun getAuditLogById(id: String): AuditLog?
    suspend fun syncAuditLogs(): Result<Unit>
}
