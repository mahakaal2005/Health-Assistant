package com.example.health_assistant.data.repository.impl

import android.util.Log
import com.example.health_assistant.data.repository.interfaces.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of AuthRepository using Firebase Authentication
 */
@Singleton
class FirebaseAuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) : AuthRepository {

    private val TAG = "FirebaseAuthRepository"

    override fun getCurrentUser(): Flow<FirebaseUser?> = callbackFlow {
        // Initially emit the current user
        trySend(firebaseAuth.currentUser)

        // Set up auth state listener
        val authStateListener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser)
        }

        // Register the listener
        firebaseAuth.addAuthStateListener(authStateListener)

        // Clean up when flow collection ends
        awaitClose {
            firebaseAuth.removeAuthStateListener(authStateListener)
        }
    }

    override suspend fun registerUser(email: String, password: String): Result<FirebaseUser?> {
        return try {
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val user = authResult.user
            Log.d(TAG, "Registration successful for user: ${user?.uid}")
            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Registration failed: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun signInUser(email: String, password: String): Result<FirebaseUser?> {
        return try {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val user = authResult.user
            Log.d(TAG, "Sign-in successful for user: ${user?.uid}")
            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Sign-in failed: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun signOut() {
        Log.d(TAG, "Signing out user")
        firebaseAuth.signOut()
    }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun isUserLoggedIn(): Boolean {
        return firebaseAuth.currentUser != null
    }
}