package com.example.health_assistant.core.design.components

import android.content.Context
import androidx.core.content.ContextCompat
import com.example.health_assistant.R
import com.example.health_assistant.core.design.tokens.HealthColors
import com.google.android.material.textfield.TextInputLayout

/**
 * Health Form Validation Utility
 * 
 * Consistent validation styling and patterns for form components.
 * Provides standardized error message styling and validation feedback.
 * 
 * Features:
 * - Consistent error styling using HealthColors tokens
 * - Standardized validation patterns for common input types
 * - Accessibility support with proper error announcements
 * - Integration with HealthTextInputLayout components
 * 
 * Usage:
 * ```
 * val validation = HealthFormValidation(context)
 * validation.validateRequired(textInputLayout, "Field is required")
 * validation.validateEmail(emailInputLayout)
 * validation.clearErrors(textInputLayout)
 * ```
 */
class HealthFormValidation(private val context: Context) {

    /**
     * Validation result data class
     */
    data class ValidationResult(
        val isValid: Boolean,
        val errorMessage: String? = null
    )

    /**
     * Validate required field
     */
    fun validateRequired(
        inputLayout: TextInputLayout,
        errorMessage: String = "This field is required"
    ): ValidationResult {
        val text = inputLayout.editText?.text?.toString()?.trim()
        
        return if (text.isNullOrEmpty()) {
            setError(inputLayout, errorMessage)
            ValidationResult(false, errorMessage)
        } else {
            clearError(inputLayout)
            ValidationResult(true)
        }
    }

    /**
     * Validate email format
     */
    fun validateEmail(
        inputLayout: TextInputLayout,
        errorMessage: String = "Please enter a valid email address"
    ): ValidationResult {
        val email = inputLayout.editText?.text?.toString()?.trim()
        
        return if (email.isNullOrEmpty()) {
            setError(inputLayout, "Email is required")
            ValidationResult(false, "Email is required")
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            setError(inputLayout, errorMessage)
            ValidationResult(false, errorMessage)
        } else {
            clearError(inputLayout)
            ValidationResult(true)
        }
    }

    /**
     * Validate minimum length
     */
    fun validateMinLength(
        inputLayout: TextInputLayout,
        minLength: Int,
        errorMessage: String? = null
    ): ValidationResult {
        val text = inputLayout.editText?.text?.toString()?.trim()
        val defaultMessage = "Must be at least $minLength characters"
        val message = errorMessage ?: defaultMessage
        
        return if (text.isNullOrEmpty()) {
            setError(inputLayout, "This field is required")
            ValidationResult(false, "This field is required")
        } else if (text.length < minLength) {
            setError(inputLayout, message)
            ValidationResult(false, message)
        } else {
            clearError(inputLayout)
            ValidationResult(true)
        }
    }

    /**
     * Validate maximum length
     */
    fun validateMaxLength(
        inputLayout: TextInputLayout,
        maxLength: Int,
        errorMessage: String? = null
    ): ValidationResult {
        val text = inputLayout.editText?.text?.toString()?.trim()
        val defaultMessage = "Must be no more than $maxLength characters"
        val message = errorMessage ?: defaultMessage
        
        return if (!text.isNullOrEmpty() && text.length > maxLength) {
            setError(inputLayout, message)
            ValidationResult(false, message)
        } else {
            clearError(inputLayout)
            ValidationResult(true)
        }
    }

    /**
     * Validate password strength
     */
    fun validatePassword(
        inputLayout: TextInputLayout,
        minLength: Int = 8,
        requireSpecialChar: Boolean = true,
        requireNumber: Boolean = true,
        requireUppercase: Boolean = true
    ): ValidationResult {
        val password = inputLayout.editText?.text?.toString()
        
        if (password.isNullOrEmpty()) {
            setError(inputLayout, "Password is required")
            return ValidationResult(false, "Password is required")
        }
        
        if (password.length < minLength) {
            setError(inputLayout, "Password must be at least $minLength characters")
            return ValidationResult(false, "Password must be at least $minLength characters")
        }
        
        if (requireUppercase && !password.any { it.isUpperCase() }) {
            setError(inputLayout, "Password must contain at least one uppercase letter")
            return ValidationResult(false, "Password must contain at least one uppercase letter")
        }
        
        if (requireNumber && !password.any { it.isDigit() }) {
            setError(inputLayout, "Password must contain at least one number")
            return ValidationResult(false, "Password must contain at least one number")
        }
        
        if (requireSpecialChar && !password.any { !it.isLetterOrDigit() }) {
            setError(inputLayout, "Password must contain at least one special character")
            return ValidationResult(false, "Password must contain at least one special character")
        }
        
        clearError(inputLayout)
        return ValidationResult(true)
    }

    /**
     * Validate that two fields match (e.g., password confirmation)
     */
    fun validateFieldsMatch(
        inputLayout1: TextInputLayout,
        inputLayout2: TextInputLayout,
        errorMessage: String = "Fields do not match"
    ): ValidationResult {
        val text1 = inputLayout1.editText?.text?.toString()
        val text2 = inputLayout2.editText?.text?.toString()
        
        return if (text1 != text2) {
            setError(inputLayout2, errorMessage)
            ValidationResult(false, errorMessage)
        } else {
            clearError(inputLayout2)
            ValidationResult(true)
        }
    }

    /**
     * Validate phone number format
     */
    fun validatePhoneNumber(
        inputLayout: TextInputLayout,
        errorMessage: String = "Please enter a valid phone number"
    ): ValidationResult {
        val phone = inputLayout.editText?.text?.toString()?.trim()
        
        return if (phone.isNullOrEmpty()) {
            setError(inputLayout, "Phone number is required")
            ValidationResult(false, "Phone number is required")
        } else if (!android.util.Patterns.PHONE.matcher(phone).matches()) {
            setError(inputLayout, errorMessage)
            ValidationResult(false, errorMessage)
        } else {
            clearError(inputLayout)
            ValidationResult(true)
        }
    }

    /**
     * Custom validation with provided validator function
     */
    fun validateCustom(
        inputLayout: TextInputLayout,
        validator: (String) -> Boolean,
        errorMessage: String
    ): ValidationResult {
        val text = inputLayout.editText?.text?.toString()?.trim() ?: ""
        
        return if (!validator(text)) {
            setError(inputLayout, errorMessage)
            ValidationResult(false, errorMessage)
        } else {
            clearError(inputLayout)
            ValidationResult(true)
        }
    }

    /**
     * Set error with consistent styling
     */
    fun setError(inputLayout: TextInputLayout, errorMessage: String) {
        inputLayout.error = errorMessage
        applyErrorStyling(inputLayout)
        announceErrorForAccessibility(inputLayout, errorMessage)
    }

    /**
     * Clear error and reset styling
     */
    fun clearError(inputLayout: TextInputLayout) {
        inputLayout.error = null
        inputLayout.isErrorEnabled = false
        resetInputStyling(inputLayout)
    }

    /**
     * Apply consistent error styling to input layout
     */
    private fun applyErrorStyling(inputLayout: TextInputLayout) {
        inputLayout.boxStrokeColor = ContextCompat.getColor(context, HealthColors.Semantic.error)
        inputLayout.setErrorTextColor(ContextCompat.getColorStateList(context, HealthColors.Semantic.error))
        inputLayout.setErrorIconTintList(ContextCompat.getColorStateList(context, HealthColors.Semantic.error))
    }

    /**
     * Reset input styling based on input type
     */
    private fun resetInputStyling(inputLayout: TextInputLayout) {
        when (inputLayout) {
            is HealthTextInputLayout -> {
                // Let the HealthTextInputLayout handle its own styling reset
                inputLayout.setInputStyle(inputLayout.getInputStyle())
            }
            else -> {
                // Reset to default stroke color for standard TextInputLayout
                inputLayout.boxStrokeColor = ContextCompat.getColor(context, HealthColors.Interactive.outline)
            }
        }
    }

    /**
     * Announce error for accessibility
     */
    private fun announceErrorForAccessibility(inputLayout: TextInputLayout, errorMessage: String) {
        inputLayout.announceForAccessibility("Error: $errorMessage")
    }

    /**
     * Validate multiple fields and return overall result
     */
    fun validateAll(vararg validations: () -> ValidationResult): ValidationResult {
        val results = validations.map { it() }
        val firstError = results.firstOrNull { !it.isValid }
        
        return if (firstError != null) {
            ValidationResult(false, firstError.errorMessage)
        } else {
            ValidationResult(true)
        }
    }

    /**
     * Clear all errors from multiple input layouts
     */
    fun clearAllErrors(vararg inputLayouts: TextInputLayout) {
        inputLayouts.forEach { clearError(it) }
    }

    companion object {
        /**
         * Common validation patterns
         */
        object Patterns {
            val NAME_PATTERN = Regex("^[a-zA-Z\\s]{2,50}$")
            val USERNAME_PATTERN = Regex("^[a-zA-Z0-9_]{3,20}$")
            val STRONG_PASSWORD_PATTERN = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@\$!%*?&])[A-Za-z\\d@\$!%*?&]{8,}$")
        }
    }
}