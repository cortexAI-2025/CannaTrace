package com.cannatrace.data.remote

/**
 * Wrapper générique pour les réponses de l'API REST.
 */
sealed class ApiResponse<out T> {
    data class Success<T>(val data: T, val message: String = "") : ApiResponse<T>()
    data class Error(val code: Int, val message: String, val details: String = "") : ApiResponse<Nothing>()
    data object Loading : ApiResponse<Nothing>()

    val isSuccess get() = this is Success
    val isError get() = this is Error

    fun getOrNull(): T? = if (this is Success) data else null

    fun errorMessage(): String = if (this is Error) message else ""
}

data class PagedResponse<T>(
    val content: List<T>,
    val totalElements: Long,
    val totalPages: Int,
    val page: Int,
    val size: Int
)

data class AuthTokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
    val userId: String,
    val role: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class RefreshTokenRequest(
    val refreshToken: String
)
