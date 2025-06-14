package com.example.health_assistant.auth.session

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.health_assistant.auth.repository.FirebaseAuthRepository

/**
 * Manages user session and authentication state using secure storage
 */
class SessionManager(private val context: Context) {

    private val TAG = "SessionManager"
    private val authRepository = FirebaseAuthRepository()
    private lateinit var encryptedPrefs: SharedPreferences

    // Keys for stored preferences
    companion object {
        private const val PREF_FILE_NAME = "health_assistant_secure_prefs"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"
    }

    init {
        initEncryptedPrefs()
    }

    /**
     * Initialize the encrypted shared preferences
     */
    private fun initEncryptedPrefs() {
        try {
            // Create a master key for encryption
            val masterKeySpec = KeyGenParameterSpec.Builder(
                MasterKey.DEFAULT_MASTER_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()

            val masterKey = MasterKey.Builder(context)
                .setKeyGenParameterSpec(masterKeySpec)
                .build()

            // Create or get the encrypted shared preferences file
            encryptedPrefs = EncryptedSharedPreferences.create(
                context,
                PREF_FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )

            Log.d(TAG, "Encrypted SharedPreferences initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Encrypted SharedPreferences", e)
            // Fallback to regular SharedPreferences if encryption fails
            encryptedPrefs = context.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE)
        }
    }

    /**
     * Save the user session after successful login
     */
    fun createLoginSession(userId: String, email: String) {
        encryptedPrefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_USER_ID, userId)
            putString(KEY_USER_EMAIL, email)
            apply()
        }
        Log.d(TAG, "Login session created for user: $userId")
    }

    /**
     * Check if user is logged in
     */
    fun isLoggedIn(): Boolean {
        // First check Firebase's current user
        val firebaseUser = authRepository.getCurrentUser()

        // If Firebase has a current user, ensure our local session is also updated
        if (firebaseUser != null) {
            if (!encryptedPrefs.getBoolean(KEY_IS_LOGGED_IN, false)) {
                // Update local session data if Firebase shows logged in but local storage doesn't
                createLoginSession(firebaseUser.uid, firebaseUser.email ?: "")
            }
            return true
        }

        // If Firebase doesn't have a current user, check our local session
        return encryptedPrefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    /**
     * Get the current user ID
     */
    fun getUserId(): String? {
        return encryptedPrefs.getString(KEY_USER_ID, null)
    }

    /**
     * Get the current user email
     */
    fun getUserEmail(): String? {
        return encryptedPrefs.getString(KEY_USER_EMAIL, null)
    }

    /**
     * Clear user session and log out
     */
    fun logout() {
        // Clear Firebase Auth
        authRepository.signOut()

        // Clear session data
        encryptedPrefs.edit().apply {
            clear()
            apply()
        }
        Log.d(TAG, "User logged out, session cleared")
    }
}