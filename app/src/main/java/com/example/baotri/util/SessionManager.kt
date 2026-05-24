package com.example.baotri.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "session")

@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val USER_ID          = longPreferencesKey("user_id")
        val USERNAME         = stringPreferencesKey("username")
        val FULL_NAME        = stringPreferencesKey("full_name")
        val ROLE             = stringPreferencesKey("role")
        val IS_LOGGED_IN     = booleanPreferencesKey("is_logged_in")
        // Ghi nhớ đăng nhập — chỉ lưu username, KHÔNG lưu password
        val REMEMBERED_USER  = stringPreferencesKey("remembered_username")
    }

    val isLoggedIn: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.IS_LOGGED_IN] ?: false }

    val currentUserId: Flow<Long> =
        context.dataStore.data.map { it[Keys.USER_ID] ?: -1L }

    val currentRole: Flow<String> =
        context.dataStore.data.map { it[Keys.ROLE] ?: "" }

    val currentFullName: Flow<String> =
        context.dataStore.data.map { it[Keys.FULL_NAME] ?: "" }

    // Username được ghi nhớ (không bao giờ lưu password)
    val rememberedUsername: Flow<String> =
        context.dataStore.data.map { it[Keys.REMEMBERED_USER] ?: "" }

    suspend fun saveSession(userId: Long, username: String, fullName: String, role: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.USER_ID]      = userId
            prefs[Keys.USERNAME]     = username
            prefs[Keys.FULL_NAME]    = fullName
            prefs[Keys.ROLE]         = role
            prefs[Keys.IS_LOGGED_IN] = true
        }
    }

    suspend fun saveRememberedUsername(username: String) {
        context.dataStore.edit { it[Keys.REMEMBERED_USER] = username }
    }

    suspend fun clearRememberedUsername() {
        context.dataStore.edit { it.remove(Keys.REMEMBERED_USER) }
    }

    suspend fun clearSession() {
        context.dataStore.edit { prefs ->
            // Xóa session nhưng GIỮ remembered username
            val remembered = prefs[Keys.REMEMBERED_USER] ?: ""
            prefs.clear()
            if (remembered.isNotBlank()) prefs[Keys.REMEMBERED_USER] = remembered
        }
    }
}
