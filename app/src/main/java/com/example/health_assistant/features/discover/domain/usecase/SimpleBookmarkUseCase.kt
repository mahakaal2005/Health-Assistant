package com.example.health_assistant.features.discover.domain.usecase

import com.example.health_assistant.core.util.Result
import com.example.health_assistant.features.discover.domain.error.DiscoverErrorHandler
import com.example.health_assistant.features.discover.domain.model.DiscoverContent
import com.example.health_assistant.features.discover.domain.repository.DiscoverRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Simplified use case for managing bookmarks and reading progress
 * Implements core business logic for bookmark operations with error handling
 */
@Singleton
class SimpleBookmarkUseCase @Inject constructor(
    private val repository: DiscoverRepository,
    private val errorHandler: DiscoverErrorHandler
) {

    /**
     * Toggle bookmark status for content
     */
    suspend fun toggleBookmark(
        contentId: String,
        contentType: String
    ): Result<Boolean> {
        return try {
            repository.toggleBookmark(contentId, contentType)
        } catch (e: Exception) {
            val error = errorHandler.handleContentLoadError(e)
            Result.Error(Exception(error.message))
        }
    }

    /**
     * Add bookmark for content
     */
    suspend fun addBookmark(
        content: DiscoverContent
    ): Result<Unit> {
        return try {
            // Validate content integrity before bookmarking
            val integrityResult = errorHandler.validateContentIntegrity(content)
            if (integrityResult is com.example.health_assistant.features.discover.domain.error.ContentIntegrityResult.Invalid) {
                return Result.Error(Exception("Cannot bookmark invalid content: ${integrityResult.reason}"))
            }

            repository.addBookmark(content.id, content.getContentType())
        } catch (e: Exception) {
            val error = errorHandler.handleContentLoadError(e)
            Result.Error(Exception(error.message))
        }
    }

    /**
     * Remove bookmark for content
     */
    suspend fun removeBookmark(contentId: String): Result<Unit> {
        return try {
            repository.removeBookmark(contentId)
        } catch (e: Exception) {
            val error = errorHandler.handleContentLoadError(e)
            Result.Error(Exception(error.message))
        }
    }

    /**
     * Get all bookmarked content
     */
    fun getBookmarkedContent(): Flow<Result<List<DiscoverContent>>> {
        return repository.getBookmarkedContent()
            .map { result ->
                when (result) {
                    is Result.Success -> {
                        val sortedBookmarks = result.data.sortedByDescending { content ->
                            content.publishedDate
                        }
                        Result.Success(sortedBookmarks)
                    }
                    is Result.Error -> result
                    is Result.Loading -> result
                }
            }
            .catch { exception ->
                val error = errorHandler.handleContentLoadError(exception)
                emit(Result.Error(Exception(error.message)))
            }
    }

    /**
     * Update reading progress for articles
     */
    suspend fun updateReadingProgress(
        articleId: String,
        progress: Float
    ): Result<Unit> {
        return try {
            val validatedProgress = progress.coerceIn(0f, 1f)
            repository.updateReadingProgress(articleId, validatedProgress)
        } catch (e: Exception) {
            val error = errorHandler.handleContentLoadError(e)
            Result.Error(Exception(error.message))
        }
    }

    /**
     * Update watch progress for videos
     */
    suspend fun updateWatchProgress(
        videoId: String,
        progress: Float
    ): Result<Unit> {
        return try {
            val validatedProgress = progress.coerceIn(0f, 1f)
            repository.updateWatchProgress(videoId, validatedProgress)
        } catch (e: Exception) {
            val error = errorHandler.handleContentLoadError(e)
            Result.Error(Exception(error.message))
        }
    }
}