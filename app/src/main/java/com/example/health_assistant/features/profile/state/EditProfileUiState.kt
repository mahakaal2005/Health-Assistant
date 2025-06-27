package com.example.health_assistant.features.profile.state

/**
 * Comprehensive UI state management for Edit Profile Feature
 * Covers all possible states with detailed information for proper UI feedback
 */

/**
 * Main UI state for the Edit Profile screen
 */
sealed class EditProfileUiState {
    object Loading : EditProfileUiState()
    data class Success(val profile: ProfileData) : EditProfileUiState()
    data class Error(val message: String, val cause: ErrorCause = ErrorCause.UNKNOWN) : EditProfileUiState()
    object Initial : EditProfileUiState()
}

/**
 * Save operation state with detailed feedback
 */
sealed class SaveOperationState {
    object Idle : SaveOperationState()
    object Saving : SaveOperationState()
    object Success : SaveOperationState()
    data class Error(
        val message: String,
        val cause: SaveErrorCause = SaveErrorCause.UNKNOWN,
        val retryable: Boolean = true
    ) : SaveOperationState()
}

/**
 * Field validation state for real-time feedback
 */
sealed class FieldValidationState {
    object Valid : FieldValidationState()
    object Idle : FieldValidationState()
    data class Invalid(val errorMessage: String) : FieldValidationState()
    data class Error(val errorMessage: String) : FieldValidationState()
    object Validating : FieldValidationState()
}

/**
 * Photo upload state for profile image handling
 */
sealed class PhotoUploadState {
    object Idle : PhotoUploadState()
    object Uploading : PhotoUploadState()
    data class Success(val photoUrl: String) : PhotoUploadState()
    data class Error(val message: String, val retryable: Boolean = true) : PhotoUploadState()
}

/**
 * Form state tracking for better UX
 */
data class FormState(
    val hasChanges: Boolean = false,
    val isValid: Boolean = false,
    val isDirty: Boolean = false,
    val validationErrors: Map<ProfileField, String> = emptyMap()
)

/**
 * Profile data container with all fields
 */
data class ProfileData(
    val userId: String,
    val email: String,
    val displayName: String = "",
    val photoUrl: String? = null,
    val birthday: String? = null, // ISO format YYYY-MM-DD
    val gender: Gender? = null,
    val height: Float? = null, // in cm
    val weight: Float? = null, // in kg
    val isProfileComplete: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Profile fields enum for validation mapping
 */
enum class ProfileField {
    DISPLAY_NAME,
    BIRTHDAY,
    GENDER,
    HEIGHT,
    WEIGHT,
    PHOTO
}

/**
 * Gender options with proper handling
 */
enum class Gender(val displayName: String) {
    MALE("Male"),
    FEMALE("Female"),
    OTHER("Other"),
    PREFER_NOT_TO_SAY("Prefer not to say");

    companion object {
        fun fromString(value: String?): Gender? {
            return values().find { it.displayName.equals(value, ignoreCase = true) }
        }

        fun getAllOptions(): List<String> {
            return values().map { it.displayName }
        }
    }
}

/**
 * Error cause categories for better error handling
 */
enum class ErrorCause {
    NETWORK,
    AUTHENTICATION,
    PERMISSION,
    VALIDATION,
    UNKNOWN
}

/**
 * Save operation error categories
 */
enum class SaveErrorCause {
    NETWORK,
    VALIDATION,
    AUTHENTICATION,
    FIRESTORE,
    IMAGE_UPLOAD,
    UNKNOWN
}

/**
 * Loading state for different operations
 */
data class LoadingState(
    val isLoadingProfile: Boolean = false,
    val isSaving: Boolean = false,
    val isUploadingPhoto: Boolean = false,
    val isValidating: Boolean = false
)