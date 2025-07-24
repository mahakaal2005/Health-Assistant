package com.example.health_assistant.features.prescriptions.components

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.health_assistant.core.design.components.HealthFormValidation
import com.example.health_assistant.core.design.components.HealthTextInputLayout
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for prescription form styling components
 * Tests HealthTextInputLayout and HealthFormValidation integration with prescription forms
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class PrescriptionFormComponentTest {

    private lateinit var context: Context
    private lateinit var formValidation: HealthFormValidation

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        formValidation = HealthFormValidation(context)
    }

    @Test
    fun `test prescription form input creation with primary style`() {
        // Given
        val inputLayout = HealthTextInputLayout.createPrimaryInput(context)
        
        // When
        val inputStyle = inputLayout.getInputStyle()
        
        // Then
        assertEquals(HealthTextInputLayout.HealthInputStyle.PRIMARY, inputStyle)
        assertNotNull(inputLayout)
    }

    @Test
    fun `test prescription form input creation with secondary style`() {
        // Given
        val inputLayout = HealthTextInputLayout.createSecondaryInput(context)
        
        // When
        val inputStyle = inputLayout.getInputStyle()
        
        // Then
        assertEquals(HealthTextInputLayout.HealthInputStyle.SECONDARY, inputStyle)
        assertNotNull(inputLayout)
    }

    @Test
    fun `test prescription form input accessibility properties`() {
        // Given
        val inputLayout = HealthTextInputLayout.createPrimaryInput(context)
        
        // When & Then
        assertEquals(true, inputLayout.isFocusable)
        assertEquals(false, inputLayout.isFocusableInTouchMode)
        assertEquals(true, inputLayout.minimumHeight > 0)
        assertNotNull(inputLayout.contentDescription)
    }

    @Test
    fun `test prescription doctor name validation - required field`() {
        // Given
        val inputLayout = HealthTextInputLayout.createPrimaryInput(context)
        inputLayout.editText?.setText("")
        
        // When
        val result = formValidation.validateRequired(inputLayout, "Doctor name is required")
        
        // Then
        assertFalse(result.isValid)
        assertEquals("Doctor name is required", result.errorMessage)
        assertNotNull(inputLayout.error)
    }

    @Test
    fun `test prescription doctor name validation - valid input`() {
        // Given
        val inputLayout = HealthTextInputLayout.createPrimaryInput(context)
        inputLayout.editText?.setText("Dr. John Smith")
        
        // When
        val result = formValidation.validateRequired(inputLayout, "Doctor name is required")
        
        // Then
        assertTrue(result.isValid)
        assertEquals(null, result.errorMessage)
        assertEquals(null, inputLayout.error)
    }

    @Test
    fun `test prescription notes validation - maximum length`() {
        // Given
        val inputLayout = HealthTextInputLayout.createPrimaryInput(context)
        val longText = "a".repeat(501) // Assuming 500 char limit
        inputLayout.editText?.setText(longText)
        
        // When
        val result = formValidation.validateMaxLength(inputLayout, 500, "Notes too long")
        
        // Then
        assertFalse(result.isValid)
        assertEquals("Notes too long", result.errorMessage)
        assertNotNull(inputLayout.error)
    }

    @Test
    fun `test prescription form validation - multiple fields`() {
        // Given
        val doctorNameInput = HealthTextInputLayout.createPrimaryInput(context)
        val notesInput = HealthTextInputLayout.createPrimaryInput(context)
        
        doctorNameInput.editText?.setText("Dr. Smith")
        notesInput.editText?.setText("Valid notes")
        
        // When
        val result = formValidation.validateAll(
            { formValidation.validateRequired(doctorNameInput, "Doctor name required") },
            { formValidation.validateMaxLength(notesInput, 500, "Notes too long") }
        )
        
        // Then
        assertTrue(result.isValid)
        assertEquals(null, result.errorMessage)
    }

    @Test
    fun `test prescription form error clearing`() {
        // Given
        val inputLayout = HealthTextInputLayout.createPrimaryInput(context)
        inputLayout.editText?.setText("")
        
        // When - Set error first
        formValidation.setError(inputLayout, "Test error")
        assertNotNull(inputLayout.error)
        
        // Then - Clear error
        formValidation.clearError(inputLayout)
        assertEquals(null, inputLayout.error)
        assertFalse(inputLayout.isErrorEnabled)
    }

    @Test
    fun `test prescription category validation - empty selection`() {
        // Given
        val inputLayout = HealthTextInputLayout.createPrimaryInput(context)
        inputLayout.editText?.setText("")
        
        // When
        val result = formValidation.validateRequired(inputLayout, "Category is required")
        
        // Then
        assertFalse(result.isValid)
        assertEquals("Category is required", result.errorMessage)
    }

    @Test
    fun `test prescription form validation - edge case with whitespace`() {
        // Given
        val inputLayout = HealthTextInputLayout.createPrimaryInput(context)
        inputLayout.editText?.setText("   ")  // Only whitespace
        
        // When
        val result = formValidation.validateRequired(inputLayout, "Field cannot be empty")
        
        // Then
        assertFalse(result.isValid)
        assertEquals("Field cannot be empty", result.errorMessage)
    }

    @Test
    fun `test prescription notes validation - boundary conditions`() {
        // Given
        val inputLayout = HealthTextInputLayout.createPrimaryInput(context)
        
        // Test exactly at limit
        val exactLimitText = "a".repeat(500)
        inputLayout.editText?.setText(exactLimitText)
        
        // When
        val result = formValidation.validateMaxLength(inputLayout, 500, "Notes too long")
        
        // Then
        assertTrue(result.isValid)
        assertEquals(null, result.errorMessage)
    }
}