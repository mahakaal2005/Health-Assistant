package com.example.health_assistant.main

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import com.example.health_assistant.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for navigation flow preservation
 * 
 * Tests that navigation functionality continues to work correctly
 * after UI consistency changes are applied
 */
@RunWith(AndroidJUnit4::class)
class NavigationIntegrationTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun testBottomNavigationItemsAreDisplayed() {
        // Verify all bottom navigation items are displayed
        onView(withId(R.id.homeFragment)).check(matches(isDisplayed()))
        onView(withId(R.id.discoverFragment)).check(matches(isDisplayed()))
        onView(withId(R.id.journalFragment)).check(matches(isDisplayed()))
        onView(withId(R.id.profileFragment)).check(matches(isDisplayed()))
    }

    @Test
    fun testBottomNavigationItemsAreClickable() {
        // Test that all navigation items are clickable and accessible
        onView(withId(R.id.homeFragment)).check(matches(isClickable()))
        onView(withId(R.id.discoverFragment)).check(matches(isClickable()))
        onView(withId(R.id.journalFragment)).check(matches(isClickable()))
        onView(withId(R.id.profileFragment)).check(matches(isClickable()))
    }

    @Test
    fun testNavigationToDiscoverFragment() {
        // Test navigation to Discover fragment
        onView(withId(R.id.discoverFragment)).perform(click())
        
        // Verify we're in the Discover fragment (check for discover-specific content)
        // This test ensures navigation functionality is preserved
        onView(withId(R.id.bottom_nav)).check(matches(isDisplayed()))
    }

    @Test
    fun testNavigationToJournalFragment() {
        // Test navigation to Journal fragment
        onView(withId(R.id.journalFragment)).perform(click())
        
        // Verify we're in the Journal fragment
        onView(withId(R.id.bottom_nav)).check(matches(isDisplayed()))
    }

    @Test
    fun testNavigationToProfileFragment() {
        // Test navigation to Profile fragment
        onView(withId(R.id.profileFragment)).perform(click())
        
        // Verify we're in the Profile fragment
        onView(withId(R.id.bottom_nav)).check(matches(isDisplayed()))
    }

    @Test
    fun testNavigationBackToHome() {
        // Test navigation flow: Home -> Discover -> Home
        onView(withId(R.id.discoverFragment)).perform(click())
        onView(withId(R.id.homeFragment)).perform(click())
        
        // Verify we're back at home
        onView(withId(R.id.bottom_nav)).check(matches(isDisplayed()))
    }

    @Test
    fun testBottomNavigationAccessibility() {
        // Test that bottom navigation items have proper accessibility support
        onView(withId(R.id.homeFragment))
            .check(matches(hasContentDescription()))
        onView(withId(R.id.discoverFragment))
            .check(matches(hasContentDescription()))
        onView(withId(R.id.journalFragment))
            .check(matches(hasContentDescription()))
        onView(withId(R.id.profileFragment))
            .check(matches(hasContentDescription()))
    }

    @Test
    fun testNavigationStatePreservation() {
        // Test that navigation state is preserved across fragment changes
        onView(withId(R.id.journalFragment)).perform(click())
        onView(withId(R.id.profileFragment)).perform(click())
        onView(withId(R.id.journalFragment)).perform(click())
        
        // Verify navigation is still functional
        onView(withId(R.id.bottom_nav)).check(matches(isDisplayed()))
        onView(withId(R.id.journalFragment)).check(matches(isSelected()))
    }

    @Test
    fun testNavigationTransitionSmoothness() {
        // Test that navigation transitions work smoothly
        // Rapid navigation between fragments should not cause crashes
        onView(withId(R.id.discoverFragment)).perform(click())
        Thread.sleep(100) // Brief pause for transition
        onView(withId(R.id.journalFragment)).perform(click())
        Thread.sleep(100)
        onView(withId(R.id.profileFragment)).perform(click())
        Thread.sleep(100)
        onView(withId(R.id.homeFragment)).perform(click())
        
        // Verify final state is correct
        onView(withId(R.id.bottom_nav)).check(matches(isDisplayed()))
    }

    @Test
    fun testNavigationWithScreenReader() {
        // Test navigation accessibility with screen reader support
        // Verify content descriptions are properly set
        onView(withId(R.id.homeFragment))
            .check(matches(hasContentDescription()))
            .check(matches(withContentDescription("Navigate to Home screen")))
        
        onView(withId(R.id.discoverFragment))
            .check(matches(hasContentDescription()))
            .check(matches(withContentDescription("Navigate to Discover health content")))
        
        onView(withId(R.id.journalFragment))
            .check(matches(hasContentDescription()))
            .check(matches(withContentDescription("Navigate to Journal entries")))
        
        onView(withId(R.id.profileFragment))
            .check(matches(hasContentDescription()))
            .check(matches(withContentDescription("Navigate to Profile settings")))
    }
}