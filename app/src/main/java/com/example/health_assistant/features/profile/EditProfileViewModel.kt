package com.example.health_assistant.features.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.health_assistant.core.util.Result
import com.example.health_assistant.data.repository.interfaces.UserProfile
import com.example.health_assistant.data.repository.interfaces.UserProfileRepository
import com.example.health_assistant.features.profile.data.ProfileFieldMapper
import com.example.health_assistant.features.profile.data.ProfileImageManager
import com.example.health_assistant.features.profile.state.*
import com.example.health_assistant.features.profile.validation.ProfileValidationRules
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/**
 * Enhanced ViewModel for Edit Profile functionality with comprehensive state management
 * Provides reactive UI updates, real-time validation, and robust error handling
 */
@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val userProfileRepository: UserProfileRepository,
    private val profileImageManager: ProfileImageManager
) : ViewModel() {

    // Date formatters
    private val isoDateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayDateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    // Internal mutable state flows
    private val _uiState = MutableStateFlow<EditProfileUiState>(EditProfileUiState.Initial)
    private val _saveState = MutableStateFlow<SaveOperationState>(SaveOperationState.Idle)
    private val _photoUploadState = MutableStateFlow<PhotoUploadState>(PhotoUploadState.Idle)
    private val _formState = MutableStateFlow(FormState())
    private val _loadingState = MutableStateFlow(LoadingState())

    // Field validation states
    private val _displayNameValidation = MutableStateFlow<FieldValidationState>(FieldValidationState.Idle)
    private val _bioValidation = MutableStateFlow<FieldValidationState>(FieldValidationState.Idle)
    private val _birthdayValidation = MutableStateFlow<FieldValidationState>(FieldValidationState.Idle)
    private val _genderValidation = MutableStateFlow<FieldValidationState>(FieldValidationState.Idle)
    private val _heightValidation = MutableStateFlow<FieldValidationState>(FieldValidationState.Idle)
    private val _weightValidation = MutableStateFlow<FieldValidationState>(FieldValidationState.Idle)

    // Current form data
    private val _currentDisplayName = MutableStateFlow("")
    private val _currentBio = MutableStateFlow<String?>(null)
    private val _currentBirthday = MutableStateFlow<String?>(null)
    private val _currentGender = MutableStateFlow<String?>(null)
    private val _currentHeight = MutableStateFlow<String?>(null)
    private val _currentWeight = MutableStateFlow<String?>(null)
    private val _currentPhotoUrl = MutableStateFlow<String?>(null)

    // Original profile data for change detection
    private var originalProfile: ProfileData? = null

    // Public exposed state flows
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()
    val saveState: StateFlow<SaveOperationState> = _saveState.asStateFlow()
    val photoUploadState: StateFlow<PhotoUploadState> = _photoUploadState.asStateFlow()
    val formState: StateFlow<FormState> = _formState.asStateFlow()
    val loadingState: StateFlow<LoadingState> = _loadingState.asStateFlow()

    // Field validation states
    val displayNameValidation: StateFlow<FieldValidationState> = _displayNameValidation.asStateFlow()
    val bioValidation: StateFlow<FieldValidationState> = _bioValidation.asStateFlow()
    val birthdayValidation: StateFlow<FieldValidationState> = _birthdayValidation.asStateFlow()
    val genderValidation: StateFlow<FieldValidationState> = _genderValidation.asStateFlow()
    val heightValidation: StateFlow<FieldValidationState> = _heightValidation.asStateFlow()
    val weightValidation: StateFlow<FieldValidationState> = _weightValidation.asStateFlow()

    // Current form data
    val currentDisplayName: StateFlow<String> = _currentDisplayName.asStateFlow()
    val currentBio: StateFlow<String?> = _currentBio.asStateFlow()
    val currentBirthday: StateFlow<String?> = _currentBirthday.asStateFlow()
    val currentGender: StateFlow<String?> = _currentGender.asStateFlow()
    val currentHeight: StateFlow<String?> = _currentHeight.asStateFlow()
    val currentWeight: StateFlow<String?> = _currentWeight.asStateFlow()
    val currentPhotoUrl: StateFlow<String?> = _currentPhotoUrl.asStateFlow()

    init {
        // Monitor form changes for validation
        setupFormChangeMonitoring()
    }

    /**
     * Load current user profile with enhanced error handling
     */
    fun loadProfile() {
        viewModelScope.launch {
            _loadingState.value = _loadingState.value.copy(isLoadingProfile = true)
            _uiState.value = EditProfileUiState.Loading

            when (val result = userProfileRepository.getUserProfile()) {
                is Result.Success -> {
                    if (result.data != null) {
                        val profileData = ProfileFieldMapper.mapUserProfileToProfileData(result.data)
                        originalProfile = profileData

                        // Load local photo if available
                        val localPhotoPath = profileImageManager.getProfileImagePath()
                        val updatedProfileData = if (localPhotoPath != null) {
                            profileData.copy(photoUrl = localPhotoPath)
                        } else {
                            profileData
                        }

                        populateFormFields(updatedProfileData)
                        _uiState.value = EditProfileUiState.Success(updatedProfileData)
                    } else {
                        _uiState.value = EditProfileUiState.Error(
                            "Profile not found. Please try again.",
                            ErrorCause.UNKNOWN
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.value = EditProfileUiState.Error(
                        result.message ?: "Failed to load profile. Please check your connection.",
                        ErrorCause.NETWORK
                    )
                }
                is Result.Loading -> {
                    // Already handled in loading state
                }
            }

            _loadingState.value = _loadingState.value.copy(isLoadingProfile = false)
        }
    }

    /**
     * Update display name with real-time validation
     */
    fun updateDisplayName(displayName: String) {
        _currentDisplayName.value = displayName
        validateFieldRealTime(ProfileField.DISPLAY_NAME, displayName)
        updateFormState()
    }

    /**
     * Update bio with validation
     */
    fun updateBio(bio: String?) {
        _currentBio.value = bio
        validateFieldRealTime(ProfileField.BIO, bio ?: "")
        updateFormState()
    }

    /**
     * Update birthday with validation
     */
    fun updateBirthday(birthday: String?, isoFormat: String? = null) {
        _currentBirthday.value = isoFormat ?: birthday
        validateFieldRealTime(ProfileField.BIRTHDAY, birthday ?: "")
        updateFormState()
    }

    /**
     * Update gender with validation
     */
    fun updateGender(gender: String?) {
        _currentGender.value = gender
        validateFieldRealTime(ProfileField.GENDER, gender ?: "")
        updateFormState()
    }

    /**
     * Update height with validation
     */
    fun updateHeight(height: String?) {
        _currentHeight.value = height
        validateFieldRealTime(ProfileField.HEIGHT, height ?: "")
        updateFormState()
    }

    /**
     * Update weight with validation
     */
    fun updateWeight(weight: String?) {
        _currentWeight.value = weight
        validateFieldRealTime(ProfileField.WEIGHT, weight ?: "")
        updateFormState()
    }

    /**
     * Update profile photo with Room integration for persistence
     */
    fun updateProfilePhoto(uri: Uri) {
        viewModelScope.launch {
            _photoUploadState.value = PhotoUploadState.Uploading
            _loadingState.value = _loadingState.value.copy(isUploadingPhoto = true)

            try {
                val currentProfileState = _uiState.value
                if (currentProfileState !is EditProfileUiState.Success) {
                    _photoUploadState.value = PhotoUploadState.Error(
                        "Profile not loaded. Please try again.",
                        retryable = true
                    )
                    return@launch
                }

                // Save image using ProfileImageManager
                when (val result = profileImageManager.saveProfileImage(uri)) {
                    is ProfileImageManager.Result.Success -> {
                        _currentPhotoUrl.value = result.data
                        _photoUploadState.value = PhotoUploadState.Success(result.data)
                        updateFormState()
                    }
                    is ProfileImageManager.Result.Error -> {
                        _photoUploadState.value = PhotoUploadState.Error(
                            result.message,
                            retryable = true
                        )
                    }
                }
            } catch (e: Exception) {
                _photoUploadState.value = PhotoUploadState.Error(
                    "Failed to process image. Please try again.",
                    retryable = true
                )
            } finally {
                _loadingState.value = _loadingState.value.copy(isUploadingPhoto = false)
            }
        }
    }

    /**
     * Save profile with comprehensive validation and error handling
     */
    fun saveProfile() {
        viewModelScope.launch {
            _saveState.value = SaveOperationState.Saving
            _loadingState.value = _loadingState.value.copy(isSaving = true)

            // Final validation before save
            val validationErrors = ProfileValidationRules.validateAllFields(
                _currentDisplayName.value,
                _currentBirthday.value,
                _currentGender.value,
                _currentHeight.value,
                _currentWeight.value
            )

            if (validationErrors.isNotEmpty()) {
                _formState.value = _formState.value.copy(validationErrors = validationErrors)
                _saveState.value = SaveOperationState.Error(
                    "Please fix the errors before saving",
                    SaveErrorCause.VALIDATION,
                    retryable = false
                )
                _loadingState.value = _loadingState.value.copy(isSaving = false)
                return@launch
            }

            val currentProfileState = _uiState.value
            if (currentProfileState !is EditProfileUiState.Success) {
                _saveState.value = SaveOperationState.Error(
                    "Profile data not available. Please refresh and try again.",
                    SaveErrorCause.UNKNOWN
                )
                _loadingState.value = _loadingState.value.copy(isSaving = false)
                return@launch
            }

            // Create updated profile (do NOT update photoUrl here)
            val updatedProfile = currentProfileState.profile.copy(
                displayName = _currentDisplayName.value.trim(),
                bio = _currentBio.value?.trim(),
                birthday = _currentBirthday.value,
                gender = Gender.fromString(_currentGender.value),
                height = _currentHeight.value?.toFloatOrNull(),
                weight = _currentWeight.value?.toFloatOrNull(),
                // photoUrl is intentionally NOT updated here
                isProfileComplete = true
            )

            // Convert to UserProfile for repository
            val userProfile = mapProfileDataToUserProfile(updatedProfile)

            // Save to repository (Firestore)
            when (val result = userProfileRepository.updateUserProfileInFirestore(userProfile)) {
                is Result.Success -> {
                    // Update all fields in local storage for offline access
                    userProfileRepository.saveUserProfileLocally(userProfile)
                    originalProfile = updatedProfile
                    _saveState.value = SaveOperationState.Success
                    updateFormState() // Reset hasChanges flag
                }
                is Result.Error -> {
                    val errorCause = when {
                        result.message?.contains("network", ignoreCase = true) == true -> SaveErrorCause.NETWORK
                        result.message?.contains("auth", ignoreCase = true) == true -> SaveErrorCause.AUTHENTICATION
                        result.message?.contains("firestore", ignoreCase = true) == true -> SaveErrorCause.FIRESTORE
                        else -> SaveErrorCause.UNKNOWN
                    }
                    _saveState.value = SaveOperationState.Error(
                        result.message ?: "Failed to save profile. Please try again.",
                        errorCause
                    )
                }
                is Result.Loading -> {
                    // Already handled in saving state
                }
            }

            _loadingState.value = _loadingState.value.copy(isSaving = false)
        }
    }

    /**
     * Reset save state to idle
     */
    fun resetSaveState() {
        _saveState.value = SaveOperationState.Idle
    }

    /**
     * Check if form has unsaved changes
     */
    fun hasUnsavedChanges(): Boolean {
        val original = originalProfile ?: return false

        return _currentDisplayName.value != original.displayName ||
               _currentBio.value != original.bio ||
               _currentBirthday.value != original.birthday ||
               _currentGender.value != original.gender?.name ||
               _currentHeight.value != original.height?.toString() ||
               _currentWeight.value != original.weight?.toString() ||
               _currentPhotoUrl.value != original.photoUrl
    }

    /**
     * Setup monitoring of form fields for real-time validation
     */
    private fun setupFormChangeMonitoring() {
        // Monitor each field individually to avoid complex combine issues
        viewModelScope.launch {
            _currentDisplayName.collect { displayName ->
                validateFieldRealTime(ProfileField.DISPLAY_NAME, displayName)
            }
        }

        viewModelScope.launch {
            _currentBio.collect { bio ->
                validateFieldRealTime(ProfileField.BIO, bio ?: "")
            }
        }

        viewModelScope.launch {
            _currentBirthday.collect { birthday ->
                validateFieldRealTime(ProfileField.BIRTHDAY, birthday ?: "")
            }
        }

        viewModelScope.launch {
            _currentGender.collect { gender ->
                validateFieldRealTime(ProfileField.GENDER, gender ?: "")
            }
        }

        viewModelScope.launch {
            _currentHeight.collect { height ->
                validateFieldRealTime(ProfileField.HEIGHT, height ?: "")
            }
        }

        viewModelScope.launch {
            _currentWeight.collect { weight ->
                validateFieldRealTime(ProfileField.WEIGHT, weight ?: "")
            }
        }
    }

    /**
     * Validate a specific field in real-time
     */
    private fun validateFieldRealTime(field: ProfileField, value: String) {
        val validationState = when (field) {
            ProfileField.DISPLAY_NAME -> {
                if (value.isBlank()) {
                    FieldValidationState.Error("Display name is required")
                } else if (value.length < 2) {
                    FieldValidationState.Error("Display name must be at least 2 characters")
                } else if (value.length > 50) {
                    FieldValidationState.Error("Display name must be less than 50 characters")
                } else {
                    FieldValidationState.Valid
                }
            }
            ProfileField.BIO -> {
                if (value.isBlank()) {
                    FieldValidationState.Valid // Optional field, no error if blank
                } else if (value.length > 160) {
                    FieldValidationState.Error("Bio must be less than 160 characters")
                } else {
                    FieldValidationState.Valid
                }
            }
            ProfileField.BIRTHDAY -> {
                if (value.isBlank()) {
                    FieldValidationState.Valid // Optional field, no error if blank
                } else if (!isValidDate(value)) {
                    FieldValidationState.Error("Invalid date format")
                } else if (!isReasonableAge(value)) {
                    FieldValidationState.Error("Please enter a valid birth date")
                } else {
                    FieldValidationState.Valid
                }
            }
            ProfileField.GENDER -> {
                if (value.isBlank()) {
                    FieldValidationState.Error("Gender is required")
                } else {
                    FieldValidationState.Valid
                }
            }
            ProfileField.HEIGHT -> {
                if (value.isBlank()) {
                    FieldValidationState.Valid // Optional field, no error if blank
                } else {
                    val heightValue = value.toFloatOrNull()
                    if (heightValue == null || heightValue < 50f || heightValue > 300f) {
                        FieldValidationState.Error("Height must be between 50 and 300 cm")
                    } else {
                        FieldValidationState.Valid
                    }
                }
            }
            ProfileField.WEIGHT -> {
                if (value.isBlank()) {
                    FieldValidationState.Valid // Optional field, no error if blank
                } else {
                    val weightValue = value.toFloatOrNull()
                    if (weightValue == null || weightValue < 20f || weightValue > 500f) {
                        FieldValidationState.Error("Weight must be between 20 and 500 kg")
                    } else {
                        FieldValidationState.Valid
                    }
                }
            }
            ProfileField.PHOTO -> {
                // Photo validation is handled separately in updateProfilePhoto
                FieldValidationState.Valid
            }
        }

        // Update the corresponding validation state flow
        when (field) {
            ProfileField.DISPLAY_NAME -> _displayNameValidation.value = validationState
            ProfileField.BIO -> _bioValidation.value = validationState
            ProfileField.BIRTHDAY -> _birthdayValidation.value = validationState
            ProfileField.GENDER -> _genderValidation.value = validationState
            ProfileField.HEIGHT -> _heightValidation.value = validationState
            ProfileField.WEIGHT -> _weightValidation.value = validationState
            ProfileField.PHOTO -> {
                // Photo validation state is handled in photoUploadState
            }
        }
    }

    /**
     * Update form state based on current values and validation results
     */
    private fun updateFormState() {
        _formState.value = _formState.value.copy(
            hasChanges = hasUnsavedChanges(),
            isValid = true, // Will be updated based on validation results
            isDirty = hasUnsavedChanges(),
            validationErrors = emptyMap() // Reset errors on valid change
        )
    }

    /**
     * Map UserProfile to ProfileData for internal representation
     */
    private fun mapUserProfileToProfileData(userProfile: UserProfile): ProfileData {
        return ProfileData(
            userId = userProfile.userId,
            email = userProfile.email,
            displayName = userProfile.displayName ?: "",
            bio = userProfile.bio, // Add bio field mapping
            birthday = userProfile.birthday,
            gender = Gender.fromString(userProfile.gender),
            height = userProfile.height,
            weight = userProfile.weight,
            photoUrl = userProfile.photoUrl,
            isProfileComplete = userProfile.isProfileComplete
        )
    }

    /**
     * Map ProfileData to UserProfile for repository operations
     */
    private fun mapProfileDataToUserProfile(profileData: ProfileData): UserProfile {
        return UserProfile(
            userId = profileData.userId,
            email = profileData.email,
            displayName = profileData.displayName.takeIf { it.isNotBlank() },
            bio = profileData.bio, // Add bio field mapping
            birthday = profileData.birthday,
            gender = profileData.gender?.name,
            height = profileData.height,
            weight = profileData.weight,
            photoUrl = profileData.photoUrl,
            isProfileComplete = profileData.isProfileComplete
        )
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
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            val age = currentYear - birthYear

            age in 5..120
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Populate form fields with profile data
     */
    private fun populateFormFields(profileData: ProfileData) {
        _currentDisplayName.value = profileData.displayName
        _currentBio.value = profileData.bio
        _currentBirthday.value = profileData.birthday
        _currentGender.value = profileData.gender?.name
        _currentHeight.value = profileData.height?.toString()
        _currentWeight.value = profileData.weight?.toString()
        _currentPhotoUrl.value = profileData.photoUrl
    }

    /**
     * Get the current user ID from the repository (suspend function)
     */
    suspend fun getCurrentUserId(): String? {
        return try {
            userProfileRepository.getUserId().firstOrNull()?.let {
                if (it is Result.Success) it.data else null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Force sync the user profile from Firestore/remote and update UI state
     */
    fun syncProfileFromRemote(userId: String) {
        viewModelScope.launch {
            _loadingState.value = _loadingState.value.copy(isLoadingProfile = true)
            _uiState.value = EditProfileUiState.Loading
            when (val result = userProfileRepository.syncUserProfileFromFirestore(userId)) {
                is Result.Success -> {
                    if (result.data != null) {
                        val profileData = ProfileFieldMapper.mapUserProfileToProfileData(result.data)
                        originalProfile = profileData
                        populateFormFields(profileData)
                        _uiState.value = EditProfileUiState.Success(profileData)
                    } else {
                        _uiState.value = EditProfileUiState.Error(
                            "Profile not found in remote storage.",
                            ErrorCause.UNKNOWN
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.value = EditProfileUiState.Error(
                        result.message ?: "Failed to sync profile from remote.",
                        ErrorCause.NETWORK
                    )
                }
                is Result.Loading -> {
                    // Optionally handle loading state if needed
                }
            }
            _loadingState.value = _loadingState.value.copy(isLoadingProfile = false)
        }
    }

    /**
     * Sync profile from Firestore and update local storage when Edit Profile is opened
     * This should be called from the fragment's onViewCreated before loading the profile.
     */
    fun syncProfileOnEditOpen() {
        viewModelScope.launch {
            val userId = getCurrentUserId()
            if (!userId.isNullOrBlank()) {
                when (val result = userProfileRepository.syncUserProfileFromFirestore(userId)) {
                    is Result.Success -> {
                        result.data?.let {
                            userProfileRepository.saveUserProfileLocally(it)
                        }
                    }
                    is Result.Error -> {
                        // Optionally log or handle error
                    }
                    is Result.Loading -> {
                        // Optionally handle loading state
                    }
                }
            }
        }
    }
}