package com.cannatrace.data.local.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [Index(value = ["email"], unique = true)]
)
data class UserEntity(
    @PrimaryKey val id: String,
    val email: String,
    val role: String,
    val name: String,
    val twoFactorEnabled: Boolean,
    val isActive: Boolean,
    val createdAt: Long,
    val passwordHash: String = "",
    val authToken: String = ""
)
