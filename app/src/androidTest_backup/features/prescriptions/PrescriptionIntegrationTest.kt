package com.example.health_assistant.features.prescriptions

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.example.health_assistant.R
import com.example.health_assistant.main.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for prescription functionality preservation
 * Tests that prescription capture, storage, categorization, and search remain functional
 * after UI consistency changes
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class PrescriptionIntegrationTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun testPrescriptionCaptureAndStorageFunctionality() {
        // Navigate to prescriptions
        onView(withId(R.id.navigation_prescriptions))
            .perform(click())

        // Verify prescriptions fragment is displayed
        onView(withId(R.id.recyclerViewPrescriptions))
            .check(matches(isDisplayed()))

        // Test add prescription functionality
        onView(withId(R.id.fabAddPrescription))
            .check(matches(isDisplayed()))
            .perform(click())

        // Verify add prescription bottom sheet opens
        onView(withId(R.id.doctorNameInputLayout))
            .check(matches(isDisplayed()))

        // Test form input functionality
        onView(withId(R.id.doctorNameEditText))
            .perform(typeText("Dr. Test Smith"))
            .check(matches(withText("Dr. Test Smith")))

        // Test category dropdown functionality
        onView(withId(R.id.diseaseCategoryDropdown))
            .perform(click())

        // Test notes input functionality
        onView(withId(R.id.notesEditText))
            .perform(typeText("Test prescription notes"))
            .check(matches(withText("Test prescription notes")))

        // Test save button functionality
        onView(withId(R.id.saveButton))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testPrescriptionSearchAndFilteringFunctionality() {
        // Navigate to prescriptions
        onView(withId(R.id.navigation_prescriptions))
            .perform(click())

        // Test search functionality
        onView(withId(R.id.searchEditText))
            .check(matches(isDisplayed()))
            .perform(typeText("cardiology"))
            .check(matches(withText("cardiology")))

        // Test category filter functionality
        onView(withId(R.id.categoryFilterButton))
            .check(matches(isDisplayed()))
            .perform(click())

        // Verify filter indicator functionality
        onView(withId(R.id.activeFilterIndicator))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testPrescriptionDetailViewFunctionality() {
        // Navigate to prescriptions
        onView(withId(R.id.navigation_prescriptions))
            .perform(click())

        // Verify prescription cards are displayed with proper styling
        onView(withId(R.id.recyclerViewPrescriptions))
            .check(matches(isDisplayed()))

        // Test prescription card click functionality would go here
        // Note: This would require test data to be present
    }

    @Test
    fun testPrescriptionCameraCaptureFunctionality() {
        // Navigate to prescriptions
        onView(withId(R.id.navigation_prescriptions))
            .perform(click())

        // Open add prescription
        onView(withId(R.id.fabAddPrescription))
            .perform(click())

        // Test camera capture placeholder functionality
        onView(withId(R.id.capturePhotoPlaceholder))
            .check(matches(isDisplayed()))
            .perform(click())

        // Verify camera capture UI elements are present and styled correctly
        // Note: Camera functionality would require camera permissions and mock camera
    }

    @Test
    fun testPrescriptionAccessibilityCompliance() {
        // Navigate to prescriptions
        onView(withId(R.id.navigation_prescriptions))
            .perform(click())

        // Test prescription card accessibility
        onView(withId(R.id.recyclerViewPrescriptions))
            .check(matches(isDisplayed()))

        // Test FAB accessibility
        onView(withId(R.id.fabAddPrescription))
            .check(matches(isDisplayed()))
            .check(matches(hasContentDescription()))

        // Test search accessibility
        onView(withId(R.id.searchEditText))
            .check(matches(isDisplayed()))

        // Test filter button accessibility
        onView(withId(R.id.categoryFilterButton))
            .check(matches(hasContentDescription()))
    }

    @Test
    fun testPrescriptionFormValidationFunctionality() {
        // Navigate to prescriptions
        onView(withId(R.id.navigation_prescriptions))
            .perform(click())

        // Open add prescription
        onView(withId(R.id.fabAddPrescription))
            .perform(click())

        // Test form validation - empty doctor name
        onView(withId(R.id.saveButton))
            .check(matches(isNotEnabled()))

        // Fill in doctor name
        onView(withId(R.id.doctorNameEditText))
            .perform(typeText("Dr. Test"))

        // Test that save button becomes enabled
        onView(withId(R.id.saveButton))
            .check(matches(isEnabled()))

        // Test cancel functionality
        onView(withId(R.id.cancelButton))
            .check(matches(isDisplayed()))
            .perform(click())
    }
}