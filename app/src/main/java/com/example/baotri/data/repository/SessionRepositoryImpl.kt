package com.example.baotri.data.repository

import com.example.baotri.domain.repository.SessionRepository
import com.example.baotri.util.SessionManager
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SessionRepositoryImpl @Inject constructor(
    private val sessionManager: SessionManager
) : SessionRepository {

    override val isLoggedIn: Flow<Boolean>        = sessionManager.isLoggedIn
    override val currentUserId: Flow<Long>        = sessionManager.currentUserId
    override val currentRole: Flow<String>        = sessionManager.currentRole
    override val currentFullName: Flow<String>    = sessionManager.currentFullName
    override val rememberedUsername: Flow<String> = sessionManager.rememberedUsername

    override suspend fun saveSession(userId: Long, username: String, fullName: String, role: String) =
        sessionManager.saveSession(userId, username, fullName, role)

    override suspend fun saveRememberedUsername(username: String) =
        sessionManager.saveRememberedUsername(username)

    override suspend fun clearRememberedUsername() =
        sessionManager.clearRememberedUsername()

    override suspend fun clearSession() =
        sessionManager.clearSession()
}