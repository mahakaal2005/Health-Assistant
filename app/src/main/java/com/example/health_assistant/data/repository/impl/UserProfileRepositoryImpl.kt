package com.example.health_assistant.data.repository.impl

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.health_assistant.core.util.Result
import com.example.health_assistant.data.repository.interfaces.UserProfile
import com.example.health_assistant.data.repository.interfaces.UserProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of UserProfileRepository using DataStore Preferences
 */
@Singleton
class UserProfileRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : UserProfileRepository {

    companion object {
        private val USER_EMAIL_KEY = stringPreferencesKey("user_email")
        private val USER_ID_KEY = stringPreferencesKey("user_id")
        private val DISPLAY_NAME_KEY = stringPreferencesKey("display_name")
    }

    override fun getUserEmail(): Flow<Result<String?>> {
        return dataStore.data
            .map<Preferences, Result<String?>> { preferences ->
                Result.Success(preferences[USER_EMAIL_KEY])
            }
            .catch { exception ->
                emit(Result.Error(exception = exception))
            }
    }

    override fun getUserId(): Flow<Result<String?>> {
        return dataStore.data
            .map<Preferences, Result<String?>> { preferences ->
                Result.Success(preferences[USER_ID_KEY])
            }
            .catch { exception ->
                emit(Result.Error(exception = exception))
            }
    }

    override suspend fun saveUserProfile(userId: String, email: String): Result<Unit> {
        return try {
            dataStore.edit { preferences ->
                preferences[USER_ID_KEY] = userId
                preferences[USER_EMAIL_KEY] = email
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(exception = e)
        }
    }

    override suspend fun clearUserProfile(): Result<Unit> {
        return try {
            dataStore.edit { preferences ->
                preferences.remove(USER_ID_KEY)
                preferences.remove(USER_EMAIL_KEY)
                preferences.remove(DISPLAY_NAME_KEY)
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(exception = e)
        }
    }

    override suspend fun updateDisplayName(displayName: String): Result<Unit> {
        return try {
            dataStore.edit { preferences ->
                preferences[DISPLAY_NAME_KEY] = displayName
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(exception = e)
        }
    }

    override suspend fun getUserProfile(): Result<UserProfile?> {
        return try {
            val preferences = dataStore.data.map { it }.catch { throw it }
            var userProfile: UserProfile? = null

            preferences.collect { prefs ->
                val userId = prefs[USER_ID_KEY]
                val email = prefs[USER_EMAIL_KEY]
                val displayName = prefs[DISPLAY_NAME_KEY]

                if (userId != null && email != null) {
                    userProfile = UserProfile(
                        userId = userId,
                        email = email,
                        displayName = displayName
                    )
                }
            }

            Result.Success(userProfile)
        } catch (e: Exception) {
            Result.Error(exception = e)
        }
    }
}