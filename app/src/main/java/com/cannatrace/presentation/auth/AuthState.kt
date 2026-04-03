package com.cannatrace.presentation.auth

import com.cannatrace.domain.model.User

sealed class AuthState {
    data object Idle : AuthState()
    data object Loading : AuthState()
    data class Success(val user: User) : AuthState()
    data class Error(val message: String) : AuthState()
    data class RequiresTwoFactor(val userId: String) : AuthState()
}
