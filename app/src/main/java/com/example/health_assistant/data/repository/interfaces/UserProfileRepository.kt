package com.example.health_assistant.data.repository.interfaces

import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for user profile-related operations
 */
interface UserProfileRepository {
    /**
     * Get user's email address
     * @return User's email as a Flow
     */
    fun getUserEmail(): Flow<String?>

    /**
     * Get user's unique ID
     * @return User's ID as a Flow
     */
    fun getUserId(): Flow<String?>

    /**
     * Save user profile information
     * @param userId User's unique ID
     * @param email User's email address
     */
    suspend fun saveUserProfile(userId: String, email: String)

    /**
     * Clear user profile data (e.g., during logout)
     */
    suspend fun clearUserProfile()
}