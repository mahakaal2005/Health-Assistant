package com.example.health_assistant.features.onboarding

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.example.health_assistant.core.util.Result
import com.example.health_assistant.core.util.RetryUtil
import com.example.health_assistant.core.util.safeSuspendCall
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

// Create a DataStore instance using the preferencesDataStore delegate
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "health_assistant_preferences")

/**
 * Repository for managing onboarding preferences using DataStore with robust error handling
 * All operations return Result<T> to handle loading, success, and error states
 */
@Singleton
class OnboardingPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    // Define preference keys
    companion object {
        private val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }

    /**
     * Check if onboarding has been completed with error handling
     */
    val isOnboardingCompleted: Flow<Result<Boolean>> = flow {
        try {
            dataStore.data.collect { preferences ->
                emit(Result.Success(preferences[ONBOARDING_COMPLETED] ?: false))
            }
        } catch (exception: Exception) {
            emit(Result.Error(exception, "Failed to load onboarding completion status") as Result<Boolean>)
        }
    }

    /**
     * Mark onboarding as completed with retry logic and error handling
     */
    suspend fun setOnboardingCompleted(): Result<Unit> = safeSuspendCall {
        RetryUtil.retryWithBackoff(
            maxRetries = 2, // DataStore operations typically don't need many retries
            shouldRetry = { it is java.io.IOException }
        ) {
            dataStore.edit { preferences ->
                preferences[ONBOARDING_COMPLETED] = true
            }
        }
    }

    /**
     * Reset onboarding status (useful for testing or user preference)
     */
    suspend fun resetOnboardingStatus(): Result<Unit> = safeSuspendCall {
        RetryUtil.retryWithBackoff(
            maxRetries = 2,
            shouldRetry = { it is java.io.IOException }
        ) {
            dataStore.edit { preferences ->
                preferences[ONBOARDING_COMPLETED] = false
            }
        }
    }
}