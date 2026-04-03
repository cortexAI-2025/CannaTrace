package com.cannatrace.domain.repository

import com.cannatrace.domain.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    suspend fun login(email: String, password: String): Result<User>
    suspend fun logout()
    suspend fun getCurrentUser(): User?
    fun getCurrentUserFlow(): Flow<User?>
    suspend fun getUserById(id: String): User?
    suspend fun createUser(user: User, password: String): Result<User>
    suspend fun updateUser(user: User): Result<User>
    suspend fun verifyTwoFactor(userId: String, code: String): Result<Boolean>
    suspend fun saveSession(user: User, token: String)
    suspend fun clearSession()
    suspend fun isLoggedIn(): Boolean
}
