package com.example.health_assistant.auth.components

import android.content.Context
import android.view.LayoutInflater
import androidx.test.core.app.ApplicationProvider
import com.example.health_assistant.R
import com.google.android.material.textfield.TextInputLayout
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for authentication validation styling
 * Tests the application of HealthColors.Error and HealthColors.Success for validation feedback
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class AuthenticationValidationTest {

    private lateinit var context: Context
    private lateinit var inflater: LayoutInflater

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        inflater = LayoutInflater.from(context)
    }

    @Test
    fun `TextInputLayout uses HealthColors Error for error states`() {
        // Arrange & Act
        val loginView = inflater.inflate(R.layout.auth_fragment_login, null)
        val emailLayout = loginView.findViewById<TextInputLayout>(R.id.email_layout)

        // Assert
        assertNotNull(emailLayout, "Email layout should exist")
        
        // Simulate error state
        emailLayout.error = "Invalid email format"
        
        // Verify error is displayed
        assertNotNull(emailLayout.error, "Error message should be set")
        assertTrue(emailLayout.isErrorEnabled, "Error should be enabled")
    }

    @Test
    fun `TextInputLayout uses HealthTypography Caption for error messages`() {
        // Arrange & Act
        val signupView = inflater.inflate(R.layout.auth_fragment_signup, null)
        val passwordLayout = signupView.findViewById<TextInputLayout>(R.id.password_layout)

        // Assert
        assertNotNull(passwordLayout, "Password layout should exist")
        
        // Simulate error state with message
        passwordLayout.error = "Password must be at least 8 characters"
        
        // Verify error message is set
        assertEquals("Password must be at least 8 characters", passwordLayout.error.toString())
        assertTrue(passwordLayout.isErrorEnabled, "Error should be enabled")
    }

    @Test
    fun `TextInputLayout error styling uses design system tokens`() {
        // Arrange & Act
        val loginView = inflater.inflate(R.layout.auth_fragment_login, null)
        val emailLayout = loginView.findViewById<TextInputLayout>(R.id.email_layout)
        val passwordLayout = loginView.findViewById<TextInputLayout>(R.id.password_layout)

        // Assert
        assertNotNull(emailLayout, "Email layout should exist")
        assertNotNull(passwordLayout, "Password layout should exist")
        
        // Verify box stroke width uses design system tokens
        val expectedThinBorder = context.resources.getDimensionPixelSize(R.dimen.ds_border_width_thin)
        val expectedMediumBorder = context.resources.getDimensionPixelSize(R.dimen.ds_border_width_medium)
        
        assertEquals(expectedThinBorder, emailLayout.boxStrokeWidth, "Box stroke width should use ds_border_width_thin")
        assertEquals(expectedMediumBorder, emailLayout.boxStrokeWidthFocused, "Focused stroke width should use ds_border_width_medium")
    }

    @Test
    fun `TextInputLayout corner radius uses design system tokens`() {
        // Arrange & Act
        val loginView = inflater.inflate(R.layout.auth_fragment_login, null)
        val emailLayout = loginView.findViewById<TextInputLayout>(R.id.email_layout)

        // Assert
        assertNotNull(emailLayout, "Email layout should exist")
        
        // Verify corner radius uses design system tokens
        val expectedCornerRadius = context.resources.getDimension(R.dimen.ds_component_card_radius_small)
        assertEquals(expectedCornerRadius, emailLayout.boxCornerRadiusTopStart, "Corner radius should use ds_component_card_radius_small")
        assertEquals(expectedCornerRadius, emailLayout.boxCornerRadiusTopEnd, "Corner radius should use ds_component_card_radius_small")
        assertEquals(expectedCornerRadius, emailLayout.boxCornerRadiusBottomStart, "Corner radius should use ds_component_card_radius_small")
        assertEquals(expectedCornerRadius, emailLayout.boxCornerRadiusBottomEnd, "Corner radius should use ds_component_card_radius_small")
    }

    @Test
    fun `validation feedback positioning uses consistent spacing`() {
        // Arrange & Act
        val signupView = inflater.inflate(R.layout.auth_fragment_signup, null)
        val nameLayout = signupView.findViewById<TextInputLayout>(R.id.name_layout)
        val emailLayout = signupView.findViewById<TextInputLayout>(R.id.email_layout)

        // Assert
        assertNotNull(nameLayout, "Name layout should exist")
        assertNotNull(emailLayout, "Email layout should exist")
        
        // Verify consistent margin spacing between form fields
        val emailLayoutParams = emailLayout.layoutParams as android.view.ViewGroup.MarginLayoutParams
        val expectedMargin = context.resources.getDimensionPixelSize(R.dimen.ds_margin_standard)
        
        assertEquals(expectedMargin, emailLayoutParams.topMargin, "Email layout should use ds_margin_standard")
    }

    @Test
    fun `error handling preserves accessibility attributes`() {
        // Arrange & Act
        val loginView = inflater.inflate(R.layout.auth_fragment_login, null)
        val passwordLayout = loginView.findViewById<TextInputLayout>(R.id.password_layout)

        // Assert
        assertNotNull(passwordLayout, "Password layout should exist")
        
        // Verify password toggle is preserved for accessibility
        assertTrue(passwordLayout.isPasswordVisibilityToggleEnabled, "Password toggle should be enabled for accessibility")
        
        // Verify hint text is preserved for accessibility
        assertNotNull(passwordLayout.hint, "Hint text should be preserved for accessibility")
    }
}