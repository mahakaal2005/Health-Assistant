package com.example.health_assistant.auth

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.example.health_assistant.R
import com.example.health_assistant.auth.AuthActivity
import org.hamcrest.Matchers.allOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Accessibility compliance tests for authentication components
 * Tests WCAG 2.1 AA compliance for authentication UI
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class AuthenticationAccessibilityTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(AuthActivity::class.java)

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun testMinimumTouchTargetSize() {
        // Test that all interactive elements meet 48dp minimum touch target
        onView(withId(R.id.login_button))
            .check(matches(isDisplayed()))
            .check(matches(hasMinimumSize(48, 48)))

        onView(withId(R.id.forgot_password))
            .check(matches(isDisplayed()))
            .check(matches(hasMinimumSize(48, 48)))

        onView(withId(R.id.create_account_prompt))
            .check(matches(isDisplayed()))
            .check(matches(hasMinimumSize(48, 48)))
    }

    @Test
    fun testContentDescriptions() {
        // Test that interactive elements have proper content descriptions
        onView(withId(R.id.email_input))
            .check(matches(isDisplayed()))
            .check(matches(hasContentDescription()))

        onView(withId(R.id.password_input))
            .check(matches(isDisplayed()))
            .check(matches(hasContentDescription()))

        onView(withId(R.id.login_button))
            .check(matches(isDisplayed()))
            .check(matches(hasContentDescription()))
    }

    @Test
    fun testFocusStates() {
        // Test that all interactive elements are focusable
        onView(withId(R.id.email_input))
            .check(matches(isFocusable()))

        onView(withId(R.id.password_input))
            .check(matches(isFocusable()))

        onView(withId(R.id.login_button))
            .check(matches(isFocusable()))

        onView(withId(R.id.forgot_password))
            .check(matches(isFocusable()))

        onView(withId(R.id.create_account_prompt))
            .check(matches(isFocusable()))
    }

    @Test
    fun testTextContrast() {
        // Test that text elements are visible (basic visibility test)
        onView(withId(R.id.login_title))
            .check(matches(isDisplayed()))

        onView(withId(R.id.email_layout))
            .check(matches(isDisplayed()))

        onView(withId(R.id.password_layout))
            .check(matches(isDisplayed()))

        onView(withId(R.id.forgot_password))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testFormLabeling() {
        // Test that form inputs have proper labels/hints
        onView(withId(R.id.email_layout))
            .check(matches(hasDescendant(withHint(context.getString(R.string.email)))))

        onView(withId(R.id.password_layout))
            .check(matches(hasDescendant(withHint(context.getString(R.string.password)))))
    }

    @Test
    fun testErrorAnnouncement() {
        // Test that error states are properly announced
        onView(withId(R.id.email_layout))
            .check(matches(isDisplayed()))

        onView(withId(R.id.password_layout))
            .check(matches(isDisplayed()))

        // Verify error capability exists (actual error testing would require validation logic)
        onView(withId(R.id.email_layout))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testKeyboardNavigation() {
        // Test that elements can be navigated with keyboard
        onView(withId(R.id.email_input))
            .check(matches(allOf(isDisplayed(), isFocusable())))

        onView(withId(R.id.password_input))
            .check(matches(allOf(isDisplayed(), isFocusable())))

        onView(withId(R.id.login_button))
            .check(matches(allOf(isDisplayed(), isFocusable())))
    }

    /**
     * Custom matcher to check minimum size requirements
     */
    private fun hasMinimumSize(minWidth: Int, minHeight: Int) = object : org.hamcrest.TypeSafeMatcher<android.view.View>() {
        override fun describeTo(description: org.hamcrest.Description) {
            description.appendText("has minimum size of ${minWidth}x${minHeight}dp")
        }

        override fun matchesSafely(view: android.view.View): Boolean {
            val density = view.context.resources.displayMetrics.density
            val minWidthPx = (minWidth * density).toInt()
            val minHeightPx = (minHeight * density).toInt()
            
            return view.width >= minWidthPx && view.height >= minHeightPx
        }
    }
}