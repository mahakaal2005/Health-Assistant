package com.example.health_assistant.data.repository.interfaces

import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for authentication-related operations
 */
interface AuthRepository {
    /**
     * Get the current logged-in user
     * @return Flow of FirebaseUser that emits updates when auth state changes
     */
    fun getCurrentUser(): Flow<FirebaseUser?>

    /**
     * Register a new user with email and password
     * @param email User's email
     * @param password User's password
     * @return Result containing FirebaseUser or an Exception
     */
    suspend fun registerUser(email: String, password: String): Result<FirebaseUser?>

    /**
     * Sign in a user with email and password
     * @param email User's email
     * @param password User's password
     * @return Result containing FirebaseUser or an Exception
     */
    suspend fun signInUser(email: String, password: String): Result<FirebaseUser?>

    /**
     * Sign out the current user
     */
    suspend fun signOut()

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
}