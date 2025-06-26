package com.example.health_assistant.features.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.health_assistant.core.util.Result
import com.example.health_assistant.features.settings.data.SettingsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*

/**
 * ViewModel for Settings screen that manages all user preferences and settings.
 * Now handles Result states for proper error handling and loading indicators.
 */
class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {

    // User Profile Section - Now handling Result states
    val userName = repository.userName.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), Result.Loading
    )

    val userAge = repository.userAge.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), Result.Loading
    )

    val userGender = repository.userGender.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), Result.Loading
    )

    val userHealthGoal = repository.userHealthGoal.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), Result.Loading
    )

    val userHealthStatus = repository.userHealthStatus.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), Result.Loading
    )

    val avatarUri = repository.avatarUri.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), Result.Loading
    )

    // Combine age and gender with proper Result handling
    val userAgeGenderText = combine(userAge, userGender) { ageResult, genderResult ->
        when {
            ageResult is Result.Loading || genderResult is Result.Loading -> Result.Loading
            ageResult is Result.Error -> ageResult
            genderResult is Result.Error -> genderResult
            ageResult is Result.Success && genderResult is Result.Success -> {
                Result.Success("${ageResult.data} Years • ${genderResult.data}")
            }
            else -> Result.Error(message = "Unknown error combining age and gender")
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Result.Loading)

    // Health Preferences Section
    val stepGoal = repository.stepGoal.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), Result.Loading
    )

    val waterGoal = repository.waterGoal.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), Result.Loading
    )

    val sleepGoal = repository.sleepGoal.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), Result.Loading
    )

    // Personalization Settings
    val aiPersonalizationEnabled = repository.aiPersonalizationEnabled.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), Result.Loading
    )

    val appLanguage = repository.appLanguage.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), Result.Loading
    )

    val appTheme = repository.appTheme.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), Result.Loading
    )

    // Notification Settings
    val medicationRemindersEnabled = repository.medicationRemindersEnabled.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), Result.Loading
    )

    val wellnessCheckinsEnabled = repository.wellnessCheckinsEnabled.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), Result.Loading
    )

    val activityGoalsEnabled = repository.activityGoalsEnabled.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), Result.Loading
    )

    val waterRemindersEnabled = repository.waterRemindersEnabled.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), Result.Loading
    )

    val healthReportsEnabled = repository.healthReportsEnabled.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), Result.Loading
    )

    // App Settings
    val appRegion = repository.appRegion.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), Result.Loading
    )

    val dataSyncEnabled = repository.dataSyncEnabled.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), Result.Loading
    )

    val appLockEnabled = repository.appLockEnabled.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), Result.Loading
    )

    val biometricAuthEnabled = repository.biometricAuthEnabled.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), Result.Loading
    )

    val betaProgramEnabled = repository.betaProgramEnabled.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), Result.Loading
    )

    // Error state for update operations
    private val _updateError = MutableStateFlow<String?>(null)
    val updateError: StateFlow<String?> = _updateError.asStateFlow()

    // Loading state for update operations
    private val _isUpdating = MutableStateFlow(false)
    val isUpdating: StateFlow<Boolean> = _isUpdating.asStateFlow()

    // Cache management
    private val _cacheSize = MutableStateFlow("Calculating...")
    val cacheSize: StateFlow<String> = _cacheSize.asStateFlow()

    // Update methods with proper error handling
    fun updateUserName(name: String) {
        viewModelScope.launch {
            _isUpdating.value = true
            _updateError.value = null

            when (val result = repository.updateUserName(name)) {
                is Result.Error -> {
                    _updateError.value = result.message
                }
                is Result.Success -> {
                    // Success - StateFlow will automatically update
                }
                is Result.Loading -> {
                    // Handle loading state if needed
                }
            }

            _isUpdating.value = false
        }
    }

    fun updateUserAge(age: Int) {
        viewModelScope.launch {
            _isUpdating.value = true
            _updateError.value = null

            when (val result = repository.updateUserAge(age)) {
                is Result.Error -> {
                    _updateError.value = result.message
                }
                is Result.Success -> {
                    // Success handled automatically
                }
                is Result.Loading -> {
                    // Handle loading state if needed
                }
            }

            _isUpdating.value = false
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
        viewModelScope.launch {
            _isUpdating.value = true
            _updateError.value = null

            when (val result = repository.updateStepGoal(steps)) {
                is Result.Error -> {
                    _updateError.value = result.message
                }
                is Result.Success -> {
                    // Success handled automatically
                }
                is Result.Loading -> {
                    // Handle loading state if needed
                }
            }

            _isUpdating.value = false
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

    /**
     * Clear any error messages
     */
    fun clearError() {
        _updateError.value = null
    }


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