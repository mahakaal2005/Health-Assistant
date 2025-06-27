package com.example.health_assistant.features.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.health_assistant.core.util.Result
import com.example.health_assistant.data.repository.interfaces.UserProfile
import com.example.health_assistant.data.repository.interfaces.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Edit Profile functionality
 * Handles profile loading, validation, and saving with proper error handling
 */
@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val userProfileRepository: UserProfileRepository
) : ViewModel() {

    // Profile loading state
    private val _profileState = MutableStateFlow<ProfileState>(ProfileState.Loading)
    val profileState: StateFlow<ProfileState> = _profileState.asStateFlow()

    // Save operation state
    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState: StateFlow<SaveState> = _saveState.asStateFlow()

    // Validation errors
    private val _validationErrors = MutableStateFlow<Map<String, String>>(emptyMap())
    val validationErrors: StateFlow<Map<String, String>> = _validationErrors.asStateFlow()

    // Current profile data
    private var currentProfile: UserProfile? = null

    // Photo URL state
    private val _currentPhotoUrl = MutableStateFlow<String?>(null)
    val currentPhotoUrl: StateFlow<String?> = _currentPhotoUrl.asStateFlow()

    /**
     * Load current user profile
     */
    fun loadProfile() {
        viewModelScope.launch {
            _profileState.value = ProfileState.Loading

            when (val result = userProfileRepository.getUserProfile()) {
                is Result.Success -> {
                    if (result.data != null) {
                        currentProfile = result.data
                        _currentPhotoUrl.value = result.data.photoUrl
                        _profileState.value = ProfileState.Success(result.data)
                    } else {
                        _profileState.value = ProfileState.Error("Profile not found")
                    }
                }
                is Result.Error -> {
                    _profileState.value = ProfileState.Error(result.message ?: "Failed to load profile")
                }
                is Result.Loading -> {
                    _profileState.value = ProfileState.Loading
                }
            }
        }
    }

    /**
     * Update profile photo URI
     * For now stores as string, will be enhanced with Room integration
     */
    fun updateProfilePhoto(uri: Uri) {
        _currentPhotoUrl.value = uri.toString()
    }

    /**
     * Save profile with validation
     */
    fun saveProfile(
        displayName: String,
        birthday: String?,
        gender: String?,
        height: Float?,
        weight: Float?
    ) {
        // Clear previous validation errors
        _validationErrors.value = emptyMap()

        // Validate input
        val errors = validateInput(displayName, birthday, gender, height, weight)
        if (errors.isNotEmpty()) {
            _validationErrors.value = errors
            return
        }

        // Save if validation passes
        viewModelScope.launch {
            _saveState.value = SaveState.Saving

            val profile = currentProfile
            if (profile == null) {
                _saveState.value = SaveState.Error("Profile not loaded")
                return@launch
            }

            // Create updated profile
            val updatedProfile = profile.copy(
                displayName = displayName.takeIf { it.isNotBlank() },
                birthday = birthday,
                gender = gender,
                height = height,
                weight = weight,
                photoUrl = _currentPhotoUrl.value,
                isProfileComplete = true // Mark as complete when user saves
            )

            // Save to repository
            when (val result = userProfileRepository.updateUserProfileInFirestore(updatedProfile)) {
                is Result.Success -> {
                    // Also update local storage
                    userProfileRepository.saveUserProfile(profile.userId, profile.email)
                    _saveState.value = SaveState.Success
                }
                is Result.Error -> {
                    _saveState.value = SaveState.Error(result.message ?: "Failed to save profile")
                }
                is Result.Loading -> {
                    // Already in saving state
                }
            }
        }
    }

    /**
     * Validate user input with comprehensive rules
     */
    private fun validateInput(
        displayName: String,
        birthday: String?,
        gender: String?,
        height: Float?,
        weight: Float?
    ): Map<String, String> {
        val errors = mutableMapOf<String, String>()

        // Display name validation
        if (displayName.isBlank()) {
            errors["displayName"] = "Display name is required"
        } else if (displayName.length < 2) {
            errors["displayName"] = "Display name must be at least 2 characters"
        } else if (displayName.length > 50) {
            errors["displayName"] = "Display name must be less than 50 characters"
        }

        // Birthday validation (optional but if provided must be valid)
        birthday?.let { birthdayStr ->
            if (!isValidDate(birthdayStr)) {
                errors["birthday"] = "Invalid date format"
            } else if (!isReasonableAge(birthdayStr)) {
                errors["birthday"] = "Please enter a valid birth date"
            }
        }

        // Height validation (optional but if provided must be reasonable)
        height?.let { h ->
            if (h < 50f || h > 300f) {
                errors["height"] = "Height must be between 50 and 300 cm"
            }
        }

        // Weight validation (optional but if provided must be reasonable)
        weight?.let { w ->
            if (w < 20f || w > 500f) {
                errors["weight"] = "Weight must be between 20 and 500 kg"
            }
        }

        return errors
    }

    /**
     * Validate date format (YYYY-MM-DD)
     */
    private fun isValidDate(dateString: String): Boolean {
        return try {
            val parts = dateString.split("-")
            if (parts.size != 3) return false

            val year = parts[0].toInt()
            val month = parts[1].toInt()
            val day = parts[2].toInt()

            year in 1900..2024 && month in 1..12 && day in 1..31
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if age is reasonable (between 5 and 120 years old)
     */
    private fun isReasonableAge(dateString: String): Boolean {
        return try {
            val parts = dateString.split("-")
            val birthYear = parts[0].toInt()
            val currentYear = java.time.Year.now().value
            val age = currentYear - birthYear

            age in 5..120
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Reset save state to idle
     */
    fun resetSaveState() {
        _saveState.value = SaveState.Idle
    }

    /**
     * Sealed classes for state management
     */
    sealed class ProfileState {
        object Loading : ProfileState()
        data class Success(val profile: UserProfile) : ProfileState()
        data class Error(val message: String) : ProfileState()
    }

    sealed class SaveState {
        object Idle : SaveState()
        object Saving : SaveState()
        object Success : SaveState()
        data class Error(val message: String) : SaveState()
    }
}