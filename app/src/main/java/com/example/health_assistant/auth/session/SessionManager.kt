package com.example.health_assistant.auth.session

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.health_assistant.core.util.Result
import com.example.health_assistant.data.repository.interfaces.AuthRepository
import com.example.health_assistant.data.repository.interfaces.UserProfileRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages user session and authentication state using secure storage
 */
@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authRepository: AuthRepository,
    private val userProfileRepository: UserProfileRepository
) {

    private val TAG = "SessionManager"
    private lateinit var encryptedPrefs: SharedPreferences
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    // Keys for stored preferences
    companion object {
        private const val PREF_FILE_NAME = "health_assistant_secure_prefs"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"
    }

    init {
        initEncryptedPrefs()
        observeAuthStateChanges()
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
     * Observe Firebase auth state changes and sync with local session
     */
    private fun observeAuthStateChanges() {
        coroutineScope.launch {
            authRepository.getCurrentUser().collect { result ->
                when (result) {
                    is Result.Success -> {
                        val firebaseUser = result.data
                        if (firebaseUser != null) {
                            // User is logged in to Firebase, create local session
                            val userId = firebaseUser.uid
                            val email = firebaseUser.email ?: ""
                            createLoginSession(userId, email)
                            Log.d(TAG, "Firebase auth state: User logged in, local session created")
                        } else {
                            // User is logged out from Firebase, clear local session
                            clearSessionOnly()
                            Log.d(TAG, "Firebase auth state: User logged out, local session cleared")
                        }
                    }
                    is Result.Error -> {
                        Log.e(TAG, "Error observing auth state: ${result.message}")
                        clearSessionOnly()
                    }
                    is Result.Loading -> {
                        // Handle loading state if needed
                    }
                }
            }
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

        // Also save to UserProfileRepository
        coroutineScope.launch {
            userProfileRepository.saveUserProfile(userId, email)
        }

        Log.d(TAG, "Login session created for user: $userId")
    }

    /**
     * Clear session data only (internal use for auth state sync)
     */
    private fun clearSessionOnly() {
        encryptedPrefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, false)
            remove(KEY_USER_ID)
            remove(KEY_USER_EMAIL)
            apply()
        }

        coroutineScope.launch {
            userProfileRepository.clearUserProfile()
        }
    }

    /**
     * Check if user is logged in
     */
    fun isLoggedIn(): Boolean {
        // First check our local session state - if we've explicitly logged out, stay logged out
        val localLoggedIn = encryptedPrefs.getBoolean(KEY_IS_LOGGED_IN, false)

        // If local session says we're logged out, don't try to auto-login even if Firebase has a session
        if (!localLoggedIn) {
            return false
        }

        // If local session says we're logged in, verify with Firebase auth repository
        return authRepository.isUserLoggedIn()
    }

    /**
     * Asynchronously check if user is logged in
     * This method is safe to call from UI thread as it uses coroutines
     */
    suspend fun isLoggedInAsync(): Boolean = withContext(Dispatchers.IO) {
        try {
            // First check our local session state
            val localLoggedIn = encryptedPrefs.getBoolean(KEY_IS_LOGGED_IN, false)

            if (!localLoggedIn) {
                Log.d(TAG, "Local session indicates user is logged out")
                return@withContext false
            }

            // If local session says we're logged in, verify with Firebase auth repository
            val firebaseLoggedIn = authRepository.isUserLoggedIn()

            if (!firebaseLoggedIn) {
                // Firebase session expired, clear local session
                Log.d(TAG, "Firebase session expired, clearing local session")
                clearSession()
                return@withContext false
            }

            Log.d(TAG, "User session is valid")
            return@withContext true

        } catch (e: Exception) {
            Log.e(TAG, "Error checking login state: ${e.message}")
            return@withContext false
        }
    }

    /**
     * Clear session data without signing out from Firebase (internal use)
     */
    private fun clearSession() {
        encryptedPrefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, false)
            remove(KEY_USER_ID)
            remove(KEY_USER_EMAIL)
            apply()
        }
    }

    /**
     * Get the current user's email
     */
    fun getUserEmail(): String? {
        return encryptedPrefs.getString(KEY_USER_EMAIL, null)
    }

    /**
     * Get the current user's ID
     */
    fun getUserId(): String? {
        return encryptedPrefs.getString(KEY_USER_ID, null)
    }

    /**
     * Logout the user
     */
    fun logout() {
        // Clear session data from preferences
        encryptedPrefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, false)
            remove(KEY_USER_ID)
            remove(KEY_USER_EMAIL)
            apply()
        }

        // Sign out from Firebase and clear user profile
        coroutineScope.launch {
            authRepository.signOut()
            userProfileRepository.clearUserProfile()
        }

        Log.d(TAG, "User logged out and session cleared")
    }
}