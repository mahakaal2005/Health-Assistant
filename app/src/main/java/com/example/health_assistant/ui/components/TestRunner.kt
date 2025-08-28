package com.example.health_assistant.ui.components

import android.content.Context
import android.util.Log

/**
 * Test runner for premium bottom navigation components
 * Can be called from debug builds or development settings
 */
object TestRunner {
    private const val TAG = "TestRunner"
    
    fun runAllTests(context: Context): PremiumBottomNavTestSuite.TestSuiteResult {
        Log.i(TAG, "Starting premium bottom navigation test suite")
        
        val testSuite = PremiumBottomNavTestSuite(context)
        val result = testSuite.runComprehensiveTests()
        
        // Log summary
        Log.i(TAG, "Test suite completed:")
        Log.i(TAG, "  Total tests: ${result.totalTests}")
        Log.i(TAG, "  Passed: ${result.passedTests}")
        Log.i(TAG, "  Failed: ${result.failedTests}")
        Log.i(TAG, "  Execution time: ${result.totalExecutionTimeMs}ms")
        
        if (result.failedTests > 0) {
            Log.w(TAG, "Failed tests:")
            result.results.filter { !it.passed }.forEach { test ->
                Log.w(TAG, "  - ${test.testName}: ${test.errorMessage}")
            }
        }
        
        return result
    }
    
    fun runPerformanceTests(context: Context): List<PremiumBottomNavTestSuite.TestResult> {
        Log.i(TAG, "Running performance tests only")
        
        val testSuite = PremiumBottomNavTestSuite(context)
        val allResults = testSuite.runComprehensiveTests()
        
        // Filter performance-related tests
        val performanceTests = allResults.results.filter { result ->
            result.testName.contains("Performance") ||
            result.testName.contains("Memory") ||
            result.testName.contains("Frame Rate") ||
            result.testName.contains("Animation")
        }
        
        Log.i(TAG, "Performance tests completed: ${performanceTests.count { it.passed }}/${performanceTests.size} passed")
        
        return performanceTests
    }
    
    fun runAccessibilityTests(context: Context): List<PremiumBottomNavTestSuite.TestResult> {
        Log.i(TAG, "Running accessibility tests only")
        
        val testSuite = PremiumBottomNavTestSuite(context)
        val allResults = testSuite.runComprehensiveTests()
        
        // Filter accessibility-related tests
        val accessibilityTests = allResults.results.filter { result ->
            result.testName.contains("Accessibility") ||
            result.testName.contains("TalkBack") ||
            result.testName.contains("High Contrast") ||
            result.testName.contains("Large Text")
        }
        
        Log.i(TAG, "Accessibility tests completed: ${accessibilityTests.count { it.passed }}/${accessibilityTests.size} passed")
        
        return accessibilityTests
    }
}