package com.example.health_assistant.features.discover.domain.error

/**
 * Sealed class representing different types of errors that can occur in the Discover feature
 */
sealed class DiscoverError(
    val message: String,
    val userMessage: String,
    val isRetryable: Boolean = true,
    val cause: Throwable? = null
) {
    
    // Network-related errors
    data class NetworkError(
        val errorMessage: String = "Network connection failed",
        val throwable: Throwable? = null
    ) : DiscoverError(
        message = errorMessage,
        userMessage = "Unable to connect to the internet. Please check your connection and try again.",
        isRetryable = true,
        cause = throwable
    )
    
    data class ServerError(
        val errorMessage: String = "Server error occurred",
        val throwable: Throwable? = null
    ) : DiscoverError(
        message = errorMessage,
        userMessage = "Our servers are experiencing issues. Please try again in a few moments.",
        isRetryable = true,
        cause = throwable
    )
    
    data class TimeoutError(
        val errorMessage: String = "Request timed out",
        val throwable: Throwable? = null
    ) : DiscoverError(
        message = errorMessage,
        userMessage = "The request is taking too long. Please try again.",
        isRetryable = true,
        cause = throwable
    )
    
    // Content-related errors
    data class ContentNotFoundError(
        val contentId: String,
        val contentType: String
    ) : DiscoverError(
        message = "Content not found: $contentType with id $contentId",
        userMessage = "The requested content is no longer available.",
        isRetryable = false
    )
    
    data class ContentLoadError(
        val errorMessage: String = "Failed to load content",
        val throwable: Throwable? = null
    ) : DiscoverError(
        message = errorMessage,
        userMessage = "Unable to load content. Please try refreshing.",
        isRetryable = true,
        cause = throwable
    )
    
    data class PartialContentError(
        val loadedCount: Int,
        val totalCount: Int,
        val failedTypes: List<String>
    ) : DiscoverError(
        message = "Partial content load: $loadedCount/$totalCount loaded, failed: $failedTypes",
        userMessage = "Some content couldn't be loaded. Showing available content.",
        isRetryable = true
    )
    
    // Database/Storage errors
    data class DatabaseError(
        val errorMessage: String = "Database operation failed",
        val throwable: Throwable? = null
    ) : DiscoverError(
        message = errorMessage,
        userMessage = "Unable to save or retrieve data. Please try again.",
        isRetryable = true,
        cause = throwable
    )
    
    data class CacheError(
        val errorMessage: String = "Cache operation failed",
        val throwable: Throwable? = null
    ) : DiscoverError(
        message = errorMessage,
        userMessage = "Unable to access offline content. Please check your storage.",
        isRetryable = false,
        cause = throwable
    )
    
    // Video/Media errors
    data class VideoPlaybackError(
        val videoId: String,
        val errorMessage: String = "Video playback failed",
        val throwable: Throwable? = null
    ) : DiscoverError(
        message = errorMessage,
        userMessage = "Unable to play video. Please try again or check your connection.",
        isRetryable = true,
        cause = throwable
    )
    
    data class MediaDownloadError(
        val mediaId: String,
        val errorMessage: String = "Media download failed",
        val throwable: Throwable? = null
    ) : DiscoverError(
        message = errorMessage,
        userMessage = "Unable to download media for offline viewing.",
        isRetryable = true,
        cause = throwable
    )
    
    // Search/Filter errors
    data class SearchError(
        val query: String,
        val errorMessage: String = "Search failed",
        val throwable: Throwable? = null
    ) : DiscoverError(
        message = errorMessage,
        userMessage = "Unable to search content. Please try again.",
        isRetryable = true,
        cause = throwable
    )
    
    // Sync errors
    data class SyncError(
        val errorMessage: String = "Content sync failed",
        val throwable: Throwable? = null
    ) : DiscoverError(
        message = errorMessage,
        userMessage = "Unable to sync latest content. Showing cached content.",
        isRetryable = true,
        cause = throwable
    )
    
    // Generic/Unknown errors
    data class UnknownError(
        val errorMessage: String = "An unexpected error occurred",
        val throwable: Throwable? = null
    ) : DiscoverError(
        message = errorMessage,
        userMessage = "Something went wrong. Please try again.",
        isRetryable = true,
        cause = throwable
    )
}