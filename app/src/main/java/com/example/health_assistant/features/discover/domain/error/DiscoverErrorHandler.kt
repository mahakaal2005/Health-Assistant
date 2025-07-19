package com.example.health_assistant.features.discover.domain.error

import com.example.health_assistant.core.util.Result
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow

/**
 * Centralized error handling for Discover section
 * Implements retry logic, error categorization, and user-friendly error messages
 */
@Singleton
class DiscoverErrorHandler @Inject constructor() {

    companion object {
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val BASE_DELAY_MS = 1000L
        private const val MAX_DELAY_MS = 30000L
    }

    /**
     * Handle content loading errors with appropriate user messages
     */
    fun handleContentLoadError(error: Throwable): DiscoverError {
        return when (error) {
            is NetworkException -> DiscoverError.NetworkError(
                errorMessage = "Unable to connect to the internet. Showing cached content.",
                throwable = error
            )
            is ContentNotFoundException -> DiscoverError.ContentNotFoundError(
                contentId = "unknown",
                contentType = "content"
            )
            is FirebaseException -> DiscoverError.SyncError(
                errorMessage = "Unable to sync latest content. Showing cached content.",
                throwable = error
            )
            is ContentValidationException -> DiscoverError.ContentLoadError(
                errorMessage = "Content failed quality validation.",
                throwable = error
            )
            is CacheException -> DiscoverError.CacheError(
                errorMessage = "Unable to access cached content.",
                throwable = error
            )
            is AuthenticationException -> DiscoverError.ServerError(
                errorMessage = "Please sign in to access personalized content.",
                throwable = error
            )
            is RateLimitException -> DiscoverError.ServerError(
                errorMessage = "Too many requests. Please try again later.",
                throwable = error
            )
            else -> DiscoverError.UnknownError(
                errorMessage = "Something went wrong. Please try again.",
                throwable = error
            )
        }
    }

    /**
     * Handle video playback errors
     */
    fun handleVideoPlaybackError(error: Throwable): VideoPlaybackError {
        return when (error) {
            is NetworkException -> VideoPlaybackError.NetworkRequired(
                message = "Internet connection required for video playback."
            )
            is StorageException -> VideoPlaybackError.DownloadFailed(
                message = "Failed to download video for offline viewing."
            )
            is VideoFormatException -> VideoPlaybackError.UnsupportedFormat(
                message = "Video format not supported on this device."
            )
            else -> VideoPlaybackError.PlaybackFailed(
                message = "Unable to play video. Please try again."
            )
        }
    }

    /**
     * Execute operation with exponential backoff retry logic
     */
    suspend fun <T> executeWithRetry(
        operation: suspend () -> Result<T>,
        maxAttempts: Int = MAX_RETRY_ATTEMPTS,
        shouldRetry: (Throwable) -> Boolean = ::isRetryableError
    ): Result<T> {
        var lastException: Throwable? = null
        
        repeat(maxAttempts) { attempt ->
            try {
                val result = operation()
                if (result is Result.Success) {
                    return result
                } else if (result is Result.Error && result.exception != null && !shouldRetry(result.exception)) {
                    return result
                }
                lastException = (result as? Result.Error)?.exception
            } catch (e: Exception) {
                lastException = e
                if (!shouldRetry(e)) {
                    return Result.Error(e)
                }
            }
            
            if (attempt < maxAttempts - 1) {
                val delayMs = calculateBackoffDelay(attempt)
                delay(delayMs)
            }
        }
        
        return Result.Error(
            lastException ?: Exception("Operation failed after $maxAttempts attempts")
        )
    }

    /**
     * Calculate exponential backoff delay with jitter
     */
    private fun calculateBackoffDelay(attempt: Int): Long {
        val exponentialDelay = BASE_DELAY_MS * (2.0.pow(attempt)).toLong()
        val jitter = (Math.random() * 0.1 * exponentialDelay).toLong()
        return minOf(exponentialDelay + jitter, MAX_DELAY_MS)
    }

    /**
     * Determine if an error is retryable
     */
    private fun isRetryableError(error: Throwable): Boolean {
        return when (error) {
            is NetworkException -> true
            is FirebaseException -> true
            is TimeoutException -> true
            is CacheException -> true
            is RateLimitException -> true
            is ContentNotFoundException -> false
            is ContentValidationException -> false
            is AuthenticationException -> false
            else -> false
        }
    }

    /**
     * Handle data inconsistency errors
     */
    fun handleDataInconsistency(
        localData: Any?,
        remoteData: Any?,
        contentId: String
    ): DataInconsistencyResolution {
        return when {
            localData == null && remoteData != null -> {
                DataInconsistencyResolution.UseRemote(
                    message = "Using latest version from server",
                    action = DataInconsistencyAction.REPLACE_LOCAL
                )
            }
            localData != null && remoteData == null -> {
                DataInconsistencyResolution.UseLocal(
                    message = "Content no longer available remotely",
                    action = DataInconsistencyAction.KEEP_LOCAL
                )
            }
            localData != null && remoteData != null -> {
                // Compare timestamps or versions if available
                DataInconsistencyResolution.Merge(
                    message = "Merging local and remote changes",
                    action = DataInconsistencyAction.MERGE_CHANGES
                )
            }
            else -> {
                DataInconsistencyResolution.Error(
                    message = "Content not available locally or remotely",
                    action = DataInconsistencyAction.REPORT_ERROR
                )
            }
        }
    }

    /**
     * Validate content integrity
     */
    fun validateContentIntegrity(content: Any): ContentIntegrityResult {
        return try {
            when (content) {
                is com.example.health_assistant.features.discover.domain.model.DiscoverContent.Article -> {
                    validateArticleIntegrity(content)
                }
                is com.example.health_assistant.features.discover.domain.model.DiscoverContent.News -> {
                    validateNewsIntegrity(content)
                }
                is com.example.health_assistant.features.discover.domain.model.DiscoverContent.Video -> {
                    validateVideoIntegrity(content)
                }
                else -> ContentIntegrityResult.Invalid("Unknown content type")
            }
        } catch (e: Exception) {
            ContentIntegrityResult.Invalid("Content validation failed: ${e.message}")
        }
    }

    private fun validateArticleIntegrity(article: com.example.health_assistant.features.discover.domain.model.DiscoverContent.Article): ContentIntegrityResult {
        val issues = mutableListOf<String>()
        
        if (article.id.isBlank()) issues.add("Missing article ID")
        if (article.title.isBlank()) issues.add("Missing article title")
        if (article.content.isBlank()) issues.add("Missing article content")
        if (article.authorName.isBlank()) issues.add("Missing author name")
        if (article.publishedDate <= 0) issues.add("Invalid published date")
        if (article.readingTimeMinutes <= 0) issues.add("Invalid reading time")
        if (article.credibilityScore !in 1..5) issues.add("Invalid credibility score")
        
        return if (issues.isEmpty()) {
            ContentIntegrityResult.Valid
        } else {
            ContentIntegrityResult.Invalid("Article validation failed: ${issues.joinToString(", ")}")
        }
    }

    private fun validateNewsIntegrity(news: com.example.health_assistant.features.discover.domain.model.DiscoverContent.News): ContentIntegrityResult {
        val issues = mutableListOf<String>()
        
        if (news.id.isBlank()) issues.add("Missing news ID")
        if (news.title.isBlank()) issues.add("Missing news title")
        if (news.summary.isBlank()) issues.add("Missing news summary")
        if (news.sourcePublication.isBlank()) issues.add("Missing source publication")
        if (news.publishedDate <= 0) issues.add("Invalid published date")
        if (news.externalUrl.isBlank()) issues.add("Missing external URL")
        
        return if (issues.isEmpty()) {
            ContentIntegrityResult.Valid
        } else {
            ContentIntegrityResult.Invalid("News validation failed: ${issues.joinToString(", ")}")
        }
    }

    private fun validateVideoIntegrity(video: com.example.health_assistant.features.discover.domain.model.DiscoverContent.Video): ContentIntegrityResult {
        val issues = mutableListOf<String>()
        
        if (video.id.isBlank()) issues.add("Missing video ID")
        if (video.title.isBlank()) issues.add("Missing video title")
        if (video.description.isBlank()) issues.add("Missing video description")
        if (video.videoUrl.isBlank()) issues.add("Missing video URL")
        if (video.expertName.isBlank()) issues.add("Missing expert name")
        if (video.publishedDate <= 0) issues.add("Invalid published date")
        if (video.durationSeconds <= 0) issues.add("Invalid video duration")
        
        return if (issues.isEmpty()) {
            ContentIntegrityResult.Valid
        } else {
            ContentIntegrityResult.Invalid("Video validation failed: ${issues.joinToString(", ")}")
        }
    }
}



/**
 * Sealed class for video playback errors
 */
sealed class VideoPlaybackError(open val message: String) {
    data class NetworkRequired(override val message: String) : VideoPlaybackError(message)
    data class DownloadFailed(override val message: String) : VideoPlaybackError(message)
    data class UnsupportedFormat(override val message: String) : VideoPlaybackError(message)
    data class PlaybackFailed(override val message: String) : VideoPlaybackError(message)
}

/**
 * Data inconsistency resolution strategies
 */
sealed class DataInconsistencyResolution(
    open val message: String,
    open val action: DataInconsistencyAction
) {
    data class UseRemote(
        override val message: String,
        override val action: DataInconsistencyAction
    ) : DataInconsistencyResolution(message, action)

    data class UseLocal(
        override val message: String,
        override val action: DataInconsistencyAction
    ) : DataInconsistencyResolution(message, action)

    data class Merge(
        override val message: String,
        override val action: DataInconsistencyAction
    ) : DataInconsistencyResolution(message, action)

    data class Error(
        override val message: String,
        override val action: DataInconsistencyAction
    ) : DataInconsistencyResolution(message, action)
}

enum class DataInconsistencyAction {
    REPLACE_LOCAL,
    KEEP_LOCAL,
    MERGE_CHANGES,
    REPORT_ERROR
}

/**
 * Content integrity validation result
 */
sealed class ContentIntegrityResult {
    object Valid : ContentIntegrityResult()
    data class Invalid(val reason: String) : ContentIntegrityResult()
}

// Custom exceptions for specific error types
class NetworkException(message: String, cause: Throwable? = null) : Exception(message, cause)
class ContentNotFoundException(message: String) : Exception(message)
class FirebaseException(message: String, cause: Throwable? = null) : Exception(message, cause)
class ContentValidationException(message: String) : Exception(message)
class CacheException(message: String, cause: Throwable? = null) : Exception(message, cause)
class AuthenticationException(message: String) : Exception(message)
class RateLimitException(message: String, val retryAfterSeconds: Long) : Exception(message)
class VideoFormatException(message: String) : Exception(message)
class TimeoutException(message: String) : Exception(message)
class StorageException(message: String, cause: Throwable? = null) : Exception(message, cause)