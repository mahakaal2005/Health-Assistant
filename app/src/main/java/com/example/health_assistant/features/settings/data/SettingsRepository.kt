package com.example.health_assistant.features.settings.data

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.health_assistant.core.util.Result
import com.example.health_assistant.core.util.RetryUtil
import com.example.health_assistant.core.util.safeSuspendCall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import java.io.File

// Create a DataStore instance at the module level
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Repository for managing user settings data persistence with robust error handling
 * All operations return Result<T> to handle loading, success, and error states
 */
class SettingsRepository(private val context: Context) {

    // Keys for DataStore
    private object PreferencesKeys {
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_AGE = intPreferencesKey("user_age")
        val USER_GENDER = stringPreferencesKey("user_gender")
        val USER_HEALTH_GOAL = stringPreferencesKey("user_health_goal")
        val USER_HEALTH_STATUS = stringPreferencesKey("user_health_status")
        val AVATAR_PATH = stringPreferencesKey("avatar_path")

        val STEP_GOAL = intPreferencesKey("step_goal")
        val WATER_GOAL = floatPreferencesKey("water_goal")
        val SLEEP_GOAL = floatPreferencesKey("sleep_goal")

        val AI_PERSONALIZATION = booleanPreferencesKey("ai_personalization")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val APP_THEME = stringPreferencesKey("app_theme")

        val NOTIFICATION_MEDICATION = booleanPreferencesKey("notification_medication")
        val NOTIFICATION_WELLNESS = booleanPreferencesKey("notification_wellness")
        val NOTIFICATION_ACTIVITY = booleanPreferencesKey("notification_activity")
        val NOTIFICATION_WATER = booleanPreferencesKey("notification_water")
        val NOTIFICATION_REPORTS = booleanPreferencesKey("notification_reports")

        val APP_REGION = stringPreferencesKey("app_region")
        val DATA_SYNC = booleanPreferencesKey("data_sync")
        val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        val BIOMETRIC_AUTH = booleanPreferencesKey("biometric_auth")

        val BETA_PROGRAM = booleanPreferencesKey("beta_program")
    }

    // User Profile Data with error handling
    val userName: Flow<Result<String>> = flow {
        try {
            context.dataStore.data.collect { preferences ->
                emit(Result.Success(preferences[PreferencesKeys.USER_NAME] ?: "Alex Johnson"))
            }
        } catch (exception: Exception) {
            emit(Result.Error(exception, "Failed to load user name") as Result<String>)
        }
    }

    val userAge: Flow<Result<Int>> = flow {
        try {
            context.dataStore.data.collect { preferences ->
                emit(Result.Success(preferences[PreferencesKeys.USER_AGE] ?: 28))
            }
        } catch (exception: Exception) {
            emit(Result.Error(exception, "Failed to load user age") as Result<Int>)
        }
    }

    val userGender: Flow<Result<String>> = flow {
        try {
            context.dataStore.data.collect { preferences ->
                emit(Result.Success(preferences[PreferencesKeys.USER_GENDER] ?: "Male"))
            }
        } catch (exception: Exception) {
            emit(Result.Error(exception, "Failed to load user gender") as Result<String>)
        }
    }

    val userHealthGoal: Flow<Result<String>> = flow {
        try {
            context.dataStore.data.collect { preferences ->
                emit(Result.Success(preferences[PreferencesKeys.USER_HEALTH_GOAL] ?: "Focus on cardio and weight loss"))
            }
        } catch (exception: Exception) {
            emit(Result.Error(exception, "Failed to load health goal") as Result<String>)
        }
    }

    val userHealthStatus: Flow<Result<String>> = flow {
        try {
            context.dataStore.data.collect { preferences ->
                emit(Result.Success(preferences[PreferencesKeys.USER_HEALTH_STATUS] ?: "Excellent"))
            }
        } catch (exception: Exception) {
            emit(Result.Error(exception, "Failed to load health status") as Result<String>)
        }
    }

    val avatarUri: Flow<Result<Uri?>> = flow {
        try {
            context.dataStore.data.collect { preferences ->
                val path = preferences[PreferencesKeys.AVATAR_PATH]
                val uri = if (path != null) Uri.parse(path) else null
                emit(Result.Success(uri))
            }
        } catch (exception: Exception) {
            emit(Result.Error(exception, "Failed to load avatar") as Result<Uri?>)
        }
    }

    // Health Preferences Data with error handling
    val stepGoal: Flow<Result<Int>> = flow {
        try {
            context.dataStore.data.collect { preferences ->
                emit(Result.Success(preferences[PreferencesKeys.STEP_GOAL] ?: 10000))
            }
        } catch (exception: Exception) {
            emit(Result.Error(exception, "Failed to load step goal") as Result<Int>)
        }
    }

    val waterGoal: Flow<Result<Float>> = flow {
        try {
            context.dataStore.data.collect { preferences ->
                emit(Result.Success(preferences[PreferencesKeys.WATER_GOAL] ?: 2.5f))
            }
        } catch (exception: Exception) {
            emit(Result.Error(exception, "Failed to load water goal") as Result<Float>)
        }
    }

    val sleepGoal: Flow<Result<Float>> = flow {
        try {
            context.dataStore.data.collect { preferences ->
                emit(Result.Success(preferences[PreferencesKeys.SLEEP_GOAL] ?: 8.0f))
            }
        } catch (exception: Exception) {
            emit(Result.Error(exception, "Failed to load sleep goal") as Result<Float>)
        }
    }

    // Personalization Data with error handling
    val aiPersonalizationEnabled: Flow<Result<Boolean>> = flow {
        try {
            context.dataStore.data.collect { preferences ->
                emit(Result.Success(preferences[PreferencesKeys.AI_PERSONALIZATION] ?: true))
            }
        } catch (exception: Exception) {
            emit(Result.Error(exception, "Failed to load AI personalization setting") as Result<Boolean>)
        }
    }

    val appLanguage: Flow<Result<String>> = flow {
        try {
            context.dataStore.data.collect { preferences ->
                emit(Result.Success(preferences[PreferencesKeys.APP_LANGUAGE] ?: "English (US)"))
            }
        } catch (exception: Exception) {
            emit(Result.Error(exception, "Failed to load app language") as Result<String>)
        }
    }

    val appTheme: Flow<Result<String>> = flow {
        try {
            context.dataStore.data.collect { preferences ->
                emit(Result.Success(preferences[PreferencesKeys.APP_THEME] ?: "Light"))
            }
        } catch (exception: Exception) {
            emit(Result.Error(exception, "Failed to load app theme") as Result<String>)
        }
    }

    // Notification Settings with error handling
    val medicationRemindersEnabled: Flow<Result<Boolean>> = flow {
        try {
            context.dataStore.data.collect { preferences ->
                emit(Result.Success(preferences[PreferencesKeys.NOTIFICATION_MEDICATION] ?: true))
            }
        } catch (exception: Exception) {
            emit(Result.Error(exception, "Failed to load medication reminders setting") as Result<Boolean>)
        }
    }

    val wellnessCheckinsEnabled: Flow<Result<Boolean>> = flow {
        try {
            context.dataStore.data.collect { preferences ->
                emit(Result.Success(preferences[PreferencesKeys.NOTIFICATION_WELLNESS] ?: true))
            }
        } catch (exception: Exception) {
            emit(Result.Error(exception, "Failed to load wellness check-ins setting") as Result<Boolean>)
        }
    }

    val activityGoalsEnabled: Flow<Result<Boolean>> = flow {
        try {
            context.dataStore.data.collect { preferences ->
                emit(Result.Success(preferences[PreferencesKeys.NOTIFICATION_ACTIVITY] ?: true))
            }
        } catch (exception: Exception) {
            emit(Result.Error(exception, "Failed to load activity goals setting") as Result<Boolean>)
        }
    }

    val waterRemindersEnabled: Flow<Result<Boolean>> = flow {
        try {
            context.dataStore.data.collect { preferences ->
                emit(Result.Success(preferences[PreferencesKeys.NOTIFICATION_WATER] ?: true))
            }
        } catch (exception: Exception) {
            emit(Result.Error(exception, "Failed to load water reminders setting") as Result<Boolean>)
        }
    }

    val healthReportsEnabled: Flow<Result<Boolean>> = flow {
        try {
            context.dataStore.data.collect { preferences ->
                emit(Result.Success(preferences[PreferencesKeys.NOTIFICATION_REPORTS] ?: true))
            }
        } catch (exception: Exception) {
            emit(Result.Error(exception, "Failed to load health reports setting") as Result<Boolean>)
        }
    }

    // App Settings with error handling
    val appRegion: Flow<Result<String>> = flow {
        try {
            context.dataStore.data.collect { preferences ->
                emit(Result.Success(preferences[PreferencesKeys.APP_REGION] ?: "United States"))
            }
        } catch (exception: Exception) {
            emit(Result.Error(exception, "Failed to load app region") as Result<String>)
        }
    }

    val dataSyncEnabled: Flow<Result<Boolean>> = flow {
        try {
            context.dataStore.data.collect { preferences ->
                emit(Result.Success(preferences[PreferencesKeys.DATA_SYNC] ?: true))
            }
        } catch (exception: Exception) {
            emit(Result.Error(exception, "Failed to load data sync setting") as Result<Boolean>)
        }
    }

    val appLockEnabled: Flow<Result<Boolean>> = flow {
        try {
            context.dataStore.data.collect { preferences ->
                emit(Result.Success(preferences[PreferencesKeys.APP_LOCK_ENABLED] ?: false))
            }
        } catch (exception: Exception) {
            emit(Result.Error(exception, "Failed to load app lock setting") as Result<Boolean>)
        }
    }

    val biometricAuthEnabled: Flow<Result<Boolean>> = flow {
        try {
            context.dataStore.data.collect { preferences ->
                emit(Result.Success(preferences[PreferencesKeys.BIOMETRIC_AUTH] ?: true))
            }
        } catch (exception: Exception) {
            emit(Result.Error(exception, "Failed to load biometric auth setting") as Result<Boolean>)
        }
    }

    // Beta Program with error handling
    val betaProgramEnabled: Flow<Result<Boolean>> = flow {
        try {
            context.dataStore.data.collect { preferences ->
                emit(Result.Success(preferences[PreferencesKeys.BETA_PROGRAM] ?: false))
            }
        } catch (exception: Exception) {
            emit(Result.Error(exception, "Failed to load beta program setting") as Result<Boolean>)
        }
    }

    // Update User Profile with retry logic and error handling
    suspend fun updateUserName(name: String): Result<Unit> = safeSuspendCall {
        RetryUtil.retryWithBackoff(
            maxRetries = 2, // DataStore operations typically don't need many retries
            shouldRetry = { it is java.io.IOException }
        ) {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.USER_NAME] = name
            }
        }
    }

    suspend fun updateUserAge(age: Int): Result<Unit> = safeSuspendCall {
        RetryUtil.retryWithBackoff(
            maxRetries = 2,
            shouldRetry = { it is java.io.IOException }
        ) {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.USER_AGE] = age
            }
        }
    }

    suspend fun updateUserGender(gender: String): Result<Unit> = safeSuspendCall {
        RetryUtil.retryWithBackoff(
            maxRetries = 2,
            shouldRetry = { it is java.io.IOException }
        ) {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.USER_GENDER] = gender
            }
        }
    }

    suspend fun updateUserHealthGoal(goal: String): Result<Unit> = safeSuspendCall {
        RetryUtil.retryWithBackoff(
            maxRetries = 2,
            shouldRetry = { it is java.io.IOException }
        ) {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.USER_HEALTH_GOAL] = goal
            }
        }
    }

    suspend fun updateUserHealthStatus(status: String): Result<Unit> = safeSuspendCall {
        RetryUtil.retryWithBackoff(
            maxRetries = 2,
            shouldRetry = { it is java.io.IOException }
        ) {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.USER_HEALTH_STATUS] = status
            }
        }
    }

    suspend fun updateAvatarUri(uri: Uri): Result<Unit> = safeSuspendCall {
        RetryUtil.retryWithBackoff(
            maxRetries = 2,
            shouldRetry = { it is java.io.IOException }
        ) {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.AVATAR_PATH] = uri.toString()
            }
        }
    }

    // Update Health Preferences with error handling
    suspend fun updateStepGoal(steps: Int): Result<Unit> = safeSuspendCall {
        RetryUtil.retryWithBackoff(
            maxRetries = 2,
            shouldRetry = { it is java.io.IOException }
        ) {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.STEP_GOAL] = steps
            }
        }
    }

    suspend fun updateWaterGoal(liters: Float): Result<Unit> = safeSuspendCall {
        RetryUtil.retryWithBackoff(
            maxRetries = 2,
            shouldRetry = { it is java.io.IOException }
        ) {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.WATER_GOAL] = liters
            }
        }
    }

    suspend fun updateSleepGoal(hours: Float): Result<Unit> = safeSuspendCall {
        RetryUtil.retryWithBackoff(
            maxRetries = 2,
            shouldRetry = { it is java.io.IOException }
        ) {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.SLEEP_GOAL] = hours
            }
        }
    }

    // Update Personalization with error handling
    suspend fun updateAiPersonalization(enabled: Boolean): Result<Unit> = safeSuspendCall {
        RetryUtil.retryWithBackoff(
            maxRetries = 2,
            shouldRetry = { it is java.io.IOException }
        ) {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.AI_PERSONALIZATION] = enabled
            }
        }
    }

    suspend fun updateAppLanguage(language: String): Result<Unit> = safeSuspendCall {
        RetryUtil.retryWithBackoff(
            maxRetries = 2,
            shouldRetry = { it is java.io.IOException }
        ) {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.APP_LANGUAGE] = language
            }
        }
    }

    suspend fun updateAppTheme(theme: String): Result<Unit> = safeSuspendCall {
        RetryUtil.retryWithBackoff(
            maxRetries = 2,
            shouldRetry = { it is java.io.IOException }
        ) {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.APP_THEME] = theme
            }
        }
    }

    // Update Notifications with error handling
    suspend fun updateMedicationReminders(enabled: Boolean): Result<Unit> = safeSuspendCall {
        RetryUtil.retryWithBackoff(
            maxRetries = 2,
            shouldRetry = { it is java.io.IOException }
        ) {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.NOTIFICATION_MEDICATION] = enabled
            }
        }
    }

    suspend fun updateWellnessCheckins(enabled: Boolean): Result<Unit> = safeSuspendCall {
        RetryUtil.retryWithBackoff(
            maxRetries = 2,
            shouldRetry = { it is java.io.IOException }
        ) {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.NOTIFICATION_WELLNESS] = enabled
            }
        }
    }

    suspend fun updateActivityGoals(enabled: Boolean): Result<Unit> = safeSuspendCall {
        RetryUtil.retryWithBackoff(
            maxRetries = 2,
            shouldRetry = { it is java.io.IOException }
        ) {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.NOTIFICATION_ACTIVITY] = enabled
            }
        }
    }

    suspend fun updateWaterReminders(enabled: Boolean): Result<Unit> = safeSuspendCall {
        RetryUtil.retryWithBackoff(
            maxRetries = 2,
            shouldRetry = { it is java.io.IOException }
        ) {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.NOTIFICATION_WATER] = enabled
            }
        }
    }

    suspend fun updateHealthReports(enabled: Boolean): Result<Unit> = safeSuspendCall {
        RetryUtil.retryWithBackoff(
            maxRetries = 2,
            shouldRetry = { it is java.io.IOException }
        ) {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.NOTIFICATION_REPORTS] = enabled
            }
        }
    }

    // Update App Settings with error handling
    suspend fun updateAppRegion(region: String): Result<Unit> = safeSuspendCall {
        RetryUtil.retryWithBackoff(
            maxRetries = 2,
            shouldRetry = { it is java.io.IOException }
        ) {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.APP_REGION] = region
            }
        }
    }

    suspend fun updateDataSync(enabled: Boolean): Result<Unit> = safeSuspendCall {
        RetryUtil.retryWithBackoff(
            maxRetries = 2,
            shouldRetry = { it is java.io.IOException }
        ) {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.DATA_SYNC] = enabled
            }
        }
    }

    suspend fun updateAppLock(enabled: Boolean): Result<Unit> = safeSuspendCall {
        RetryUtil.retryWithBackoff(
            maxRetries = 2,
            shouldRetry = { it is java.io.IOException }
        ) {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.APP_LOCK_ENABLED] = enabled
            }
        }
    }

    suspend fun updateBiometricAuth(enabled: Boolean): Result<Unit> = safeSuspendCall {
        RetryUtil.retryWithBackoff(
            maxRetries = 2,
            shouldRetry = { it is java.io.IOException }
        ) {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.BIOMETRIC_AUTH] = enabled
            }
        }
    }

    // Update Beta Program with error handling
    suspend fun updateBetaProgram(enabled: Boolean): Result<Unit> = safeSuspendCall {
        RetryUtil.retryWithBackoff(
            maxRetries = 2,
            shouldRetry = { it is java.io.IOException }
        ) {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.BETA_PROGRAM] = enabled
            }
        }
    }

    // Helper method to calculate the app's cache size
    fun getAppCacheSize(): Long {
        var size: Long = 0
        try {
            size += getDirSize(context.cacheDir)
            val externalCacheDir = context.externalCacheDir
            if (externalCacheDir != null) {
                size += getDirSize(externalCacheDir)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return size
    }

    // Helper to clear app cache
    fun clearAppCache(): Boolean {
        try {
            val cacheDir = context.cacheDir
            deleteDir(cacheDir)

            val externalCacheDir = context.externalCacheDir
            if (externalCacheDir != null) {
                deleteDir(externalCacheDir)
            }

            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    private fun getDirSize(dir: File): Long {
        var size: Long = 0

        val files = dir.listFiles()
        if (files != null) {
            for (file in files) {
                if (file.isFile) {
                    size += file.length()
                } else {
                    size += getDirSize(file)
                }
            }
        }

        return size
    }

    private fun deleteDir(dir: File): Boolean {
        val files = dir.listFiles()
        if (files != null) {
            for (file in files) {
                if (file.isDirectory) {
                    deleteDir(file)
                } else {
                    file.delete()
                }
            }
        }
        return true
    }

    /**
     * Bulk update method for updating multiple settings atomically
     * Useful for reducing DataStore write operations and improving performance
     */
    suspend fun updateMultipleSettings(updates: (MutablePreferences) -> Unit): Result<Unit> = safeSuspendCall {
        RetryUtil.retryWithBackoff(
            maxRetries = 2,
            shouldRetry = { it is java.io.IOException }
        ) {
            context.dataStore.edit(updates)
        }
    }
}