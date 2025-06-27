package com.example.health_assistant.data.repository.interfaces

import com.example.health_assistant.core.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for user profile operations with robust error handling
 */
interface UserProfileRepository {
    /**
     * Get user email
     * @return Flow of Result<String?> containing user email or error
     */
    fun getUserEmail(): Flow<Result<String?>>

    /**
     * Get user ID
     * @return Flow of Result<String?> containing user ID or error
     */
    fun getUserId(): Flow<Result<String?>>

    /**
     * Save user profile information
     * @param userId User's unique identifier
     * @param email User's email address
     * @return Result indicating success or failure
     */
    suspend fun saveUserProfile(userId: String, email: String): Result<Unit>

    /**
     * Clear user profile data (useful for sign out)
     * @return Result indicating success or failure
     */
    suspend fun clearUserProfile(): Result<Unit>

    /**
     * Update user display name
     * @param displayName New display name
     * @return Result indicating success or failure
     */
    suspend fun updateDisplayName(displayName: String): Result<Unit>

    /**
     * Get user profile data
     * @return Result containing user profile or error
     */
    suspend fun getUserProfile(): Result<UserProfile?>

    /**
     * Update personal health information
     * @param personalHealthInfo Personal health data to save
     * @return Result indicating success or failure
     */
    suspend fun updatePersonalHealthInfo(personalHealthInfo: PersonalHealthInfo): Result<Unit>

    /**
     * Mark user profile as complete
     * @return Result indicating success or failure
     */
    suspend fun markProfileComplete(): Result<Unit>

    /**
     * Check if user profile is complete
     * @return Result containing boolean indicating if profile is complete
     */
    suspend fun isProfileComplete(): Result<Boolean>

    /**
     * Create user profile in Firestore after successful signup
     * @param userProfile Complete user profile data
     * @return Result indicating success or failure
     */
    suspend fun createUserProfileInFirestore(userProfile: UserProfile): Result<Unit>

    /**
     * Sync user profile from Firestore
     * @param userId User's unique identifier
     * @return Result containing user profile from Firestore or error
     */
    suspend fun syncUserProfileFromFirestore(userId: String): Result<UserProfile?>

    /**
     * Update user profile in Firestore
     * @param userProfile Updated user profile data
     * @return Result indicating success or failure
     */
    suspend fun updateUserProfileInFirestore(userProfile: UserProfile): Result<Unit>

    /**
     * Delete user profile from Firestore
     * @param userId User's unique identifier
     * @return Result indicating success or failure
     */
    suspend fun deleteUserProfileFromFirestore(userId: String): Result<Unit>
}

/**
 * Data class representing user profile information
 */
data class UserProfile(
    val userId: String,
    val email: String,
    val displayName: String? = null,
    val photoUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    // Personal health information
    val gender: String? = null,
    val height: Float? = null, // in cm
    val weight: Float? = null, // in kg
    val birthday: String? = null, // ISO date format (YYYY-MM-DD)
    val isProfileComplete: Boolean = false
)

/**
 * Data class for updating personal health information
 */
data class PersonalHealthInfo(
    val gender: String,
    val height: Float, // in cm
    val weight: Float, // in kg
    val birthday: String // ISO date format (YYYY-MM-DD)
)