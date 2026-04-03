package com.cannatrace.domain.usecase

import com.cannatrace.utils.HashUtils
import javax.inject.Inject

class GenerateAuditHashUseCase @Inject constructor() {

    /**
     * Computes a SHA-256 hash for audit log entries using hash chaining.
     * hash = SHA256(previousHash|timestamp|action|entityId|data)
     */
    fun computeHash(
        previousHash: String,
        timestamp: Long,
        action: String,
        entityId: String,
        data: String
    ): String {
        return HashUtils.computeHash(previousHash, timestamp, action, entityId, data)
    }

    /**
     * Verifies the integrity of an audit log entry by recomputing and comparing the hash.
     */
    fun verifyHash(
        storedHash: String,
        previousHash: String,
        timestamp: Long,
        action: String,
        entityId: String,
        data: String
    ): Boolean {
        val recomputed = computeHash(previousHash, timestamp, action, entityId, data)
        return recomputed == storedHash
    }
}
