package com.example.health_assistant.features.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.health_assistant.features.settings.data.SettingsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*

/**
 * ViewModel for Settings screen that manages all user preferences and settings.
 * Exposes StateFlow properties for reactive UI updates.
 */
class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {

    // User Profile Section
    val userName = repository.userName.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), "Alex Johnson"
    )

    val userAge = repository.userAge.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 28
    )

    val userGender = repository.userGender.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), "Male"
    )

    val userHealthGoal = repository.userHealthGoal.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), "Focus on cardio and weight loss"
    )

    val userHealthStatus = repository.userHealthStatus.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), "Excellent"
    )

    val avatarUri = repository.avatarUri.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), null
    )

    // Format the user display string for age and gender
    val userAgeGenderText = combine(userAge, userGender) { age, gender ->
        "$age Years • $gender"
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "28 Years • Male")

    // Health Preferences Section
    val stepGoal = repository.stepGoal.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 10000
    )

    val waterGoal = repository.waterGoal.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 2.5f
    )

    val sleepGoal = repository.sleepGoal.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 8.0f
    )

    // Formatted text for UI display
    val stepGoalText = stepGoal.map { steps ->
        NumberFormat.getNumberInstance(Locale.getDefault()).format(steps) + " steps"
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "10,000 steps")

    val waterGoalText = waterGoal.map { liters ->
        String.format("%.1f liters", liters)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "2.5 liters")

    val sleepGoalText = sleepGoal.map { hours ->
        String.format("%.1f hours", hours)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "8.0 hours")

    // Personalization
    val aiPersonalizationEnabled = repository.aiPersonalizationEnabled.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), true
    )

    val appLanguage = repository.appLanguage.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), "English (US)"
    )

    val appTheme = repository.appTheme.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), "Light"
    )

    // Notification Settings
    val medicationRemindersEnabled = repository.medicationRemindersEnabled.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), true
    )

    val wellnessCheckinsEnabled = repository.wellnessCheckinsEnabled.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), true
    )

    val activityGoalsEnabled = repository.activityGoalsEnabled.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), true
    )

    val waterRemindersEnabled = repository.waterRemindersEnabled.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), true
    )

    val healthReportsEnabled = repository.healthReportsEnabled.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), true
    )

    // App Settings
    val appRegion = repository.appRegion.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), "United States"
    )

    val dataSyncEnabled = repository.dataSyncEnabled.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), true
    )

    val appLockEnabled = repository.appLockEnabled.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), false
    )

    val biometricAuthEnabled = repository.biometricAuthEnabled.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), true
    )

    // Beta Program
    val betaProgramEnabled = repository.betaProgramEnabled.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), false
    )

    // Cache Size
    private val _cacheSize = MutableStateFlow("0 MB")
    val cacheSize: StateFlow<String> = _cacheSize

    // App Lock Status Text
    val appLockStatusText = appLockEnabled.map { enabled ->
        if (enabled) "PIN protection enabled" else "Set up PIN protection"
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Set up PIN protection")

    init {
        updateCacheSize()
    }

    // User Profile Actions
    fun updateUserName(name: String) {
        if (name.isNotBlank()) {
            viewModelScope.launch {
                repository.updateUserName(name)
            }
        }
    }

    fun updateUserAge(age: Int) {
        if (age > 0) {
            viewModelScope.launch {
                repository.updateUserAge(age)
            }
        }
    }

    fun updateUserGender(gender: String) {
        if (gender.isNotBlank()) {
            viewModelScope.launch {
                repository.updateUserGender(gender)
            }
        }
    }

    fun updateUserHealthGoal(goal: String) {
        if (goal.isNotBlank()) {
            viewModelScope.launch {
                repository.updateUserHealthGoal(goal)
            }
        }
    }

    fun updateUserHealthStatus(status: String) {
        if (status.isNotBlank()) {
            viewModelScope.launch {
                repository.updateUserHealthStatus(status)
            }
        }
    }

    fun updateAvatarUri(uri: Uri?) {
        uri?.let {
            viewModelScope.launch {
                repository.updateAvatarUri(it)
            }
        }
    }

    // Health Preferences Actions
    fun updateStepGoal(steps: Int) {
        if (steps >= 1000) {
            viewModelScope.launch {
                repository.updateStepGoal(steps)
            }
        }
    }

    fun updateWaterGoal(liters: Float) {
        if (liters >= 0.5f) {
            viewModelScope.launch {
                repository.updateWaterGoal(liters)
            }
        }
    }

    fun updateSleepGoal(hours: Float) {
        if (hours >= 4.0f) {
            viewModelScope.launch {
                repository.updateSleepGoal(hours)
            }
        }
    }

    // Personalization Actions
    fun toggleAiPersonalization(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateAiPersonalization(enabled)
        }
    }

    fun updateAppLanguage(language: String) {
        if (language.isNotBlank()) {
            viewModelScope.launch {
                repository.updateAppLanguage(language)
            }
        }
    }

    fun updateAppTheme(theme: String) {
        if (theme.isNotBlank()) {
            viewModelScope.launch {
                repository.updateAppTheme(theme)
            }
        }
    }

    // Notification Actions
    fun toggleMedicationReminders(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateMedicationReminders(enabled)
        }
    }

    fun toggleWellnessCheckins(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateWellnessCheckins(enabled)
        }
    }

    fun toggleActivityGoals(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateActivityGoals(enabled)
        }
    }

    fun toggleWaterReminders(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateWaterReminders(enabled)
        }
    }

    fun toggleHealthReports(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateHealthReports(enabled)
        }
    }

    // App Settings Actions
    fun updateAppRegion(region: String) {
        if (region.isNotBlank()) {
            viewModelScope.launch {
                repository.updateAppRegion(region)
            }
        }
    }

    fun toggleDataSync(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateDataSync(enabled)
        }
    }

    fun setAppLockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateAppLock(enabled)
        }
    }

    fun toggleBiometricAuth(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateBiometricAuth(enabled)
        }
    }

    // Beta Program Actions
    fun toggleBetaProgram(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateBetaProgram(enabled)
        }
    }

    // Cache Actions
    fun updateCacheSize() {
        viewModelScope.launch {
            val cacheSizeBytes = repository.getAppCacheSize()
            _cacheSize.value = formatCacheSize(cacheSizeBytes)
        }
    }

    fun clearCache(): Boolean {
        val success = repository.clearAppCache()
        if (success) {
            updateCacheSize()
        }
        return success
    }

    private fun formatCacheSize(size: Long): String {
        val kb = size / 1024.0
        val mb = kb / 1024.0

        return when {
            mb >= 1.0 -> String.format("%.1f MB", mb)
            kb >= 1.0 -> String.format("%.1f KB", kb)
            else -> String.format("%d B", size)
        }
    }

    // ViewModel Factory for dependency injection
    class Factory(private val repository: SettingsRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
                return SettingsViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}