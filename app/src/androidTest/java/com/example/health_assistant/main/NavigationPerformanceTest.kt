package com.example.health_assistant.main

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.example.health_assistant.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

/**
 * Performance tests for navigation responsiveness
 * 
 * Tests that navigation performance meets requirements after
 * UI consistency changes are applied
 */
@RunWith(AndroidJUnit4::class)
class NavigationPerformanceTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun testNavigationTransitionPerformance() {
        // Test that navigation transitions complete within acceptable time
        val startTime = System.currentTimeMillis()
        
        // Perform navigation sequence
        onView(withId(R.id.discoverFragment)).perform(click())
        Thread.sleep(50) // Allow transition to start
        onView(withId(R.id.journalFragment)).perform(click())
        Thread.sleep(50)
        onView(withId(R.id.profileFragment)).perform(click())
        Thread.sleep(50)
        onView(withId(R.id.homeFragment)).perform(click())
        
        val endTime = System.currentTimeMillis()
        val totalTime = endTime - startTime
        
        // Navigation sequence should complete within 2 seconds
        assertTrue("Navigation sequence should complete within 2000ms, took ${totalTime}ms", 
            totalTime < 2000)
    }

    @Test
    fun testSingleNavigationTransitionSpeed() {
        // Test that individual navigation transitions are fast
        val measurements = mutableListOf<Long>()
        
        // Measure multiple navigation transitions
        repeat(5) {
            val startTime = System.currentTimeMillis()
            onView(withId(R.id.discoverFragment)).perform(click())
            Thread.sleep(100) // Wait for transition to complete
            val endTime = System.currentTimeMillis()
            
            measurements.add(endTime - startTime)
            
            // Return to home for next test
            onView(withId(R.id.homeFragment)).perform(click())
            Thread.sleep(100)
        }
        
        val averageTime = measurements.average()
        
        // Individual transitions should average under 300ms
        assertTrue("Average navigation transition should be under 300ms, was ${averageTime}ms", 
            averageTime < 300)
    }

    @Test
    fun testNavigationMemoryUsage() {
        // Test that navigation doesn't cause excessive memory usage
        val runtime = Runtime.getRuntime()
        val initialMemory = runtime.totalMemory() - runtime.freeMemory()
        
        // Perform extensive navigation to test memory usage
        repeat(10) {
            onView(withId(R.id.discoverFragment)).perform(click())
            onView(withId(R.id.journalFragment)).perform(click())
            onView(withId(R.id.profileFragment)).perform(click())
            onView(withId(R.id.homeFragment)).perform(click())
        }
        
        // Force garbage collection
        System.gc()
        Thread.sleep(100)
        
        val finalMemory = runtime.totalMemory() - runtime.freeMemory()
        val memoryIncrease = finalMemory - initialMemory
        val memoryIncreasePercent = (memoryIncrease.toDouble() / initialMemory) * 100
        
        // Memory usage should not increase by more than 15% (NFR1 requirement)
        assertTrue("Memory usage should not increase by more than 15%, increased by ${memoryIncreasePercent}%", 
            memoryIncreasePercent < 15.0)
    }

    @Test
    fun testNavigationUnderLoad() {
        // Test navigation performance under load conditions
        val startTime = System.currentTimeMillis()
        
        // Simulate rapid navigation (stress test)
        repeat(20) {
            onView(withId(R.id.discoverFragment)).perform(click())
            onView(withId(R.id.journalFragment)).perform(click())
            onView(withId(R.id.profileFragment)).perform(click())
            onView(withId(R.id.homeFragment)).perform(click())
        }
        
        val endTime = System.currentTimeMillis()
        val totalTime = endTime - startTime
        
        // Stress test should complete within 10 seconds
        assertTrue("Navigation stress test should complete within 10000ms, took ${totalTime}ms", 
            totalTime < 10000)
    }

    @Test
    fun testNavigationAnimationPerformance() {
        // Test that navigation animations don't cause frame drops
        // This is a basic test - in production, you'd use more sophisticated frame monitoring
        
        val startTime = System.currentTimeMillis()
        
        // Test smooth transitions between all fragments
        onView(withId(R.id.discoverFragment)).perform(click())
        Thread.sleep(300) // Wait for animation to complete
        onView(withId(R.id.journalFragment)).perform(click())
        Thread.sleep(300)
        onView(withId(R.id.profileFragment)).perform(click())
        Thread.sleep(300)
        onView(withId(R.id.homeFragment)).perform(click())
        Thread.sleep(300)
        
        val endTime = System.currentTimeMillis()
        val totalTime = endTime - startTime
        
        // Animations should complete smoothly within expected time
        assertTrue("Navigation animations should complete within 1500ms, took ${totalTime}ms", 
            totalTime < 1500)
    }

    @Test
    fun testNavigationConcurrentOperations() {
        // Test navigation performance when other operations are running
        // Simulate background work during navigation
        
        val backgroundTask = Thread {
            // Simulate some background processing
            repeat(1000) {
                Math.sqrt(it.toDouble())
            }
        }
        
        backgroundTask.start()
        
        val startTime = System.currentTimeMillis()
        
        // Navigate while background task is running
        onView(withId(R.id.discoverFragment)).perform(click())
        onView(withId(R.id.journalFragment)).perform(click())
        onView(withId(R.id.profileFragment)).perform(click())
        onView(withId(R.id.homeFragment)).perform(click())
        
        val endTime = System.currentTimeMillis()
        val totalTime = endTime - startTime
        
        backgroundTask.join() // Wait for background task to complete
        
        // Navigation should still be responsive even with background work
        assertTrue("Navigation should remain responsive during background operations, took ${totalTime}ms", 
            totalTime < 2000)
    }
}