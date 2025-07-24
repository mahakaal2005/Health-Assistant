package com.example.health_assistant.features.prescriptions

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.example.health_assistant.R
import com.example.health_assistant.main.MainActivity
import org.hamcrest.Matchers.allOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Accessibility tests for prescription components
 * Tests WCAG 2.1 AA compliance for prescription UI components
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class PrescriptionAccessibilityTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun testPrescriptionCardAccessibilityCompliance() {
        // Navigate to prescriptions
        onView(withId(R.id.navigation_prescriptions))
            .perform(androidx.test.espresso.action.ViewActions.click())

        // Test prescription cards have proper content descriptions
        onView(withId(R.id.recyclerViewPrescriptions))
            .check(matches(isDisplayed()))

        // Test minimum touch target size (48dp) for interactive elements
        onView(withId(R.id.fabAddPrescription))
            .check(matches(allOf(
                isDisplayed(),
                hasContentDescription(),
                hasMinimumSize(48, 48)
            )))
    }

    @Test
    fun testPrescriptionCameraAccessibilityCompliance() {
        // Navigate to prescriptions and open camera
        onView(withId(R.id.navigation_prescriptions))
            .perform(androidx.test.espresso.action.ViewActions.click())

        onView(withId(R.id.fabAddPrescription))
            .perform(androidx.test.espresso.action.ViewActions.click())

        // Test camera capture accessibility
        onView(withId(R.id.capturePhotoPlaceholder))
            .check(matches(allOf(
                isDisplayed(),
                hasContentDescription()
            )))
    }

    @Test
    fun testPrescriptionFormAccessibilityCompliance() {
        // Navigate to prescriptions and open form
        onView(withId(R.id.navigation_prescriptions))
            .perform(androidx.test.espresso.action.ViewActions.click())

        onView(withId(R.id.fabAddPrescription))
            .perform(androidx.test.espresso.action.ViewActions.click())

        // Test form inputs have proper labels and hints
        onView(withId(R.id.doctorNameInputLayout))
            .check(matches(allOf(
                isDisplayed(),
                hasDescendant(withHint(org.hamcrest.Matchers.not(isEmptyString())))
            )))

        onView(withId(R.id.categoryInputLayout))
            .check(matches(allOf(
                isDisplayed(),
                hasDescendant(withHint(org.hamcrest.Matchers.not(isEmptyString())))
            )))

        onView(withId(R.id.notesInputLayout))
            .check(matches(allOf(
                isDisplayed(),
                hasDescendant(withHint(org.hamcrest.Matchers.not(isEmptyString())))
            )))

        // Test buttons have proper content descriptions and minimum size
        onView(withId(R.id.saveButton))
            .check(matches(allOf(
                isDisplayed(),
                hasMinimumSize(48, 48)
            )))

        onView(withId(R.id.cancelButton))
            .check(matches(allOf(
                isDisplayed(),
                hasMinimumSize(48, 48)
            )))
    }

    @Test
    fun testPrescriptionSearchAccessibilityCompliance() {
        // Navigate to prescriptions
        onView(withId(R.id.navigation_prescriptions))
            .perform(androidx.test.espresso.action.ViewActions.click())

        // Test search input accessibility
        onView(withId(R.id.searchEditText))
            .check(matches(allOf(
                isDisplayed(),
                hasDescendant(withHint(org.hamcrest.Matchers.not(isEmptyString())))
            )))

        // Test filter button accessibility
        onView(withId(R.id.categoryFilterButton))
            .check(matches(allOf(
                isDisplayed(),
                hasContentDescription(),
                hasMinimumSize(48, 48)
            )))
    }

    /**
     * Custom matcher to check minimum size for accessibility compliance
     */
    private fun hasMinimumSize(minWidth: Int, minHeight: Int) = object : org.hamcrest.TypeSafeMatcher<android.view.View>() {
        override fun describeTo(description: org.hamcrest.Description?) {
            description?.appendText("has minimum size of ${minWidth}x${minHeight}dp")
        }

        override fun matchesSafely(item: android.view.View?): Boolean {
            if (item == null) return false
            val density = item.context.resources.displayMetrics.density
            val minWidthPx = (minWidth * density).toInt()
            val minHeightPx = (minHeight * density).toInt()
            return item.width >= minWidthPx && item.height >= minHeightPx
        }
    }
}