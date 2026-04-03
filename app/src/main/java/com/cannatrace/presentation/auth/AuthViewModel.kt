package com.cannatrace.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cannatrace.domain.usecase.LoginUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _passwordVisible = MutableStateFlow(false)
    val passwordVisible: StateFlow<Boolean> = _passwordVisible.asStateFlow()

    fun onEmailChanged(value: String) { _email.value = value }

    fun onPasswordChanged(value: String) { _password.value = value }

    fun togglePasswordVisibility() { _passwordVisible.value = !_passwordVisible.value }

    fun login() {
        val emailVal = _email.value.trim()
        val passwordVal = _password.value

        if (emailVal.isBlank()) {
            _authState.value = AuthState.Error("L'adresse email est obligatoire.")
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(emailVal).matches()) {
            _authState.value = AuthState.Error("Format d'email invalide.")
            return
        }
        if (passwordVal.isBlank()) {
            _authState.value = AuthState.Error("Le mot de passe est obligatoire.")
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            loginUseCase(emailVal, passwordVal)
                .onSuccess { user ->
                    _authState.value = AuthState.Success(user)
                }
                .onFailure { error ->
                    _authState.value = AuthState.Error(
                        error.message ?: "Erreur d'authentification."
                    )
                }
        }
    }

    fun resetState() { _authState.value = AuthState.Idle }
}
