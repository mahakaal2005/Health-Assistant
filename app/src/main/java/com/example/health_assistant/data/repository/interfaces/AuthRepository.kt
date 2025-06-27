package com.example.health_assistant.data.repository.interfaces

import com.example.health_assistant.core.util.Result
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for authentication-related operations with robust error handling
 */
interface AuthRepository {
    /**
     * Get the current logged-in user
     * @return Flow of Result<FirebaseUser?> that emits updates when auth state changes
     */
    fun getCurrentUser(): Flow<Result<FirebaseUser?>>

    /**
     * Register a new user with email and password
     * @param email User's email
     * @param password User's password
     * @return Result containing FirebaseUser or error details
     */
    suspend fun registerUser(email: String, password: String): Result<FirebaseUser?>

    /**
     * Sign in a user with email and password
     * @param email User's email
     * @param password User's password
     * @return Result containing FirebaseUser or error details
     */
    suspend fun signInUser(email: String, password: String): Result<FirebaseUser?>

    /**
     * Sign out the current user
     * @return Result indicating success or failure
     */
    suspend fun signOut(): Result<Unit>

    /**
     * Send a password reset email
     * @param email User's email
     * @return Result indicating success or failure
     */
    suspend fun sendPasswordResetEmail(email: String): Result<Unit>

    /**
     * Check if a user is currently logged in
     * @return true if logged in, false otherwise
     */
    fun isUserLoggedIn(): Boolean

    /**
     * Delete the current user account
     * @return Result indicating success or failure
     */
    suspend fun deleteAccount(): Result<Unit>

    /**
     * Re-authenticate the current user with their credentials
     * @param email User's email
     * @param password User's password
     * @return Result indicating success or failure
     */
    suspend fun reauthenticateUser(email: String, password: String): Result<Unit>

    /**
     * Update user password
     * @param newPassword The new password
     * @return Result indicating success or failure
     */
    suspend fun updatePassword(newPassword: String): Result<Unit>
}