package com.example.health_assistant.features.profile.validation

import com.example.health_assistant.features.profile.state.Gender
import com.example.health_assistant.features.profile.state.ProfileField
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Centralized validation rules for Edit Profile with detailed error messages
 * Follows modern Android validation patterns with accessibility-friendly messages
 */
object ProfileValidationRules {

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    /**
     * Validation result container
     */
    data class ValidationResult(
        val isValid: Boolean,
        val errorMessage: String? = null,
        val field: ProfileField? = null
    ) {
        companion object {
            fun valid() = ValidationResult(true)
            fun invalid(message: String, field: ProfileField? = null) =
                ValidationResult(false, message, field)
        }
    }

    /**
     * Comprehensive form validation
     */
    fun validateAllFields(
        displayName: String,
        birthday: String?,
        gender: String?,
        height: String?,
        weight: String?
    ): Map<ProfileField, String> {
        val errors = mutableMapOf<ProfileField, String>()

        // Validate display name (required)
        validateDisplayName(displayName).let { result ->
            if (!result.isValid) {
                errors[ProfileField.DISPLAY_NAME] = result.errorMessage!!
            }
        }

        // Validate birthday (optional but must be valid if provided)
        if (!birthday.isNullOrBlank()) {
            validateBirthday(birthday).let { result ->
                if (!result.isValid) {
                    errors[ProfileField.BIRTHDAY] = result.errorMessage!!
                }
            }
        }

        // Validate gender (optional but must be valid if provided)
        if (!gender.isNullOrBlank()) {
            validateGender(gender).let { result ->
                if (!result.isValid) {
                    errors[ProfileField.GENDER] = result.errorMessage!!
                }
            }
        }

        // Validate height (optional but must be valid if provided)
        if (!height.isNullOrBlank()) {
            validateHeight(height).let { result ->
                if (!result.isValid) {
                    errors[ProfileField.HEIGHT] = result.errorMessage!!
                }
            }
        }

        // Validate weight (optional but must be valid if provided)
        if (!weight.isNullOrBlank()) {
            validateWeight(weight).let { result ->
                if (!result.isValid) {
                    errors[ProfileField.WEIGHT] = result.errorMessage!!
                }
            }
        }

        return errors
    }

    /**
     * Display name validation with detailed feedback
     */
    fun validateDisplayName(displayName: String): ValidationResult {
        return when {
            displayName.isBlank() ->
                ValidationResult.invalid("Display name is required", ProfileField.DISPLAY_NAME)

            displayName.length < 2 ->
                ValidationResult.invalid("Display name must be at least 2 characters", ProfileField.DISPLAY_NAME)

            displayName.length > 50 ->
                ValidationResult.invalid("Display name cannot exceed 50 characters", ProfileField.DISPLAY_NAME)

            !displayName.matches(Regex("^[a-zA-Z0-9\\s._-]+$")) ->
                ValidationResult.invalid("Display name contains invalid characters", ProfileField.DISPLAY_NAME)

            displayName.trim() != displayName ->
                ValidationResult.invalid("Display name cannot start or end with spaces", ProfileField.DISPLAY_NAME)

            else -> ValidationResult.valid()
        }
    }

    /**
     * Birthday validation with age restrictions
     */
    fun validateBirthday(birthday: String): ValidationResult {
        return try {
            // Check format
            if (!birthday.matches(Regex("^\\d{4}-\\d{2}-\\d{2}$"))) {
                return ValidationResult.invalid("Invalid date format. Use YYYY-MM-DD", ProfileField.BIRTHDAY)
            }

            val date = LocalDate.parse(birthday, DateTimeFormatter.ISO_LOCAL_DATE)
            val today = LocalDate.now()
            val age = Period.between(date, today).years

            when {
                date.isAfter(today) ->
                    ValidationResult.invalid("Birthday cannot be in the future", ProfileField.BIRTHDAY)

                age < 5 ->
                    ValidationResult.invalid("Age must be at least 5 years", ProfileField.BIRTHDAY)

                age > 120 ->
                    ValidationResult.invalid("Please enter a valid birth date", ProfileField.BIRTHDAY)

                else -> ValidationResult.valid()
            }
        } catch (e: Exception) {
            ValidationResult.invalid("Invalid date format", ProfileField.BIRTHDAY)
        }
    }

    /**
     * Gender validation
     */
    fun validateGender(gender: String): ValidationResult {
        return if (Gender.fromString(gender) != null) {
            ValidationResult.valid()
        } else {
            ValidationResult.invalid("Please select a valid gender option", ProfileField.GENDER)
        }
    }

    /**
     * Height validation with realistic ranges
     */
    fun validateHeight(height: String): ValidationResult {
        return try {
            val heightValue = height.toFloat()
            when {
                heightValue < 50f ->
                    ValidationResult.invalid("Height must be at least 50 cm", ProfileField.HEIGHT)

                heightValue > 300f ->
                    ValidationResult.invalid("Height cannot exceed 300 cm", ProfileField.HEIGHT)

                !isValidNumber(height) ->
                    ValidationResult.invalid("Please enter a valid height", ProfileField.HEIGHT)

                else -> ValidationResult.valid()
            }
        } catch (e: NumberFormatException) {
            ValidationResult.invalid("Please enter a valid number for height", ProfileField.HEIGHT)
        }
    }

    /**
     * Weight validation with realistic ranges
     */
    fun validateWeight(weight: String): ValidationResult {
        return try {
            val weightValue = weight.toFloat()
            when {
                weightValue < 20f ->
                    ValidationResult.invalid("Weight must be at least 20 kg", ProfileField.WEIGHT)

                weightValue > 500f ->
                    ValidationResult.invalid("Weight cannot exceed 500 kg", ProfileField.WEIGHT)

                !isValidNumber(weight) ->
                    ValidationResult.invalid("Please enter a valid weight", ProfileField.WEIGHT)

                else -> ValidationResult.valid()
            }
        } catch (e: NumberFormatException) {
            ValidationResult.invalid("Please enter a valid number for weight", ProfileField.WEIGHT)
        }
    }

    /**
     * Real-time validation for individual fields
     */
    fun validateField(field: ProfileField, value: String): ValidationResult {
        return when (field) {
            ProfileField.DISPLAY_NAME -> validateDisplayName(value)
            ProfileField.BIRTHDAY -> if (value.isBlank()) ValidationResult.valid() else validateBirthday(value)
            ProfileField.GENDER -> if (value.isBlank()) ValidationResult.valid() else validateGender(value)
            ProfileField.HEIGHT -> if (value.isBlank()) ValidationResult.valid() else validateHeight(value)
            ProfileField.WEIGHT -> if (value.isBlank()) ValidationResult.valid() else validateWeight(value)
            ProfileField.PHOTO -> ValidationResult.valid() // Photo validation handled separately
        }
    }

    /**
     * Check if form is complete (all required fields filled and valid)
     */
    fun isFormComplete(
        displayName: String,
        birthday: String?,
        gender: String?,
        height: String?,
        weight: String?
    ): Boolean {
        // Only display name is required, others are optional
        val displayNameValid = validateDisplayName(displayName).isValid
        val birthdayValid = birthday.isNullOrBlank() || validateBirthday(birthday).isValid
        val genderValid = gender.isNullOrBlank() || validateGender(gender).isValid
        val heightValid = height.isNullOrBlank() || validateHeight(height).isValid
        val weightValid = weight.isNullOrBlank() || validateWeight(weight).isValid

        return displayNameValid && birthdayValid && genderValid && heightValid && weightValid
    }

    /**
     * Get user-friendly validation summary
     */
    fun getValidationSummary(errors: Map<ProfileField, String>): String {
        return when {
            errors.isEmpty() -> "All fields are valid"
            errors.size == 1 -> "Please fix the ${errors.keys.first().name.lowercase()} field"
            else -> "Please fix ${errors.size} fields with errors"
        }
    }

    /**
     * Helper function to validate number format
     */
    private fun isValidNumber(value: String): Boolean {
        return try {
            val number = value.toFloat()
            number.isFinite() && !number.isNaN()
        } catch (e: NumberFormatException) {
            false
        }
    }

    /**
     * Get accessibility-friendly field names
     */
    fun getFieldDisplayName(field: ProfileField): String {
        return when (field) {
            ProfileField.DISPLAY_NAME -> "Display Name"
            ProfileField.BIRTHDAY -> "Birthday"
            ProfileField.GENDER -> "Gender"
            ProfileField.HEIGHT -> "Height"
            ProfileField.WEIGHT -> "Weight"
            ProfileField.PHOTO -> "Profile Photo"
        }
    }
}