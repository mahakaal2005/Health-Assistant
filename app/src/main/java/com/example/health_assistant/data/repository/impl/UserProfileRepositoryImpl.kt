package com.example.health_assistant.data.repository.impl

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import com.example.health_assistant.core.util.Result
import com.example.health_assistant.data.repository.interfaces.UserProfile
import com.example.health_assistant.data.repository.interfaces.UserProfileRepository
import com.example.health_assistant.data.repository.interfaces.PersonalHealthInfo
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of UserProfileRepository using DataStore Preferences and Firestore
 */
@Singleton
class UserProfileRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val firestore: FirebaseFirestore
) : UserProfileRepository {

    companion object {
        private val USER_EMAIL_KEY = stringPreferencesKey("user_email")
        private val USER_ID_KEY = stringPreferencesKey("user_id")
        private val DISPLAY_NAME_KEY = stringPreferencesKey("display_name")
        private val BIO_KEY = stringPreferencesKey("bio") // Add bio key for DataStore
        private val PHOTO_URL_KEY = stringPreferencesKey("photo_url")
        private val CREATED_AT_KEY = longPreferencesKey("created_at")
        // Personal health information keys
        private val GENDER_KEY = stringPreferencesKey("gender")
        private val HEIGHT_KEY = floatPreferencesKey("height")
        private val WEIGHT_KEY = floatPreferencesKey("weight")
        private val BIRTHDAY_KEY = stringPreferencesKey("birthday")
        private val IS_PROFILE_COMPLETE_KEY = booleanPreferencesKey("is_profile_complete")

        // Firestore collection name
        private const val USERS_COLLECTION = "users"
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
            // Use .first() instead of .collect() to avoid hanging
            val preferences = dataStore.data.catch { throw it }.first()
            var userProfile: UserProfile? = null

            val userId = preferences[USER_ID_KEY]
            val email = preferences[USER_EMAIL_KEY]
            val displayName = preferences[DISPLAY_NAME_KEY]

            if (userId != null && email != null) {
                userProfile = UserProfile(
                    userId = userId,
                    email = email,
                    displayName = displayName,
                    bio = preferences[BIO_KEY], // Include bio from DataStore
                    photoUrl = preferences[PHOTO_URL_KEY],
                    createdAt = preferences[CREATED_AT_KEY] ?: System.currentTimeMillis(),
                    gender = preferences[GENDER_KEY],
                    height = preferences[HEIGHT_KEY],
                    weight = preferences[WEIGHT_KEY],
                    birthday = preferences[BIRTHDAY_KEY],
                    isProfileComplete = preferences[IS_PROFILE_COMPLETE_KEY] ?: false
                )
            }

            Result.Success(userProfile)
        } catch (e: Exception) {
            Result.Error(exception = e)
        }
    }

    override suspend fun updatePersonalHealthInfo(personalHealthInfo: PersonalHealthInfo): Result<Unit> {
        return try {
            android.util.Log.d("UserProfileRepo", "Attempting to save health info: gender=${personalHealthInfo.gender}, height=${personalHealthInfo.height}, weight=${personalHealthInfo.weight}, birthday=${personalHealthInfo.birthday}")

            // Step 1: Save to local DataStore first
            dataStore.edit { preferences ->
                preferences[GENDER_KEY] = personalHealthInfo.gender
                preferences[HEIGHT_KEY] = personalHealthInfo.height
                preferences[WEIGHT_KEY] = personalHealthInfo.weight
                preferences[BIRTHDAY_KEY] = personalHealthInfo.birthday
            }

            android.util.Log.d("UserProfileRepo", "Health info saved successfully to DataStore")

            // Step 2: Get current user profile to sync to Firestore
            val currentProfile = getUserProfile()
            if (currentProfile is Result.Success && currentProfile.data != null) {
                val updatedProfile = currentProfile.data.copy(
                    gender = personalHealthInfo.gender,
                    height = personalHealthInfo.height,
                    weight = personalHealthInfo.weight,
                    birthday = personalHealthInfo.birthday
                )

                // Step 3: Update Firestore with the complete profile including health info
                val firestoreResult = updateUserProfileInFirestore(updatedProfile)
                if (firestoreResult is Result.Error) {
                    android.util.Log.w("UserProfileRepo", "Firestore sync failed but local save succeeded: ${firestoreResult.message}")
                    // Don't fail the entire operation if Firestore sync fails - local save succeeded
                } else {
                    android.util.Log.d("UserProfileRepo", "Health info successfully synced to Firestore")
                }
            } else {
                android.util.Log.w("UserProfileRepo", "Could not get current profile for Firestore sync, but local save succeeded")
            }

            // Simple verification without hanging Flow collection
            try {
                val currentPrefs = dataStore.data.map { it }.catch { emit(emptyPreferences()) }
                val firstValue = currentPrefs.first() // Use first() instead of collect with return
                android.util.Log.d("UserProfileRepo", "Verification read: gender=${firstValue[GENDER_KEY]}, height=${firstValue[HEIGHT_KEY]}, weight=${firstValue[WEIGHT_KEY]}, birthday=${firstValue[BIRTHDAY_KEY]}")
            } catch (verifyException: Exception) {
                android.util.Log.w("UserProfileRepo", "Verification failed but save succeeded", verifyException)
            }

            Result.Success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("UserProfileRepo", "Failed to save health info to DataStore", e)
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
            // Use .first() instead of .collect() to avoid hanging
            val preferences = dataStore.data.catch { throw it }.first()
            val isComplete = preferences[IS_PROFILE_COMPLETE_KEY] ?: false

            Result.Success(isComplete)
        } catch (e: Exception) {
            Result.Error(exception = e)
        }
    }

    // ==================== FIRESTORE METHODS ====================

    override suspend fun createUserProfileInFirestore(userProfile: UserProfile): Result<Unit> {
        return try {
            // Create Firestore document with explicit field mapping and proper null handling
            // This ensures all fields are set with appropriate defaults instead of null values
            val profileData = mapOf(
                "uid" to userProfile.userId,
                "email" to userProfile.email,
                "displayName" to (userProfile.displayName ?: ""), // Empty string instead of null
                "bio" to (userProfile.bio ?: ""), // Empty string instead of null
                "photoUrl" to (userProfile.photoUrl ?: ""), // Empty string instead of null
                "createdAt" to com.google.firebase.Timestamp(java.util.Date(userProfile.createdAt)),
                "updatedAt" to com.google.firebase.Timestamp.now(),
                // Personal health information with proper defaults
                "gender" to (userProfile.gender ?: ""), // Empty string for unspecified gender
                "height" to (userProfile.height ?: 0.0f), // 0.0 for unset height
                "weight" to (userProfile.weight ?: 0.0f), // 0.0 for unset weight
                "birthday" to (userProfile.birthday ?: ""), // Empty string for unset birthday
                "isProfileComplete" to userProfile.isProfileComplete,
                // Default preferences structure
                "preferences" to mapOf(
                    "notifications" to true,
                    "units" to "metric"
                )
            )

            firestore.collection(USERS_COLLECTION)
                .document(userProfile.userId)
                .set(profileData)
                .await()

            // Also save to local DataStore for offline access
            saveUserProfile(userProfile.userId, userProfile.email)

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, "Failed to create user profile in Firestore")
        }
    }

    override suspend fun syncUserProfileFromFirestore(userId: String): Result<UserProfile?> {
        return try {
            val document = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .get()
                .await()

            if (document.exists()) {
                val data = document.data ?: return Result.Success(null)

                // Helper function to convert empty strings back to null for optional fields
                // BUT preserve actual display names even if they look empty
                fun String?.nullIfTrulyEmpty(): String? = when {
                    this == null -> null
                    this.isBlank() -> null
                    else -> this
                }

                val userProfile = UserProfile(
                    userId = data["uid"] as String,
                    email = data["email"] as String,
                    // For display name, preserve any non-blank value since users enter this during signup
                    displayName = (data["displayName"] as? String)?.takeIf { it.isNotBlank() },
                    // For bio, preserve any non-blank value that user has entered
                    bio = (data["bio"] as? String)?.takeIf { it.isNotBlank() },
                    photoUrl = (data["photoUrl"] as? String).nullIfTrulyEmpty(),
                    createdAt = (data["createdAt"] as? com.google.firebase.Timestamp)?.toDate()?.time ?: System.currentTimeMillis(),
                    // Handle health info fields with proper type conversion and null handling
                    gender = (data["gender"] as? String).nullIfTrulyEmpty(),
                    height = when (val heightValue = data["height"]) {
                        is Double -> if (heightValue == 0.0) null else heightValue.toFloat()
                        is Float -> if (heightValue == 0.0f) null else heightValue
                        is Long -> if (heightValue == 0L) null else heightValue.toFloat()
                        else -> null
                    },
                    weight = when (val weightValue = data["weight"]) {
                        is Double -> if (weightValue == 0.0) null else weightValue.toFloat()
                        is Float -> if (weightValue == 0.0f) null else weightValue
                        is Long -> if (weightValue == 0L) null else weightValue.toFloat()
                        else -> null
                    },
                    birthday = (data["birthday"] as? String).nullIfTrulyEmpty(),
                    isProfileComplete = data["isProfileComplete"] as? Boolean ?: false
                )

                // Update local DataStore with synced data
                updateLocalProfileFromFirestore(userProfile)

                Result.Success(userProfile)
            } else {
                Result.Success(null)
            }
        } catch (e: Exception) {
            Result.Error(e, "Failed to sync user profile from Firestore")
        }
    }

    override suspend fun updateUserProfileInFirestore(userProfile: UserProfile): Result<Unit> {
        return try {
            // Use consistent null handling for updates - same as create method
            val updateData = mapOf(
                "displayName" to (userProfile.displayName ?: ""), // Empty string instead of null
                "bio" to (userProfile.bio ?: ""), // Include bio in Firestore updates
                "photoUrl" to (userProfile.photoUrl ?: ""), // Empty string instead of null
                "updatedAt" to com.google.firebase.Timestamp.now(),
                "gender" to (userProfile.gender ?: ""), // Empty string for unspecified gender
                "height" to (userProfile.height ?: 0.0f), // 0.0 for unset height
                "weight" to (userProfile.weight ?: 0.0f), // 0.0 for unset weight
                "birthday" to (userProfile.birthday ?: ""), // Empty string for unset birthday
                "isProfileComplete" to userProfile.isProfileComplete
            )

            firestore.collection(USERS_COLLECTION)
                .document(userProfile.userId)
                .set(updateData, SetOptions.merge())
                .await()

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, "Failed to update user profile in Firestore")
        }
    }

    override suspend fun deleteUserProfileFromFirestore(userId: String): Result<Unit> {
        return try {
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .delete()
                .await()

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, "Failed to delete user profile from Firestore")
        }
    }

    // ==================== PRIVATE HELPER METHODS ====================

    /**
     * Helper method to update local DataStore with data from Firestore
     */
    private suspend fun updateLocalProfileFromFirestore(userProfile: UserProfile) {
        try {
            dataStore.edit { preferences ->
                preferences[USER_ID_KEY] = userProfile.userId
                preferences[USER_EMAIL_KEY] = userProfile.email
                userProfile.displayName?.let { preferences[DISPLAY_NAME_KEY] = it }
                userProfile.bio?.let { preferences[BIO_KEY] = it }
                userProfile.photoUrl?.let { preferences[PHOTO_URL_KEY] = it }
                preferences[CREATED_AT_KEY] = userProfile.createdAt
                userProfile.gender?.let { preferences[GENDER_KEY] = it }
                userProfile.height?.let { preferences[HEIGHT_KEY] = it }
                userProfile.weight?.let { preferences[WEIGHT_KEY] = it }
                userProfile.birthday?.let { preferences[BIRTHDAY_KEY] = it }
                preferences[IS_PROFILE_COMPLETE_KEY] = userProfile.isProfileComplete
            }
        } catch (e: Exception) {
            // Log error but don't throw - Firestore sync succeeded
            android.util.Log.e("UserProfileRepo", "Failed to update local DataStore", e)
        }
    }

    override suspend fun saveUserProfileLocally(userProfile: UserProfile): Result<Unit> {
        return try {
            dataStore.edit { preferences ->
                preferences[USER_ID_KEY] = userProfile.userId
                preferences[USER_EMAIL_KEY] = userProfile.email
                preferences[DISPLAY_NAME_KEY] = userProfile.displayName ?: ""
                preferences[BIO_KEY] = userProfile.bio ?: "" // Include bio in local storage
                preferences[PHOTO_URL_KEY] = userProfile.photoUrl ?: ""
                preferences[CREATED_AT_KEY] = userProfile.createdAt
                preferences[GENDER_KEY] = userProfile.gender ?: ""
                preferences[HEIGHT_KEY] = userProfile.height ?: 0f
                preferences[WEIGHT_KEY] = userProfile.weight ?: 0f
                preferences[BIRTHDAY_KEY] = userProfile.birthday ?: ""
                preferences[IS_PROFILE_COMPLETE_KEY] = userProfile.isProfileComplete
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(exception = e)
        }
    }
}