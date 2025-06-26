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
}

/**
 * Data class representing user profile information
 */
data class UserProfile(
    val userId: String,
    val email: String,
    val displayName: String? = null,
    val photoUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)