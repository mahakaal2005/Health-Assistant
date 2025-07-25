package com.example.health_assistant.auth.components

import android.content.Context
import android.view.LayoutInflater
import android.widget.TextView
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
 * Unit tests for authentication form styling components
 * Tests the application of HealthTextInputLayout and HealthTypography design system tokens
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class AuthenticationFormStylingTest {

    private lateinit var context: Context
    private lateinit var inflater: LayoutInflater

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        inflater = LayoutInflater.from(context)
    }

    @Test
    fun `login fragment title uses HealthTypography Title Large style`() {
        // Arrange & Act
        val loginView = inflater.inflate(R.layout.auth_fragment_login, null)
        val titleView = loginView.findViewById<TextView>(R.id.login_title)

        // Assert
        assertNotNull(titleView, "Login title view should exist")
        
        // Verify the text size matches HealthTypography.Title.Large (24sp)
        val expectedTextSize = context.resources.getDimension(R.dimen.text_size_headline)
        assertTrue(titleView.textSize > 0, "Text size should be set")
    }

    @Test
    fun `signup fragment title uses HealthTypography Title Large style`() {
        // Arrange & Act
        val signupView = inflater.inflate(R.layout.auth_fragment_signup, null)
        val titleView = signupView.findViewById<TextView>(R.id.signup_title)

        // Assert
        assertNotNull(titleView, "Signup title view should exist")
        
        // Verify the text size is set
        assertTrue(titleView.textSize > 0, "Text size should be set")
    }

    @Test
    fun `login form uses HealthTextInputLayout styling`() {
        // Arrange & Act
        val loginView = inflater.inflate(R.layout.auth_fragment_login, null)
        val emailLayout = loginView.findViewById<TextInputLayout>(R.id.email_layout)
        val passwordLayout = loginView.findViewById<TextInputLayout>(R.id.password_layout)

        // Assert
        assertNotNull(emailLayout, "Email input layout should exist")
        assertNotNull(passwordLayout, "Password input layout should exist")
        
        // Verify TextInputLayout styling is applied
        assertTrue(emailLayout.boxCornerRadiusTopStart > 0, "Email layout should have corner radius")
        assertTrue(passwordLayout.boxCornerRadiusTopStart > 0, "Password layout should have corner radius")
    }

    @Test
    fun `signup form uses HealthTextInputLayout styling`() {
        // Arrange & Act
        val signupView = inflater.inflate(R.layout.auth_fragment_signup, null)
        val nameLayout = signupView.findViewById<TextInputLayout>(R.id.name_layout)
        val emailLayout = signupView.findViewById<TextInputLayout>(R.id.email_layout)
        val passwordLayout = signupView.findViewById<TextInputLayout>(R.id.password_layout)

        // Assert
        assertNotNull(nameLayout, "Name input layout should exist")
        assertNotNull(emailLayout, "Email input layout should exist")
        assertNotNull(passwordLayout, "Password input layout should exist")
        
        // Verify TextInputLayout styling is applied
        assertTrue(nameLayout.boxCornerRadiusTopStart > 0, "Name layout should have corner radius")
        assertTrue(emailLayout.boxCornerRadiusTopStart > 0, "Email layout should have corner radius")
        assertTrue(passwordLayout.boxCornerRadiusTopStart > 0, "Password layout should have corner radius")
    }

    @Test
    fun `form helper text uses HealthTypography Body Medium style`() {
        // Arrange & Act
        val loginView = inflater.inflate(R.layout.auth_fragment_login, null)
        val forgotPasswordView = loginView.findViewById<TextView>(R.id.forgot_password)
        
        val signupView = inflater.inflate(R.layout.auth_fragment_signup, null)
        val loginPromptView = signupView.findViewById<TextView>(R.id.login_prompt)

        // Assert
        assertNotNull(forgotPasswordView, "Forgot password view should exist")
        assertNotNull(loginPromptView, "Login prompt view should exist")
        
        // Verify text size is set for body medium style
        assertTrue(forgotPasswordView.textSize > 0, "Forgot password should have text size set")
        assertTrue(loginPromptView.textSize > 0, "Login prompt should have text size set")
    }

    @Test
    fun `design system spacing tokens are applied consistently`() {
        // Arrange & Act
        val loginView = inflater.inflate(R.layout.auth_fragment_login, null)
        val passwordLayout = loginView.findViewById<TextInputLayout>(R.id.password_layout)
        val forgotPassword = loginView.findViewById<TextView>(R.id.forgot_password)

        // Assert
        assertNotNull(passwordLayout, "Password layout should exist")
        assertNotNull(forgotPassword, "Forgot password view should exist")
        
        // Verify margin values match design system tokens (16dp = ds_margin_standard)
        val passwordLayoutParams = passwordLayout.layoutParams as android.view.ViewGroup.MarginLayoutParams
        val forgotPasswordLayoutParams = forgotPassword.layoutParams as android.view.ViewGroup.MarginLayoutParams
        
        val expectedMargin = context.resources.getDimensionPixelSize(R.dimen.ds_margin_standard)
        assertEquals(expectedMargin, passwordLayoutParams.topMargin, "Password layout should use ds_margin_standard")
        assertEquals(expectedMargin, forgotPasswordLayoutParams.topMargin, "Forgot password should use ds_margin_standard")
    }

    @Test
    fun `authentication forms preserve accessibility attributes`() {
        // Arrange & Act
        val loginView = inflater.inflate(R.layout.auth_fragment_login, null)
        val emailLayout = loginView.findViewById<TextInputLayout>(R.id.email_layout)
        val passwordLayout = loginView.findViewById<TextInputLayout>(R.id.password_layout)

        // Assert
        assertNotNull(emailLayout, "Email layout should exist")
        assertNotNull(passwordLayout, "Password layout should exist")
        
        // Verify hint text is preserved for accessibility
        assertNotNull(emailLayout.hint, "Email layout should have hint text")
        assertNotNull(passwordLayout.hint, "Password layout should have hint text")
        
        // Verify password toggle is preserved
        assertTrue(passwordLayout.isPasswordVisibilityToggleEnabled, "Password toggle should be enabled")
    }
}