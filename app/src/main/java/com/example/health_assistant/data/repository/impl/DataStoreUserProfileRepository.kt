package com.example.health_assistant.data.repository.impl

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.health_assistant.core.util.Result
import com.example.health_assistant.core.util.RetryUtil
import com.example.health_assistant.core.util.safeSuspendCall
import com.example.health_assistant.data.repository.interfaces.UserProfile
import com.example.health_assistant.data.repository.interfaces.UserProfileRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// Create a DataStore instance for user profile
private val Context.userProfileDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_profile")

/**
 * DataStore implementation of UserProfileRepository with robust error handling
 */
@Singleton
class DataStoreUserProfileRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : UserProfileRepository {

    private val dataStore = context.userProfileDataStore

    private object PreferencesKeys {
        val USER_ID = stringPreferencesKey("user_id")
        val EMAIL = stringPreferencesKey("email")
        val DISPLAY_NAME = stringPreferencesKey("display_name")
        val PHOTO_URL = stringPreferencesKey("photo_url")
        val CREATED_AT = longPreferencesKey("created_at")
    }

    override fun getUserEmail(): Flow<Result<String?>> = dataStore.data
        .map<Preferences, Result<String?>> { preferences ->
            Result.Success(preferences[PreferencesKeys.EMAIL])
        }
        .catch { exception ->
            emit(Result.Error(exception = exception))
        }

    override fun getUserId(): Flow<Result<String?>> = dataStore.data
        .map<Preferences, Result<String?>> { preferences ->
            Result.Success(preferences[PreferencesKeys.USER_ID])
        }
        .catch { exception ->
            emit(Result.Error(exception = exception))
        }

    override suspend fun saveUserProfile(userId: String, email: String): Result<Unit> =
        safeSuspendCall {
            RetryUtil.retryWithBackoff(
                maxRetries = 2,
                shouldRetry = { it is java.io.IOException }
            ) {
                dataStore.edit { preferences ->
                    preferences[PreferencesKeys.USER_ID] = userId
                    preferences[PreferencesKeys.EMAIL] = email
                    preferences[PreferencesKeys.CREATED_AT] = System.currentTimeMillis()
                }
            }
        }

    override suspend fun clearUserProfile(): Result<Unit> = safeSuspendCall {
        RetryUtil.retryWithBackoff(
            maxRetries = 2,
            shouldRetry = { it is java.io.IOException }
        ) {
            dataStore.edit { preferences ->
                preferences.clear()
            }
        }
    }

    override suspend fun updateDisplayName(displayName: String): Result<Unit> =
        safeSuspendCall {
            RetryUtil.retryWithBackoff(
                maxRetries = 2,
                shouldRetry = { it is java.io.IOException }
            ) {
                dataStore.edit { preferences ->
                    preferences[PreferencesKeys.DISPLAY_NAME] = displayName
                }
            }
        }

    override suspend fun getUserProfile(): Result<UserProfile?> = safeSuspendCall {
        var userProfile: UserProfile? = null

        dataStore.data.collect { preferences ->
            val userId = preferences[PreferencesKeys.USER_ID]
            val email = preferences[PreferencesKeys.EMAIL]

            userProfile = if (userId != null && email != null) {
                UserProfile(
                    userId = userId,
                    email = email,
                    displayName = preferences[PreferencesKeys.DISPLAY_NAME],
                    photoUrl = preferences[PreferencesKeys.PHOTO_URL],
                    createdAt = preferences[PreferencesKeys.CREATED_AT] ?: System.currentTimeMillis()
                )
            } else {
                null
            }
        }

        userProfile
    }
}