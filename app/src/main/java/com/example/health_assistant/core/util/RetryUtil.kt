package com.example.health_assistant.core.util

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.retryWhen
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Utility object for implementing retry logic across the app
 */
object RetryUtil {

    /**
     * Default retry configuration for network operations
     */
    const val DEFAULT_MAX_RETRIES = 3
    const val DEFAULT_INITIAL_DELAY = 1000L // 1 second
    const val DEFAULT_MULTIPLIER = 2.0
    const val DEFAULT_MAX_DELAY = 10000L // 10 seconds

    /**
     * Determines if an exception is retryable (network-related errors)
     */
    fun isRetryableException(exception: Throwable): Boolean {
        return when (exception) {
            is IOException,
            is SocketTimeoutException,
            is UnknownHostException -> true
            else -> false
        }
    }

    /**
     * Implements exponential backoff retry logic for suspend functions
     */
    suspend inline fun <T> retryWithBackoff(
        maxRetries: Int = DEFAULT_MAX_RETRIES,
        initialDelay: Long = DEFAULT_INITIAL_DELAY,
        multiplier: Double = DEFAULT_MULTIPLIER,
        maxDelay: Long = DEFAULT_MAX_DELAY,
        shouldRetry: (Throwable) -> Boolean = ::isRetryableException,
        crossinline operation: suspend () -> T
    ): T {
        var currentDelay = initialDelay
        repeat(maxRetries) { attempt ->
            try {
                return operation()
            } catch (e: Exception) {
                // Don't retry on the last attempt or if exception isn't retryable
                if (attempt == maxRetries - 1 || !shouldRetry(e)) {
                    throw e
                }

                delay(currentDelay)
                currentDelay = (currentDelay * multiplier).toLong().coerceAtMost(maxDelay)
            }
        }
        // This should never be reached due to throw in catch block
        return operation()
    }
}

/**
 * Extension function to add retry logic to Flows
 */
fun <T> Flow<T>.retryWithExponentialBackoff(
    maxRetries: Int = RetryUtil.DEFAULT_MAX_RETRIES,
    initialDelay: Long = RetryUtil.DEFAULT_INITIAL_DELAY,
    multiplier: Double = RetryUtil.DEFAULT_MULTIPLIER,
    maxDelay: Long = RetryUtil.DEFAULT_MAX_DELAY,
    shouldRetry: (Throwable) -> Boolean = RetryUtil::isRetryableException
): Flow<T> {
    var currentDelay = initialDelay
    var attemptCount = 0

    return retryWhen { cause, _ ->
        if (attemptCount >= maxRetries || !shouldRetry(cause)) {
            false
        } else {
            attemptCount++
            delay(currentDelay)
            currentDelay = (currentDelay * multiplier).toLong().coerceAtMost(maxDelay)
            true
        }
    }
}