package com.cannatrace.domain.model

data class AuditLog(
    val id: String,
    val action: String,
    val entityType: String,
    val entityId: String,
    val userId: String,
    val timestamp: Long,
    val hash: String,
    val previousHash: String,
    val data: String
)
