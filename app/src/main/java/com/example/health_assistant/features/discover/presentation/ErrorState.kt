package com.example.health_assistant.features.discover.presentation

import com.example.health_assistant.features.discover.domain.error.DiscoverError

/**
 * UI state for error handling in Discover feature
 */
sealed class ErrorState {
    object None : ErrorState()
    
    data class Error(
        val discoverError: DiscoverError,
        val isRetrying: Boolean = false,
        val retryAttempt: Int = 0
    ) : ErrorState()
    
    data class PartialError(
        val discoverError: DiscoverError.PartialContentError,
        val isRetrying: Boolean = false
    ) : ErrorState()
    
    data class NetworkOffline(
        val hasCache: Boolean = false
    ) : ErrorState()
}

/**
 * UI state for content reporting
 */
sealed class ReportState {
    object None : ReportState()
    object Loading : ReportState()
    object Success : ReportState()
    data class Error(val message: String) : ReportState()
}

/**
 * Data class for retry operation state
 */
data class RetryState(
    val isRetrying: Boolean = false,
    val retryAttempt: Int = 0,
    val maxAttempts: Int = 3,
    val nextRetryDelayMs: Long = 0L
) {
    val canRetry: Boolean get() = retryAttempt < maxAttempts
    val isLastAttempt: Boolean get() = retryAttempt >= maxAttempts - 1
}