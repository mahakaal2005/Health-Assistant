package com.example.health_assistant.auth.repository

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Repository class that handles Firebase Authentication operations
 */
class FirebaseAuthRepository {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val TAG = "FirebaseAuthRepository"

    /**
     * Get the current logged-in user
     * @return FirebaseUser? Current user or null if not logged in
     */
    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }

    /**
     * Register a new user with email and password
     * @param email User's email
     * @param password User's password
     * @param onSuccess Callback for successful registration
     * @param onFailure Callback for registration failure
     */
    fun registerUser(
        email: String,
        password: String,
        onSuccess: (FirebaseUser?) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "createUserWithEmail:success")
                    onSuccess(auth.currentUser)
                } else {
                    Log.w(TAG, "createUserWithEmail:failure", task.exception)
                    onFailure(task.exception ?: Exception("Registration failed"))
                }
            }
    }

    /**
     * Sign in an existing user with email and password
     * @param email User's email
     * @param password User's password
     * @param onSuccess Callback for successful login
     * @param onFailure Callback for login failure
     */
    fun signInUser(
        email: String,
        password: String,
        onSuccess: (FirebaseUser?) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "signInWithEmail:success")
                    onSuccess(auth.currentUser)
                } else {
                    Log.w(TAG, "signInWithEmail:failure", task.exception)
                    onFailure(task.exception ?: Exception("Login failed"))
                }
            }
    }

    /**
     * Sign out the current user
     */
    fun signOut() {
        auth.signOut()
    }

    /**
     * Send password reset email
     * @param email User's email
     * @param onSuccess Callback for successful password reset email sent
     * @param onFailure Callback for password reset email failure
     */
    fun resetPassword(
        email: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "resetPassword:success")
                    onSuccess()
                } else {
                    Log.w(TAG, "resetPassword:failure", task.exception)
                    onFailure(task.exception ?: Exception("Password reset failed"))
                }
            }
    }
}