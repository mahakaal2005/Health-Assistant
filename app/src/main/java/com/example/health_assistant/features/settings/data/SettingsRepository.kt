package com.example.health_assistant.features.settings.data

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.*
import java.io.File

// Create a DataStore instance at the module level
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Repository for managing user settings data persistence
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

    // User Profile Data
    val userName: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.USER_NAME] ?: "Alex Johnson"
        }

    val userAge: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.USER_AGE] ?: 28
        }

    val userGender: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.USER_GENDER] ?: "Male"
        }

    val userHealthGoal: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.USER_HEALTH_GOAL] ?: "Focus on cardio and weight loss"
        }

    val userHealthStatus: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.USER_HEALTH_STATUS] ?: "Excellent"
        }

    val avatarUri: Flow<Uri?> = context.dataStore.data
        .map { preferences ->
            val path = preferences[PreferencesKeys.AVATAR_PATH]
            if (path != null) Uri.parse(path) else null
        }

    // Health Preferences Data
    val stepGoal: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.STEP_GOAL] ?: 10000
        }

    val waterGoal: Flow<Float> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.WATER_GOAL] ?: 2.5f
        }

    val sleepGoal: Flow<Float> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.SLEEP_GOAL] ?: 8.0f
        }

    // Personalization Data
    val aiPersonalizationEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.AI_PERSONALIZATION] ?: true
        }

    val appLanguage: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.APP_LANGUAGE] ?: "English (US)"
        }

    val appTheme: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.APP_THEME] ?: "Light"
        }

    // Notification Settings
    val medicationRemindersEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.NOTIFICATION_MEDICATION] ?: true
        }

    val wellnessCheckinsEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.NOTIFICATION_WELLNESS] ?: true
        }

    val activityGoalsEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.NOTIFICATION_ACTIVITY] ?: true
        }

    val waterRemindersEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.NOTIFICATION_WATER] ?: true
        }

    val healthReportsEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.NOTIFICATION_REPORTS] ?: true
        }

    // App Settings
    val appRegion: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.APP_REGION] ?: "United States"
        }

    val dataSyncEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.DATA_SYNC] ?: true
        }

    val appLockEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.APP_LOCK_ENABLED] ?: false
        }

    val biometricAuthEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.BIOMETRIC_AUTH] ?: true
        }

    // Beta Program
    val betaProgramEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.BETA_PROGRAM] ?: false
        }

    // Update User Profile
    suspend fun updateUserName(name: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.USER_NAME] = name
        }
    }

    suspend fun updateUserAge(age: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.USER_AGE] = age
        }
    }

    suspend fun updateUserGender(gender: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.USER_GENDER] = gender
        }
    }

    suspend fun updateUserHealthGoal(goal: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.USER_HEALTH_GOAL] = goal
        }
    }

    suspend fun updateUserHealthStatus(status: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.USER_HEALTH_STATUS] = status
        }
    }

    suspend fun updateAvatarUri(uri: Uri) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AVATAR_PATH] = uri.toString()
        }
    }

    // Update Health Preferences
    suspend fun updateStepGoal(steps: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.STEP_GOAL] = steps
        }
    }

    suspend fun updateWaterGoal(liters: Float) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.WATER_GOAL] = liters
        }
    }

    suspend fun updateSleepGoal(hours: Float) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SLEEP_GOAL] = hours
        }
    }

    // Update Personalization
    suspend fun updateAiPersonalization(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AI_PERSONALIZATION] = enabled
        }
    }

    suspend fun updateAppLanguage(language: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.APP_LANGUAGE] = language
        }
    }

    suspend fun updateAppTheme(theme: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.APP_THEME] = theme
        }
    }

    // Update Notifications
    suspend fun updateMedicationReminders(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATION_MEDICATION] = enabled
        }
    }

    suspend fun updateWellnessCheckins(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATION_WELLNESS] = enabled
        }
    }

    suspend fun updateActivityGoals(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATION_ACTIVITY] = enabled
        }
    }

    suspend fun updateWaterReminders(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATION_WATER] = enabled
        }
    }

    suspend fun updateHealthReports(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATION_REPORTS] = enabled
        }
    }

    // Update App Settings
    suspend fun updateAppRegion(region: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.APP_REGION] = region
        }
    }

    suspend fun updateDataSync(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DATA_SYNC] = enabled
        }
    }

    suspend fun updateAppLock(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.APP_LOCK_ENABLED] = enabled
        }
    }

    suspend fun updateBiometricAuth(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.BIOMETRIC_AUTH] = enabled
        }
    }

    // Update Beta Program
    suspend fun updateBetaProgram(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.BETA_PROGRAM] = enabled
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
}