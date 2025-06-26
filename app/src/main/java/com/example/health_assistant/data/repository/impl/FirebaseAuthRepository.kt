package com.example.health_assistant.data.repository.impl

import com.example.health_assistant.core.util.Result
import com.example.health_assistant.core.util.RetryUtil
import com.example.health_assistant.core.util.safeSuspendCall
import com.example.health_assistant.data.repository.interfaces.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase implementation of AuthRepository with robust error handling and retry logic
 */
@Singleton
class FirebaseAuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) : AuthRepository {

    override fun getCurrentUser(): Flow<Result<FirebaseUser?>> = callbackFlow {
        val authStateListener = FirebaseAuth.AuthStateListener { auth ->
            trySend(Result.Success(auth.currentUser))
        }

        firebaseAuth.addAuthStateListener(authStateListener)

        awaitClose {
            firebaseAuth.removeAuthStateListener(authStateListener)
        }
    }.catch<Result<FirebaseUser?>> { exception ->
        emit(Result.Error(exception = exception))
    }

    override suspend fun registerUser(email: String, password: String): Result<FirebaseUser?> =
        safeSuspendCall {
            RetryUtil.retryWithBackoff(
                maxRetries = 3,
                shouldRetry = { exception ->
                    // Retry on network issues but not on auth-specific errors
                    RetryUtil.isRetryableException(exception) &&
                    !exception.message?.contains("auth", ignoreCase = true).orElse(false)
                }
            ) {
                val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
                authResult.user
            }
        }

    override suspend fun signInUser(email: String, password: String): Result<FirebaseUser?> =
        safeSuspendCall {
            RetryUtil.retryWithBackoff(
                maxRetries = 3,
                shouldRetry = { exception ->
                    // Retry on network issues but not on auth-specific errors
                    RetryUtil.isRetryableException(exception) &&
                    !exception.message?.contains("auth", ignoreCase = true).orElse(false)
                }
            ) {
                val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
                authResult.user
            }
        }

    override suspend fun signOut(): Result<Unit> = safeSuspendCall {
        firebaseAuth.signOut()
    }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> = safeSuspendCall {
        RetryUtil.retryWithBackoff(
            maxRetries = 3,
            shouldRetry = RetryUtil::isRetryableException
        ) {
            firebaseAuth.sendPasswordResetEmail(email).await()
        }
    }

    override fun isUserLoggedIn(): Boolean {
        return firebaseAuth.currentUser != null
    }

    override suspend fun deleteAccount(): Result<Unit> = safeSuspendCall {
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            RetryUtil.retryWithBackoff(
                maxRetries = 2,
                shouldRetry = RetryUtil::isRetryableException
            ) {
                currentUser.delete().await()
            }
        } else {
            throw IllegalStateException("No user is currently logged in")
        }
    }

    override suspend fun updatePassword(newPassword: String): Result<Unit> = safeSuspendCall {
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            RetryUtil.retryWithBackoff(
                maxRetries = 2,
                shouldRetry = RetryUtil::isRetryableException
            ) {
                currentUser.updatePassword(newPassword).await()
            }
        } else {
            throw IllegalStateException("No user is currently logged in")
        }
    }
}

// Extension function to safely handle null boolean operations
private fun Boolean?.orElse(defaultValue: Boolean): Boolean = this ?: defaultValue