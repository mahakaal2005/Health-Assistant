package com.example.health_assistant.features.discover.domain.error

import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class RetryManagerTest {
    
    private lateinit var retryManager: RetryManager
    
    @Before
    fun setUp() {
        retryManager = RetryManager()
    }
    
    @Test
    fun `executeWithRetry succeeds on first attempt`() = runTest {
        // Given
        var attemptCount = 0
        val operation: suspend (Int) -> String = { attempt ->
            attemptCount = attempt
            "Success"
        }
        
        // When
        val result = retryManager.executeWithRetry(operation = operation)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals("Success", result.getOrNull())
        assertEquals(1, attemptCount)
    }
    
    @Test
    fun `executeWithRetry succeeds on second attempt`() = runTest {
        // Given
        var attemptCount = 0
        val operation: suspend (Int) -> String = { attempt ->
            attemptCount = attempt
            if (attempt == 1) {
                throw RuntimeException("First attempt fails")
            } else {
                "Success on retry"
            }
        }
        
        // When
        val result = retryManager.executeWithRetry(operation = operation)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals("Success on retry", result.getOrNull())
        assertEquals(2, attemptCount)
    }
    
    @Test
    fun `executeWithRetry fails after max attempts`() = runTest {
        // Given
        var attemptCount = 0
        val operation: suspend (Int) -> String = { attempt ->
            attemptCount = attempt
            throw RuntimeException("Always fails")
        }
        
        // When
        val result = retryManager.executeWithRetry(maxAttempts = 3, operation = operation)
        
        // Then
        assertTrue(result.isFailure)
        assertEquals("Always fails", result.exceptionOrNull()?.message)
        assertEquals(3, attemptCount)
    }
    
    @Test
    fun `isRetryable returns correct value for different error types`() {
        // Given
        val networkError = DiscoverError.NetworkError()
        val contentNotFoundError = DiscoverError.ContentNotFoundError("123", "article")
        val serverError = DiscoverError.ServerError()
        val cacheError = DiscoverError.CacheError()
        
        // When & Then
        assertTrue(retryManager.isRetryable(networkError))
        assertFalse(retryManager.isRetryable(contentNotFoundError))
        assertTrue(retryManager.isRetryable(serverError))
        assertFalse(retryManager.isRetryable(cacheError))
    }
    
    @Test
    fun `getRetryDelay returns appropriate delays for different error types`() {
        // Given
        val networkError = DiscoverError.NetworkError()
        val serverError = DiscoverError.ServerError()
        val timeoutError = DiscoverError.TimeoutError()
        val unknownError = DiscoverError.UnknownError()
        
        // When
        val networkDelay = retryManager.getRetryDelay(networkError, 0)
        val serverDelay = retryManager.getRetryDelay(serverError, 0)
        val timeoutDelay = retryManager.getRetryDelay(timeoutError, 0)
        val unknownDelay = retryManager.getRetryDelay(unknownError, 0)
        
        // Then
        assertTrue(networkDelay > 0)
        assertTrue(serverDelay > networkDelay) // Server errors have longer delays
        assertTrue(timeoutDelay > 0)
        assertEquals(1000L, unknownDelay) // Base delay for unknown errors
    }
    
    @Test
    fun `getRetryDelay increases with attempt number`() {
        // Given
        val networkError = DiscoverError.NetworkError()
        
        // When
        val delay0 = retryManager.getRetryDelay(networkError, 0)
        val delay1 = retryManager.getRetryDelay(networkError, 1)
        val delay2 = retryManager.getRetryDelay(networkError, 2)
        
        // Then
        assertTrue(delay1 > delay0)
        assertTrue(delay2 > delay1)
    }
}