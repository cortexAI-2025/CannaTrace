package com.cannatrace.data.local.mapper

import com.cannatrace.data.local.database.entities.UserEntity
import com.cannatrace.domain.model.User
import com.cannatrace.domain.model.UserRole

fun UserEntity.toDomain(): User = User(
    id = id,
    email = email,
    role = UserRole.valueOf(role),
    name = name,
    twoFactorEnabled = twoFactorEnabled,
    isActive = isActive,
    createdAt = createdAt
)

fun User.toEntity(passwordHash: String = "", authToken: String = ""): UserEntity = UserEntity(
    id = id,
    email = email,
    role = role.name,
    name = name,
    twoFactorEnabled = twoFactorEnabled,
    isActive = isActive,
    createdAt = createdAt,
    passwordHash = passwordHash,
    authToken = authToken
)
