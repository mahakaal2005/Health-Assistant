package com.example.health_assistant.data.repository.impl

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import com.example.health_assistant.core.util.Result
import com.example.health_assistant.data.repository.interfaces.UserProfile
import com.example.health_assistant.data.repository.interfaces.UserProfileRepository
import com.example.health_assistant.data.repository.interfaces.PersonalHealthInfo
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
        private val PHOTO_URL_KEY = stringPreferencesKey("photo_url")
        private val CREATED_AT_KEY = longPreferencesKey("created_at")
        // Personal health information keys
        private val GENDER_KEY = stringPreferencesKey("gender")
        private val HEIGHT_KEY = floatPreferencesKey("height")
        private val WEIGHT_KEY = floatPreferencesKey("weight")
        private val BIRTHDAY_KEY = stringPreferencesKey("birthday")
        private val IS_PROFILE_COMPLETE_KEY = booleanPreferencesKey("is_profile_complete")
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
                        displayName = displayName,
                        photoUrl = prefs[PHOTO_URL_KEY],
                        createdAt = prefs[CREATED_AT_KEY] ?: System.currentTimeMillis(),
                        gender = prefs[GENDER_KEY],
                        height = prefs[HEIGHT_KEY],
                        weight = prefs[WEIGHT_KEY],
                        birthday = prefs[BIRTHDAY_KEY],
                        isProfileComplete = prefs[IS_PROFILE_COMPLETE_KEY] ?: false
                    )
                }
            }

            Result.Success(userProfile)
        } catch (e: Exception) {
            Result.Error(exception = e)
        }
    }

    override suspend fun updatePersonalHealthInfo(personalHealthInfo: PersonalHealthInfo): Result<Unit> {
        return try {
            dataStore.edit { preferences ->
                preferences[GENDER_KEY] = personalHealthInfo.gender
                preferences[HEIGHT_KEY] = personalHealthInfo.height
                preferences[WEIGHT_KEY] = personalHealthInfo.weight
                preferences[BIRTHDAY_KEY] = personalHealthInfo.birthday
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(exception = e)
        }
    }

    override suspend fun markProfileComplete(): Result<Unit> {
        return try {
            dataStore.edit { preferences ->
                preferences[IS_PROFILE_COMPLETE_KEY] = true
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(exception = e)
        }
    }

    override suspend fun isProfileComplete(): Result<Boolean> {
        return try {
            val preferences = dataStore.data.map { it }.catch { throw it }
            var isComplete = false

            preferences.collect { prefs ->
                isComplete = prefs[IS_PROFILE_COMPLETE_KEY] ?: false
            }

            Result.Success(isComplete)
        } catch (e: Exception) {
            Result.Error(exception = e)
        }
    }
}