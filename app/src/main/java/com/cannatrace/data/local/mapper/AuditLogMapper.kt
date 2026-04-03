package com.cannatrace.data.local.mapper

import com.cannatrace.data.local.database.entities.AuditLogEntity
import com.cannatrace.domain.model.AuditLog

fun AuditLogEntity.toDomain(): AuditLog = AuditLog(
    id = id,
    action = action,
    entityType = entityType,
    entityId = entityId,
    userId = userId,
    timestamp = timestamp,
    hash = hash,
    previousHash = previousHash,
    data = data
)

fun AuditLog.toEntity(): AuditLogEntity = AuditLogEntity(
    id = id,
    action = action,
    entityType = entityType,
    entityId = entityId,
    userId = userId,
    timestamp = timestamp,
    hash = hash,
    previousHash = previousHash,
    data = data
)
