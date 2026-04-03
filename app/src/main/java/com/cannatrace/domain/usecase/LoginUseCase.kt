package com.cannatrace.domain.usecase

import com.cannatrace.domain.model.User
import com.cannatrace.domain.repository.UserRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val userRepository: UserRepository
) {

    suspend operator fun invoke(email: String, password: String): Result<User> {
        if (email.isBlank()) {
            return Result.failure(IllegalArgumentException("L'adresse e-mail est obligatoire"))
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return Result.failure(IllegalArgumentException("Format d'e-mail invalide"))
        }
        if (password.isBlank()) {
            return Result.failure(IllegalArgumentException("Le mot de passe est obligatoire"))
        }
        if (password.length < 8) {
            return Result.failure(IllegalArgumentException("Le mot de passe doit contenir au moins 8 caractères"))
        }

        return userRepository.login(email.trim().lowercase(), password)
    }
}
