package com.example.baotri.domain.usecase.auth

import com.example.baotri.domain.repository.SessionRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

data class UserSession(
    val userId: Long,
    val fullName: String,
    val role: String
)

class GetCurrentSessionUseCase @Inject constructor(
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(): UserSession = UserSession(
        userId   = sessionRepository.currentUserId.first(),
        fullName = sessionRepository.currentFullName.first(),
        role     = sessionRepository.currentRole.first()
    )
}