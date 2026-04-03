package com.cannatrace.data.repository

import com.cannatrace.data.local.database.dao.UserDao
import com.cannatrace.data.local.mapper.toDomain
import com.cannatrace.data.local.mapper.toEntity
import com.cannatrace.data.remote.CannaTraceApi
import com.cannatrace.data.remote.LoginRequest
import com.cannatrace.domain.model.User
import com.cannatrace.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
    private val api: CannaTraceApi
) : UserRepository {

    override suspend fun login(email: String, password: String): Result<User> = runCatching {
        val response = api.login(LoginRequest(email, password))
        if (response.isSuccessful) {
            val tokenResponse = response.body()!!
            val existingUser = userDao.getUserByEmail(email)
                ?: throw IllegalStateException("Utilisateur non trouvé localement.")
            userDao.updateAuthToken(existingUser.id, tokenResponse.accessToken)
            existingUser.toDomain()
        } else {
            throw IllegalArgumentException("Identifiants invalides. Code: ${response.code()}")
        }
    }

    override suspend fun logout() {
        userDao.getLoggedInUser()?.let {
            userDao.clearAuthToken(it.id)
        }
    }

    override suspend fun getCurrentUser(): User? =
        userDao.getLoggedInUser()?.toDomain()

    override fun getCurrentUserFlow(): Flow<User?> =
        userDao.getAllActiveUsers().map { users ->
            users.firstOrNull { it.authToken.isNotBlank() }?.toDomain()
        }

    override suspend fun getUserById(id: String): User? =
        userDao.getUserById(id)?.toDomain()

    override suspend fun createUser(user: User, password: String): Result<User> = runCatching {
        // Le hash du mot de passe doit être fait côté serveur dans une implémentation réelle.
        // Ici on simule un hash simple pour le mock local.
        val hash = password.hashCode().toString()
        userDao.insertUser(user.toEntity(passwordHash = hash))
        user
    }

    override suspend fun updateUser(user: User): Result<User> = runCatching {
        val existing = userDao.getUserById(user.id)
        userDao.updateUser(
            user.toEntity(
                passwordHash = existing?.passwordHash ?: "",
                authToken = existing?.authToken ?: ""
            )
        )
        user
    }

    override suspend fun verifyTwoFactor(userId: String, code: String): Result<Boolean> = runCatching {
        // Dans une implémentation réelle : vérifier via TOTP/HOTP côté serveur
        // Pour le mock, on accepte le code "123456"
        code == "123456"
    }

    override suspend fun saveSession(user: User, token: String) {
        val existing = userDao.getUserById(user.id)
        if (existing == null) {
            userDao.insertUser(user.toEntity(authToken = token))
        } else {
            userDao.updateAuthToken(user.id, token)
        }
    }

    override suspend fun clearSession() {
        userDao.getLoggedInUser()?.let {
            userDao.clearAuthToken(it.id)
        }
    }

    override suspend fun isLoggedIn(): Boolean =
        userDao.getLoggedInUser() != null
}
