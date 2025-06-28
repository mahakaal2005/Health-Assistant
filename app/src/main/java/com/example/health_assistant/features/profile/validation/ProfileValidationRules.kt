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

        // Validate birthday (optional)
        birthday?.let { birthdayValue ->
            if (birthdayValue.isNotBlank()) {
                validateBirthday(birthdayValue).let { result ->
                    if (!result.isValid) {
                        errors[ProfileField.BIRTHDAY] = result.errorMessage!!
                    }
                }
            }
        }

        // Validate gender (optional but recommended)
        gender?.let { genderValue ->
            if (genderValue.isNotBlank()) {
                validateGender(genderValue).let { result ->
                    if (!result.isValid) {
                        errors[ProfileField.GENDER] = result.errorMessage!!
                    }
                }
            }
        }

        // Validate height (optional)
        height?.let { heightValue ->
            if (heightValue.isNotBlank()) {
                validateHeight(heightValue).let { result ->
                    if (!result.isValid) {
                        errors[ProfileField.HEIGHT] = result.errorMessage!!
                    }
                }
            }
        }

        // Validate weight (optional)
        weight?.let { weightValue ->
            if (weightValue.isNotBlank()) {
                validateWeight(weightValue).let { result ->
                    if (!result.isValid) {
                        errors[ProfileField.WEIGHT] = result.errorMessage!!
                    }
                }
            }
        }

        return errors
    }

    /**
     * Validate display name
     */
    fun validateDisplayName(displayName: String): ValidationResult {
        return when {
            displayName.isBlank() -> ValidationResult.invalid(
                "Display name is required",
                ProfileField.DISPLAY_NAME
            )
            displayName.length < 2 -> ValidationResult.invalid(
                "Display name must be at least 2 characters",
                ProfileField.DISPLAY_NAME
            )
            displayName.length > 50 -> ValidationResult.invalid(
                "Display name must be less than 50 characters",
                ProfileField.DISPLAY_NAME
            )
            !displayName.matches(Regex("^[a-zA-Z0-9\\s._-]+$")) -> ValidationResult.invalid(
                "Display name contains invalid characters",
                ProfileField.DISPLAY_NAME
            )
            else -> ValidationResult.valid()
        }
    }

    /**
     * Validate bio
     */
    fun validateBio(bio: String): ValidationResult {
        return when {
            bio.length > 160 -> ValidationResult.invalid(
                "Bio must be less than 160 characters",
                ProfileField.BIO
            )
            else -> ValidationResult.valid()
        }
    }

    /**
     * Validate birthday
     */
    fun validateBirthday(birthday: String): ValidationResult {
        if (birthday.isBlank()) return ValidationResult.valid()

        return try {
            // Validate format (YYYY-MM-DD)
            val parts = birthday.split("-")
            if (parts.size != 3) {
                return ValidationResult.invalid(
                    "Invalid date format. Use YYYY-MM-DD",
                    ProfileField.BIRTHDAY
                )
            }

            val year = parts[0].toInt()
            val month = parts[1].toInt()
            val day = parts[2].toInt()

            // Basic range checks
            if (year < 1900 || year > Calendar.getInstance().get(Calendar.YEAR)) {
                return ValidationResult.invalid(
                    "Please enter a valid birth year",
                    ProfileField.BIRTHDAY
                )
            }

            if (month < 1 || month > 12) {
                return ValidationResult.invalid(
                    "Please enter a valid month (1-12)",
                    ProfileField.BIRTHDAY
                )
            }

            if (day < 1 || day > 31) {
                return ValidationResult.invalid(
                    "Please enter a valid day",
                    ProfileField.BIRTHDAY
                )
            }

            // Check if date is reasonable (age between 5 and 120)
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            val age = currentYear - year

            if (age < 5) {
                return ValidationResult.invalid(
                    "Age must be at least 5 years",
                    ProfileField.BIRTHDAY
                )
            }

            if (age > 120) {
                return ValidationResult.invalid(
                    "Please enter a valid birth date",
                    ProfileField.BIRTHDAY
                )
            }

            ValidationResult.valid()
        } catch (e: Exception) {
            ValidationResult.invalid(
                "Invalid date format",
                ProfileField.BIRTHDAY
            )
        }
    }

    /**
     * Validate gender
     */
    fun validateGender(gender: String): ValidationResult {
        return when {
            gender.isBlank() -> ValidationResult.invalid(
                "Gender is required",
                ProfileField.GENDER
            )
            Gender.fromString(gender) == null -> ValidationResult.invalid(
                "Please select a valid gender option",
                ProfileField.GENDER
            )
            else -> ValidationResult.valid()
        }
    }

    /**
     * Validate height
     */
    fun validateHeight(height: String): ValidationResult {
        if (height.isBlank()) return ValidationResult.valid()

        return try {
            val heightValue = height.toFloat()
            when {
                heightValue < 50f -> ValidationResult.invalid(
                    "Height must be at least 50 cm",
                    ProfileField.HEIGHT
                )
                heightValue > 300f -> ValidationResult.invalid(
                    "Height must be less than 300 cm",
                    ProfileField.HEIGHT
                )
                else -> ValidationResult.valid()
            }
        } catch (e: NumberFormatException) {
            ValidationResult.invalid(
                "Please enter a valid height in cm",
                ProfileField.HEIGHT
            )
        }
    }

    /**
     * Validate weight
     */
    fun validateWeight(weight: String): ValidationResult {
        if (weight.isBlank()) return ValidationResult.valid()

        return try {
            val weightValue = weight.toFloat()
            when {
                weightValue < 20f -> ValidationResult.invalid(
                    "Weight must be at least 20 kg",
                    ProfileField.WEIGHT
                )
                weightValue > 500f -> ValidationResult.invalid(
                    "Weight must be less than 500 kg",
                    ProfileField.WEIGHT
                )
                else -> ValidationResult.valid()
            }
        } catch (e: NumberFormatException) {
            ValidationResult.invalid(
                "Please enter a valid weight in kg",
                ProfileField.WEIGHT
            )
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
     * Get user-friendly error message for a specific field
     */
    fun getFieldErrorMessage(field: ProfileField, value: String): String? {
        return when (field) {
            ProfileField.DISPLAY_NAME -> validateDisplayName(value).errorMessage
            ProfileField.BIO -> validateBio(value).errorMessage
            ProfileField.BIRTHDAY -> validateBirthday(value).errorMessage
            ProfileField.GENDER -> validateGender(value).errorMessage
            ProfileField.HEIGHT -> validateHeight(value).errorMessage
            ProfileField.WEIGHT -> validateWeight(value).errorMessage
            ProfileField.PHOTO -> null // Photo validation handled separately
        }
    }

    /**
     * Check if a specific field is valid
     */
    fun isFieldValid(field: ProfileField, value: String): Boolean {
        return when (field) {
            ProfileField.DISPLAY_NAME -> validateDisplayName(value).isValid
            ProfileField.BIO -> validateBio(value).isValid
            ProfileField.BIRTHDAY -> validateBirthday(value).isValid
            ProfileField.GENDER -> validateGender(value).isValid
            ProfileField.HEIGHT -> validateHeight(value).isValid
            ProfileField.WEIGHT -> validateWeight(value).isValid
            ProfileField.PHOTO -> true // Photo validation handled separately
        }
    }
}