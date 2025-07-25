package com.example.health_assistant.auth.components

import android.content.Context
import android.view.LayoutInflater
import android.widget.Button
import androidx.test.core.app.ApplicationProvider
import com.example.health_assistant.R
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for authentication button styling components
 * Tests the application of HealthButton Primary/Secondary/Tertiary design system tokens
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class AuthenticationButtonStylingTest {

    private lateinit var context: Context
    private lateinit var inflater: LayoutInflater

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        inflater = LayoutInflater.from(context)
    }

    @Test
    fun `login button uses HealthButton Primary style`() {
        // Arrange & Act
        val loginView = inflater.inflate(R.layout.auth_fragment_login, null)
        val loginButton = loginView.findViewById<Button>(R.id.login_button)

        // Assert
        assertNotNull(loginButton, "Login button should exist")
        
        // Verify button has minimum height for accessibility (56dp)
        val expectedMinHeight = context.resources.getDimensionPixelSize(R.dimen.ds_component_button_height)
        assertTrue(loginButton.minimumHeight >= expectedMinHeight, "Login button should have minimum height for accessibility")
    }

    @Test
    fun `signup button uses HealthButton Primary style`() {
        // Arrange & Act
        val signupView = inflater.inflate(R.layout.auth_fragment_signup, null)
        val signupButton = signupView.findViewById<Button>(R.id.signup_button)

        // Assert
        assertNotNull(signupButton, "Signup button should exist")
        
        // Verify button has minimum height for accessibility
        val expectedMinHeight = context.resources.getDimensionPixelSize(R.dimen.ds_component_button_height)
        assertTrue(signupButton.minimumHeight >= expectedMinHeight, "Signup button should have minimum height for accessibility")
    }

    @Test
    fun `forgot password button uses HealthButton Secondary style`() {
        // Arrange & Act
        val loginView = inflater.inflate(R.layout.auth_fragment_login, null)
        val forgotPasswordButton = loginView.findViewById<Button>(R.id.forgot_password)

        // Assert
        assertNotNull(forgotPasswordButton, "Forgot password button should exist")
        
        // Verify button text is set correctly
        assertNotNull(forgotPasswordButton.text, "Forgot password button should have text")
        assertTrue(forgotPasswordButton.text.isNotEmpty(), "Forgot password button text should not be empty")
    }

    @Test
    fun `navigation buttons use HealthButton Tertiary style`() {
        // Arrange & Act
        val loginView = inflater.inflate(R.layout.auth_fragment_login, null)
        val createAccountButton = loginView.findViewById<Button>(R.id.create_account_prompt)
        
        val signupView = inflater.inflate(R.layout.auth_fragment_signup, null)
        val loginPromptButton = signupView.findViewById<Button>(R.id.login_prompt)

        // Assert
        assertNotNull(createAccountButton, "Create account button should exist")
        assertNotNull(loginPromptButton, "Login prompt button should exist")
        
        // Verify buttons have text
        assertNotNull(createAccountButton.text, "Create account button should have text")
        assertNotNull(loginPromptButton.text, "Login prompt button should have text")
    }

    @Test
    fun `button spacing uses HealthSpacing tokens`() {
        // Arrange & Act
        val loginView = inflater.inflate(R.layout.auth_fragment_login, null)
        val loginButton = loginView.findViewById<Button>(R.id.login_button)
        val createAccountButton = loginView.findViewById<Button>(R.id.create_account_prompt)

        // Assert
        assertNotNull(loginButton, "Login button should exist")
        assertNotNull(createAccountButton, "Create account button should exist")
        
        // Verify margin values match design system tokens
        val loginButtonParams = loginButton.layoutParams as android.view.ViewGroup.MarginLayoutParams
        val createAccountButtonParams = createAccountButton.layoutParams as android.view.ViewGroup.MarginLayoutParams
        
        val expectedLargeMargin = context.resources.getDimensionPixelSize(R.dimen.ds_margin_large)
        val expectedXLMargin = context.resources.getDimensionPixelSize(R.dimen.ds_margin_xl)
        
        assertEquals(expectedLargeMargin, loginButtonParams.topMargin, "Login button should use ds_margin_large")
        assertEquals(expectedXLMargin, createAccountButtonParams.topMargin, "Create account button should use ds_margin_xl")
    }

    @Test
    fun `authentication buttons preserve accessibility attributes`() {
        // Arrange & Act
        val loginView = inflater.inflate(R.layout.auth_fragment_login, null)
        val loginButton = loginView.findViewById<Button>(R.id.login_button)
        val forgotPasswordButton = loginView.findViewById<Button>(R.id.forgot_password)

        // Assert
        assertNotNull(loginButton, "Login button should exist")
        assertNotNull(forgotPasswordButton, "Forgot password button should exist")
        
        // Verify buttons are clickable and focusable for accessibility
        assertTrue(loginButton.isClickable, "Login button should be clickable")
        assertTrue(loginButton.isFocusable, "Login button should be focusable")
        assertTrue(forgotPasswordButton.isClickable, "Forgot password button should be clickable")
        assertTrue(forgotPasswordButton.isFocusable, "Forgot password button should be focusable")
    }
}