package com.example.health_assistant.features.discover.domain.usecase

import com.example.health_assistant.core.util.Result
import com.example.health_assistant.features.discover.domain.error.DiscoverErrorHandler
import com.example.health_assistant.features.discover.domain.model.DiscoverContent
import com.example.health_assistant.features.discover.domain.model.HealthContentCategory
import com.example.health_assistant.features.discover.domain.repository.DiscoverRepository
import com.example.health_assistant.features.discover.domain.validation.ContentCredibilityValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Simplified use case for getting content with offline-first approach
 * Implements core business logic for content retrieval with validation and error handling
 */
@Singleton
class SimpleGetContentUseCase @Inject constructor(
    private val repository: DiscoverRepository,
    private val credibilityValidator: ContentCredibilityValidator,
    private val errorHandler: DiscoverErrorHandler
) {

    /**
     * Get mixed content feed with offline-first approach
     */
    fun getMixedContentFeed(
        category: String? = null,
        limit: Int = 30
    ): Flow<Result<List<DiscoverContent>>> {
        return repository.getMixedContentFeed(category, limit)
            .map { result ->
                when (result) {
                    is Result.Success -> {
                        val validatedContent = validateContentList(result.data)
                        Result.Success(validatedContent)
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
     * Get health articles with credibility validation
     */
    fun getHealthArticles(
        category: String? = null,
        limit: Int = 20,
        minCredibilityScore: Int = 1
    ): Flow<Result<List<DiscoverContent.Article>>> {
        return repository.getHealthArticles(category, limit)
            .map { result ->
                when (result) {
                    is Result.Success -> {
                        val filteredArticles = result.data.filter { article ->
                            article.credibilityScore >= minCredibilityScore
                        }
                        Result.Success(filteredArticles)
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
     * Get health news with breaking news prioritization
     */
    fun getHealthNews(
        category: String? = null,
        limit: Int = 10
    ): Flow<Result<List<DiscoverContent.News>>> {
        return repository.getHealthNews(category, limit)
            .map { result ->
                when (result) {
                    is Result.Success -> {
                        val sortedNews = result.data.sortedWith(
                            compareByDescending<DiscoverContent.News> { it.isBreakingNews }
                                .thenByDescending { it.publishedDate }
                        )
                        Result.Success(sortedNews)
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
     * Get educational videos with difficulty filtering
     */
    fun getEducationalVideos(
        category: String? = null,
        limit: Int = 15,
        difficultyLevel: String? = null
    ): Flow<Result<List<DiscoverContent.Video>>> {
        return repository.getEducationalVideos(category, limit)
            .map { result ->
                when (result) {
                    is Result.Success -> {
                        val filteredVideos = if (difficultyLevel != null) {
                            result.data.filter { video ->
                                video.difficultyLevel.equals(difficultyLevel, ignoreCase = true)
                            }
                        } else {
                            result.data
                        }
                        Result.Success(filteredVideos)
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
     * Get bookmarked content
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
     * Validate content list for credibility and integrity
     */
    private fun validateContentList(contentList: List<DiscoverContent>): List<DiscoverContent> {
        return contentList.mapNotNull { content ->
            val integrityResult = errorHandler.validateContentIntegrity(content)
            if (integrityResult is com.example.health_assistant.features.discover.domain.error.ContentIntegrityResult.Valid) {
                content
            } else {
                null // Filter out invalid content
            }
        }
    }
}