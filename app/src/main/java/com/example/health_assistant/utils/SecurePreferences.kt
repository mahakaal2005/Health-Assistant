package com.example.health_assistant.utils

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStoreException
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.IOException
import java.security.GeneralSecurityException

/**
 * Utility class for handling encrypted SharedPreferences with robust fallback mechanisms
 */
class SecurePreferences(private val context: Context) {

    companion object {
        private const val TAG = "SecurePreferences"
        private const val PREFERENCES_FILE_NAME = "secure_health_preferences"
        private const val FALLBACK_PREFERENCES_NAME = "health_preferences_fallback"
        private const val ENCRYPTION_STATUS_KEY = "encryption_status"
        
        // Flag values
        private const val STATUS_ENCRYPTION_FAILED = "failed"
        private const val STATUS_ENCRYPTION_WORKING = "working"
        private const val STATUS_MIGRATED_TO_ENCRYPTED = "migrated_to_encrypted"
        private const val STATUS_MIGRATED_TO_FALLBACK = "migrated_to_fallback"
    }

    private var sharedPreferences: SharedPreferences
    private var isEncrypted = false
    
    // State flow for observing encryption state changes
    private val _encryptionState = MutableStateFlow<EncryptionState>(EncryptionState.INITIALIZING)
    val encryptionState: StateFlow<EncryptionState> = _encryptionState

    init {
        // Initialize with fallback first to ensure we have access to preferences
        val fallbackPrefs = context.getSharedPreferences(FALLBACK_PREFERENCES_NAME, Context.MODE_PRIVATE)
        
        // Check if we've previously recorded encryption failures
        val encryptionStatus = fallbackPrefs.getString(ENCRYPTION_STATUS_KEY, null)
        
        sharedPreferences = if (encryptionStatus == STATUS_ENCRYPTION_FAILED) {
            // Use fallback if encryption has previously failed
            Log.w(TAG, "Using fallback SharedPreferences due to previous failures")
            isEncrypted = false
            _encryptionState.value = EncryptionState.FALLBACK
            fallbackPrefs
        } else {
            // Try to initialize encrypted preferences
            initializeEncryptedSharedPreferences(fallbackPrefs)
        }
    }

    private fun initializeEncryptedSharedPreferences(fallbackPrefs: SharedPreferences): SharedPreferences {
        try {
            // Create or retrieve the Master Key for encryption
            val masterKeySpec = KeyGenParameterSpec.Builder(
                MasterKey.DEFAULT_MASTER_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(MasterKey.DEFAULT_AES_GCM_MASTER_KEY_SIZE)
                .build()

            val masterKey = MasterKey.Builder(context)
                .setKeyGenParameterSpec(masterKeySpec)
                .build()

            // Create the encrypted SharedPreferences using the MasterKey
            val encryptedPrefs = EncryptedSharedPreferences.create(
                context,
                PREFERENCES_FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )

            isEncrypted = true
            _encryptionState.value = EncryptionState.ENCRYPTED
            
            // Record successful encryption
            fallbackPrefs.edit().putString(ENCRYPTION_STATUS_KEY, STATUS_ENCRYPTION_WORKING).apply()
            
            Log.d(TAG, "Successfully initialized encrypted SharedPreferences")
            
            // Migrate data from fallback if needed
            if (shouldMigrateFromFallback(fallbackPrefs)) {
                migrateFromFallbackToEncrypted(fallbackPrefs, encryptedPrefs)
            }
            
            return encryptedPrefs

        } catch (e: Exception) {
            return fallbackToRegularPreferences(e, fallbackPrefs)
        }
    }

    /**
     * Determine if we need to migrate data from fallback preferences
     */
    private fun shouldMigrateFromFallback(fallbackPrefs: SharedPreferences): Boolean {
        // Check if fallback has data and we haven't already migrated
        val allEntries = fallbackPrefs.all
        val hasData = allEntries.isNotEmpty() && allEntries.any { it.key != ENCRYPTION_STATUS_KEY }
        val status = fallbackPrefs.getString(ENCRYPTION_STATUS_KEY, null)
        return hasData && status != STATUS_MIGRATED_TO_ENCRYPTED
    }
    
    /**
     * Migrate data from fallback preferences to encrypted preferences
     */
    private fun migrateFromFallbackToEncrypted(fallbackPrefs: SharedPreferences, encryptedPrefs: SharedPreferences) {
        try {
            Log.d(TAG, "Migrating data from fallback to encrypted preferences")
            val editor = encryptedPrefs.edit()
            
            // Copy all values except status flags
            for ((key, value) in fallbackPrefs.all) {
                if (key != ENCRYPTION_STATUS_KEY) {
                    when (value) {
                        is String -> editor.putString(key, value)
                        is Int -> editor.putInt(key, value)
                        is Boolean -> editor.putBoolean(key, value)
                        is Float -> editor.putFloat(key, value)
                        is Long -> editor.putLong(key, value)
                        is Set<*> -> {
                            @Suppress("UNCHECKED_CAST")
                            editor.putStringSet(key, value as Set<String>)
                        }
                    }
                }
            }
            
            // Apply changes and update migration status
            editor.apply()
            fallbackPrefs.edit()
                .putString(ENCRYPTION_STATUS_KEY, STATUS_MIGRATED_TO_ENCRYPTED)
                .apply()
                
            Log.d(TAG, "Migration to encrypted preferences successful")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to migrate data to encrypted preferences: ${e.message}")
        }
    }
    
    /**
     * Migrate data from encrypted preferences to fallback
     */
    private fun migrateFromEncryptedToFallback() {
        try {
            // Try to open the encrypted file without encryption
            val encryptedFile = context.getSharedPreferences(PREFERENCES_FILE_NAME, Context.MODE_PRIVATE)
            val fallbackPrefs = context.getSharedPreferences(FALLBACK_PREFERENCES_NAME, Context.MODE_PRIVATE)
            
            // Copy any readable values - this may fail if the file is truly encrypted
            val editor = fallbackPrefs.edit()
            for ((key, value) in encryptedFile.all) {
                when (value) {
                    is String -> editor.putString(key, value)
                    is Int -> editor.putInt(key, value)
                    is Boolean -> editor.putBoolean(key, value)
                    is Float -> editor.putFloat(key, value)
                    is Long -> editor.putLong(key, value)
                    is Set<*> -> {
                        @Suppress("UNCHECKED_CAST")
                        editor.putStringSet(key, value as Set<String>)
                    }
                }
            }
            
            editor.putString(ENCRYPTION_STATUS_KEY, STATUS_MIGRATED_TO_FALLBACK)
            editor.apply()
            Log.d(TAG, "Attempted migration from encrypted to fallback")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error migrating from encrypted preferences: ${e.message}")
        }
    }

    private fun fallbackToRegularPreferences(e: Exception, fallbackPrefs: SharedPreferences): SharedPreferences {
        val errorType = when (e) {
            is KeyStoreException, is IOException, is GeneralSecurityException -> {
                Log.e(TAG, "Security/KeyStore error: ${e.message}")
                EncryptionState.KEYSTORE_ERROR
            }
            else -> {
                Log.e(TAG, "Unexpected error with encrypted preferences: ${e.message}")
                EncryptionState.GENERAL_ERROR
            }
        }
        
        // Record the failure
        fallbackPrefs.edit().putString(ENCRYPTION_STATUS_KEY, STATUS_ENCRYPTION_FAILED).apply()
        
        // Try to migrate data if possible
        migrateFromEncryptedToFallback()
        
        isEncrypted = false
        _encryptionState.value = errorType
        
        Log.w(TAG, "Using fallback SharedPreferences due to encryption failure")
        return fallbackPrefs
    }

    /**
     * Reset encryption status to try encrypted storage again
     */
    fun resetEncryptionStatus() {
        val fallbackPrefs = context.getSharedPreferences(FALLBACK_PREFERENCES_NAME, Context.MODE_PRIVATE)
        fallbackPrefs.edit().remove(ENCRYPTION_STATUS_KEY).apply()
        _encryptionState.value = EncryptionState.RETRY_REQUESTED
    }
    
    // Helper methods with robust error handling
    
    fun putString(key: String, value: String?) {
        try {
            sharedPreferences.edit().putString(key, value).apply()
        } catch (e: Exception) {
            handleRuntimeFailure(e) {
                sharedPreferences.edit().putString(key, value).apply()
            }
        }
    }

    fun getString(key: String, defaultValue: String? = null): String? {
        return try {
            sharedPreferences.getString(key, defaultValue)
        } catch (e: Exception) {
            handleRuntimeFailure(e)
            defaultValue
        }
    }

    fun putInt(key: String, value: Int) {
        try {
            sharedPreferences.edit().putInt(key, value).apply()
        } catch (e: Exception) {
            handleRuntimeFailure(e) {
                sharedPreferences.edit().putInt(key, value).apply()
            }
        }
    }

    fun getInt(key: String, defaultValue: Int = 0): Int {
        return try {
            sharedPreferences.getInt(key, defaultValue)
        } catch (e: Exception) {
            handleRuntimeFailure(e)
            defaultValue
        }
    }

    fun putBoolean(key: String, value: Boolean) {
        try {
            sharedPreferences.edit().putBoolean(key, value).apply()
        } catch (e: Exception) {
            handleRuntimeFailure(e) {
                sharedPreferences.edit().putBoolean(key, value).apply()
            }
        }
    }

    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return try {
            sharedPreferences.getBoolean(key, defaultValue)
        } catch (e: Exception) {
            handleRuntimeFailure(e)
            defaultValue
        }
    }

    fun putLong(key: String, value: Long) {
        try {
            sharedPreferences.edit().putLong(key, value).apply()
        } catch (e: Exception) {
            handleRuntimeFailure(e) {
                sharedPreferences.edit().putLong(key, value).apply()
            }
        }
    }

    fun getLong(key: String, defaultValue: Long = 0L): Long {
        return try {
            sharedPreferences.getLong(key, defaultValue)
        } catch (e: Exception) {
            handleRuntimeFailure(e)
            defaultValue
        }
    }

    fun putFloat(key: String, value: Float) {
        try {
            sharedPreferences.edit().putFloat(key, value).apply()
        } catch (e: Exception) {
            handleRuntimeFailure(e) {
                sharedPreferences.edit().putFloat(key, value).apply()
            }
        }
    }

    fun getFloat(key: String, defaultValue: Float = 0f): Float {
        return try {
            sharedPreferences.getFloat(key, defaultValue)
        } catch (e: Exception) {
            handleRuntimeFailure(e)
            defaultValue
        }
    }

    fun contains(key: String): Boolean {
        return try {
            sharedPreferences.contains(key)
        } catch (e: Exception) {
            handleRuntimeFailure(e)
            false
        }
    }

    fun remove(key: String) {
        try {
            sharedPreferences.edit().remove(key).apply()
        } catch (e: Exception) {
            handleRuntimeFailure(e) {
                sharedPreferences.edit().remove(key).apply()
            }
        }
    }

    /**
     * Handle runtime failures by switching to fallback if needed
     */
    private fun handleRuntimeFailure(e: Exception, retryAction: (() -> Unit)? = null) {
        Log.e(TAG, "Runtime error accessing preferences: ${e.message}", e)

        // Only switch if we're currently using encrypted prefs
        if (isEncrypted) {
            Log.w(TAG, "Switching to fallback preferences due to runtime failure")
            val fallbackPrefs = context.getSharedPreferences(FALLBACK_PREFERENCES_NAME, Context.MODE_PRIVATE)
            fallbackPrefs.edit().putString(ENCRYPTION_STATUS_KEY, STATUS_ENCRYPTION_FAILED).apply()
            sharedPreferences = fallbackPrefs
            isEncrypted = false
            _encryptionState.value = EncryptionState.RUNTIME_FAILURE

            // Retry the operation with fallback prefs
            retryAction?.invoke()
        }
    }

    /**
     * Encryption state for monitoring encryption capability
     */
    enum class EncryptionState {
        INITIALIZING,
        ENCRYPTED,
        FALLBACK,
        KEYSTORE_ERROR,
        GENERAL_ERROR,
        RUNTIME_FAILURE,
        RETRY_REQUESTED
    }
}
