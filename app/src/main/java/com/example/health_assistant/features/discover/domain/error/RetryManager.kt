package com.example.health_assistant.features.discover.domain.error

import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min
import kotlin.math.pow

/**
 * Manages retry logic with exponential backoff for failed operations
 */
@Singleton
class RetryManager @Inject constructor() {
    
    companion object {
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val BASE_DELAY_MS = 1000L
        private const val MAX_DELAY_MS = 10000L
        private const val BACKOFF_MULTIPLIER = 2.0
    }
    
    /**
     * Executes an operation with retry logic and exponential backoff
     */
    suspend fun <T> executeWithRetry(
        maxAttempts: Int = MAX_RETRY_ATTEMPTS,
        operation: suspend (attempt: Int) -> T
    ): Result<T> {
        var lastException: Exception? = null
        
        repeat(maxAttempts) { attempt ->
            try {
                val result = operation(attempt + 1)
                return Result.success(result)
            } catch (e: Exception) {
                lastException = e
                
                // Don't retry on the last attempt
                if (attempt < maxAttempts - 1) {
                    val delayMs = calculateBackoffDelay(attempt)
                    delay(delayMs)
                }
            }
        }
        
        return Result.failure(lastException ?: Exception("Unknown retry failure"))
    }
    
    /**
     * Calculates exponential backoff delay with jitter
     */
    private fun calculateBackoffDelay(attempt: Int): Long {
        val exponentialDelay = BASE_DELAY_MS * BACKOFF_MULTIPLIER.pow(attempt).toLong()
        val cappedDelay = min(exponentialDelay, MAX_DELAY_MS)
        
        // Add jitter (±25% randomization) to prevent thundering herd
        val jitterRange = (cappedDelay * 0.25).toLong()
        val jitter = (-jitterRange..jitterRange).random()
        
        return cappedDelay + jitter
    }
    
    /**
     * Determines if an error is retryable based on its type
     */
    fun isRetryable(error: DiscoverError): Boolean {
        return error.isRetryable
    }
    
    /**
     * Gets appropriate retry delay for a specific error type
     */
    fun getRetryDelay(error: DiscoverError, attempt: Int): Long {
        return when (error) {
            is DiscoverError.NetworkError -> calculateBackoffDelay(attempt)
            is DiscoverError.ServerError -> calculateBackoffDelay(attempt) * 2 // Longer delay for server errors
            is DiscoverError.TimeoutError -> calculateBackoffDelay(attempt)
            is DiscoverError.SyncError -> calculateBackoffDelay(attempt)
            else -> BASE_DELAY_MS
        }
    }
}