package com.cannatrace.domain.model

data class User(
    val id: String,
    val email: String,
    val role: UserRole,
    val name: String,
    val twoFactorEnabled: Boolean = false,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
