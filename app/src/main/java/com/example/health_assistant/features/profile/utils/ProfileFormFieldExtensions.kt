package com.example.health_assistant.features.profile.utils

import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputLayout

/**
 * Extension functions for profile form field operations
 * Provides consistent behavior and reduces boilerplate code
 */

/**
 * Set error state on TextInputLayout with accessibility support
 */
fun TextInputLayout.setErrorWithAccessibility(errorMessage: String?) {
    if (errorMessage != null) {
        error = errorMessage
        isErrorEnabled = true
        announceForAccessibility("Error: $errorMessage")
    } else {
        error = null
        isErrorEnabled = false
    }
}

/**
 * Clear error state and provide success feedback
 */
fun TextInputLayout.clearErrorWithSuccess() {
    if (error != null) {
        announceForAccessibility("Field is now valid")
    }
    error = null
    isErrorEnabled = false
}

/**
 * Set field as required with visual indicator
 */
fun TextInputLayout.markAsRequired() {
    val currentHint = hint.toString()
    if (!currentHint.contains("*")) {
        hint = "$currentHint *"
    }
}

/**
 * Focus field and show keyboard with smooth scrolling
 */
fun TextInputLayout.focusWithKeyboard(parentView: View? = null) {
    editText?.let { editText ->
        editText.requestFocus()

        // Show keyboard
        val imm = ContextCompat.getSystemService(context, InputMethodManager::class.java)
        imm?.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)

        // Smooth scroll to field if parent view provided
        parentView?.let { parent ->
            parent.post {
                val location = IntArray(2)
                getLocationOnScreen(location)
                if (parent is androidx.core.widget.NestedScrollView) {
                    parent.smoothScrollTo(0, location[1] - 200)
                }
            }
        }
    }
}

/**
 * Validate field with visual feedback
 */
fun TextInputLayout.validateWithFeedback(
    validator: (String) -> Boolean,
    errorMessage: String,
    successMessage: String? = null
): Boolean {
    val text = editText?.text?.toString() ?: ""
    val isValid = validator(text)

    if (isValid) {
        clearErrorWithSuccess()
        successMessage?.let { message ->
            announceForAccessibility(message)
        }
    } else {
        setErrorWithAccessibility(errorMessage)
    }

    return isValid
}

/**
 * Hide keyboard for the fragment
 */
fun Fragment.hideKeyboard() {
    view?.let { view ->
        val imm = ContextCompat.getSystemService(requireContext(), InputMethodManager::class.java)
        imm?.hideSoftInputFromWindow(view.windowToken, 0)
    }
}

/**
 * Show keyboard for specific view
 */
fun Fragment.showKeyboard(view: View) {
    val imm = ContextCompat.getSystemService(requireContext(), InputMethodManager::class.java)
    imm?.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
}

/**
 * Announce message for accessibility
 */
fun View.announceAccessibilityMessage(message: String) {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
        announceForAccessibility(message)
    }
}

/**
 * Set content description with context
 */
fun View.setContextualContentDescription(baseDescription: String, context: String? = null) {
    contentDescription = if (context != null) {
        "$baseDescription. $context"
    } else {
        baseDescription
    }
}