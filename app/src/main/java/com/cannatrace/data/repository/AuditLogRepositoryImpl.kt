package com.cannatrace.data.repository

import com.cannatrace.data.local.database.dao.AuditLogDao
import com.cannatrace.data.local.mapper.toDomain
import com.cannatrace.data.local.mapper.toEntity
import com.cannatrace.data.remote.CannaTraceApi
import com.cannatrace.domain.model.AuditLog
import com.cannatrace.domain.repository.AuditLogRepository
import com.cannatrace.utils.HashUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuditLogRepositoryImpl @Inject constructor(
    private val auditLogDao: AuditLogDao,
    private val api: CannaTraceApi
) : AuditLogRepository {

    override fun getAllAuditLogs(): Flow<List<AuditLog>> =
        auditLogDao.getAllAuditLogs().map { entities -> entities.map { it.toDomain() } }

    override fun getAuditLogsByEntity(entityType: String, entityId: String): Flow<List<AuditLog>> =
        auditLogDao.getAuditLogsByEntity(entityType, entityId).map { entities ->
            entities.map { it.toDomain() }
        }

    override fun getAuditLogsByUser(userId: String): Flow<List<AuditLog>> =
        auditLogDao.getAuditLogsByUser(userId).map { entities -> entities.map { it.toDomain() } }

    override suspend fun getLatestAuditLog(): AuditLog? =
        auditLogDao.getLatestAuditLog()?.toDomain()

    override suspend fun createAuditLog(auditLog: AuditLog): Result<AuditLog> = runCatching {
        auditLogDao.insertAuditLog(auditLog.toEntity())
        auditLog
    }

    /**
     * Vérifie l'intégrité de la chaîne de hachage des logs d'audit.
     * Chaque hash doit être cohérent avec son prédécesseur.
     */
    override suspend fun verifyHashChain(): Boolean {
        val logs = auditLogDao.getAllAuditLogsForVerification()
        if (logs.isEmpty()) return true

        for (i in 1 until logs.size) {
            val current = logs[i]
            val recomputed = HashUtils.computeHash(
                previousHash = current.previousHash,
                timestamp = current.timestamp,
                action = current.action,
                entityId = current.entityId,
                data = current.data
            )
            if (recomputed != current.hash) {
                return false
            }
        }
        return true
    }

    override suspend fun getAuditLogById(id: String): AuditLog? =
        auditLogDao.getAuditLogById(id)?.toDomain()

    override suspend fun syncAuditLogs(): Result<Unit> = runCatching {
        val response = api.getAuditLogs()
        if (response.isSuccessful) {
            response.body()?.content?.forEach { entity ->
                auditLogDao.insertAuditLog(entity)
            }
        }
    }
}
