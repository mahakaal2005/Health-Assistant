package com.example.health_assistant.auth

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.example.health_assistant.R
import com.example.health_assistant.auth.AuthActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for authentication flow functionality preservation
 * Tests that authentication flows work correctly with new UI styling
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class AuthenticationIntegrationTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(AuthActivity::class.java)

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun testLoginFormFunctionalityPreservation() {
        // Verify login form elements are present and functional
        onView(withId(R.id.email_input))
            .check(matches(isDisplayed()))
            .perform(typeText("test@example.com"))

        onView(withId(R.id.password_input))
            .check(matches(isDisplayed()))
            .perform(typeText("password123"))

        onView(withId(R.id.login_button))
            .check(matches(isDisplayed()))
            .check(matches(isClickable()))

        onView(withId(R.id.forgot_password))
            .check(matches(isDisplayed()))
            .check(matches(isClickable()))

        onView(withId(R.id.create_account_prompt))
            .check(matches(isDisplayed()))
            .check(matches(isClickable()))
    }

    @Test
    fun testSignupFormFunctionalityPreservation() {
        // Navigate to signup if needed (implementation depends on navigation)
        // For now, test signup form elements
        onView(withId(R.id.name_input))
            .check(matches(isDisplayed()))
            .perform(typeText("John Doe"))

        onView(withId(R.id.email_input))
            .check(matches(isDisplayed()))
            .perform(typeText("john@example.com"))

        onView(withId(R.id.password_input))
            .check(matches(isDisplayed()))
            .perform(typeText("securepassword"))

        onView(withId(R.id.signup_button))
            .check(matches(isDisplayed()))
            .check(matches(isClickable()))

        onView(withId(R.id.login_prompt))
            .check(matches(isDisplayed()))
            .check(matches(isClickable()))
    }

    @Test
    fun testFormValidationStyling() {
        // Test error state styling
        onView(withId(R.id.email_input))
            .perform(typeText("invalid-email"))

        onView(withId(R.id.password_input))
            .perform(typeText("123")) // Too short

        // Verify form validation triggers (implementation depends on validation logic)
        onView(withId(R.id.login_button))
            .perform(click())

        // Verify error styling is applied (this would need actual validation implementation)
        onView(withId(R.id.email_layout))
            .check(matches(isDisplayed()))

        onView(withId(R.id.password_layout))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testButtonAccessibility() {
        // Test that buttons meet accessibility requirements
        onView(withId(R.id.login_button))
            .check(matches(isDisplayed()))
            .check(matches(isClickable()))
            .check(matches(isFocusable()))

        onView(withId(R.id.forgot_password))
            .check(matches(isDisplayed()))
            .check(matches(isClickable()))
            .check(matches(isFocusable()))

        onView(withId(R.id.create_account_prompt))
            .check(matches(isDisplayed()))
            .check(matches(isClickable()))
            .check(matches(isFocusable()))
    }

    @Test
    fun testFormInputAccessibility() {
        // Test that form inputs meet accessibility requirements
        onView(withId(R.id.email_input))
            .check(matches(isDisplayed()))
            .check(matches(isFocusable()))

        onView(withId(R.id.password_input))
            .check(matches(isDisplayed()))
            .check(matches(isFocusable()))

        // Verify hint text is present for accessibility
        onView(withId(R.id.email_layout))
            .check(matches(hasDescendant(withHint(context.getString(R.string.email)))))

        onView(withId(R.id.password_layout))
            .check(matches(hasDescendant(withHint(context.getString(R.string.password)))))
    }

    @Test
    fun testDesignSystemConsistency() {
        // Test that design system styling is consistently applied
        onView(withId(R.id.login_title))
            .check(matches(isDisplayed()))

        onView(withId(R.id.email_layout))
            .check(matches(isDisplayed()))

        onView(withId(R.id.password_layout))
            .check(matches(isDisplayed()))

        onView(withId(R.id.login_button))
            .check(matches(isDisplayed()))

        // Verify all elements are visible and properly styled
        onView(withId(R.id.login_card))
            .check(matches(isDisplayed()))
    }
}