package com.example.health_assistant.features.discover.domain

import com.example.health_assistant.core.util.Result
import com.example.health_assistant.features.discover.domain.error.DiscoverErrorHandler
import com.example.health_assistant.features.discover.domain.model.DiscoverContent
import com.example.health_assistant.features.discover.domain.model.HealthContentCategory
import com.example.health_assistant.features.discover.domain.repository.DiscoverRepository
import com.example.health_assistant.features.discover.domain.usecase.SimpleBookmarkUseCase
import com.example.health_assistant.features.discover.domain.usecase.SimpleContentValidationUseCase
import com.example.health_assistant.features.discover.domain.usecase.SimpleGetContentUseCase
import com.example.health_assistant.features.discover.domain.usecase.SimpleSearchUseCase
import com.example.health_assistant.features.discover.domain.validation.ContentCredibilityValidator
import com.example.health_assistant.features.discover.domain.model.ContentValidationResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central coordinator for Discover section business logic
 * Orchestrates use cases and provides high-level operations for the presentation layer
 */
@Singleton
class DiscoverManager @Inject constructor(
    private val repository: DiscoverRepository,
    private val getContentUseCase: SimpleGetContentUseCase,
    private val bookmarkUseCase: SimpleBookmarkUseCase,
    private val validationUseCase: SimpleContentValidationUseCase,
    private val searchUseCase: SimpleSearchUseCase,
    private val credibilityValidator: ContentCredibilityValidator,
    private val errorHandler: DiscoverErrorHandler
) {

    /**
     * Get comprehensive content feed with all content types
     * Implements offline-first pattern with automatic validation
     */
    fun getContentFeed(
        category: HealthContentCategory? = null,
        includeValidation: Boolean = true
    ): Flow<Result<DiscoverFeedData>> {
        return flow {
            emit(Result.Loading)
            
            try {
                val categoryKey = category?.key
                
                // Combine all content types
                combine(
                    repository.getHealthArticles(categoryKey, 10),
                    repository.getHealthNews(categoryKey, 5),
                    repository.getEducationalVideos(categoryKey, 8)
                ) { articlesResult, newsResult, videosResult ->
                    
                    val feedData = DiscoverFeedData(
                        articles = (articlesResult as? Result.Success)?.data ?: emptyList(),
                        news = (newsResult as? Result.Success)?.data ?: emptyList(),
                        videos = (videosResult as? Result.Success)?.data ?: emptyList(),
                        hasErrors = listOf(articlesResult, newsResult, videosResult).any { it is Result.Error },
                        lastUpdated = System.currentTimeMillis()
                    )
                    
                    if (includeValidation) {
                        validateFeedContent(feedData)
                    } else {
                        Result.Success(feedData)
                    }
                }.collect { result ->
                    emit(result)
                }
            } catch (e: Exception) {
                val error = errorHandler.handleContentLoadError(e)
                emit(Result.Error(Exception(error.message)))
            }
        }
    }

    /**
     * Get personalized content recommendations
     */
    fun getPersonalizedFeed(): Flow<Result<DiscoverFeedData>> {
        return flow {
            emit(Result.Loading)
            
            try {
                // Get user's preferred categories (this would come from user preferences)
                val preferredCategories = getDefaultCategories()
                
                // Get mixed content based on preferences
                val mixedContent = repository.getMixedContentFeed(
                    category = null, // Get all categories
                    limit = 20
                )
                
                mixedContent.collect { result ->
                    when (result) {
                        is Result.Success -> {
                            val personalizedFeed = createPersonalizedFeed(result.data, preferredCategories)
                            emit(Result.Success(personalizedFeed))
                        }
                        is Result.Error -> emit(result)
                        is Result.Loading -> emit(result)
                    }
                }
            } catch (e: Exception) {
                val error = errorHandler.handleContentLoadError(e)
                emit(Result.Error(Exception(error.message)))
            }
        }
    }

    /**
     * Perform comprehensive content search
     */
    suspend fun searchContent(
        query: String,
        filters: SearchFilters = SearchFilters()
    ): Result<SearchResults> {
        return try {
            val searchResult = repository.searchContent(
                query = query,
                contentTypes = filters.contentTypes,
                limit = filters.limit
            )
            
            when (searchResult) {
                is Result.Success -> {
                    val filteredResults = applySearchFilters(searchResult.data, filters)
                    val searchResults = SearchResults(
                        query = query,
                        results = filteredResults,
                        totalCount = filteredResults.size,
                        appliedFilters = filters
                    )
                    Result.Success(searchResults)
                }
                is Result.Error -> searchResult
                is Result.Loading -> searchResult
            }
        } catch (e: Exception) {
            val error = errorHandler.handleContentLoadError(e)
            Result.Error(Exception(error.message))
        }
    }

    /**
     * Get trending content with engagement metrics
     */
    fun getTrendingContent(limit: Int = 15): Flow<Result<List<DiscoverContent>>> {
        return repository.getTrendingContent(limit)
    }

    /**
     * Manage content bookmarks
     */
    suspend fun toggleBookmark(content: DiscoverContent): Result<Boolean> {
        return repository.toggleBookmark(content.id, content.getContentType())
    }

    /**
     * Get all bookmarked content
     */
    fun getBookmarkedContent(): Flow<Result<List<DiscoverContent>>> {
        return repository.getBookmarkedContent()
    }

    /**
     * Update reading/watching progress
     */
    suspend fun updateProgress(contentId: String, contentType: String, progress: Float): Result<Unit> {
        return when (contentType) {
            "article" -> repository.updateReadingProgress(contentId, progress)
            "video" -> repository.updateWatchProgress(contentId, progress)
            else -> Result.Error(Exception("Unsupported content type for progress tracking"))
        }
    }

    /**
     * Validate content credibility
     */
    suspend fun validateContent(content: DiscoverContent): Result<ContentValidationResult> {
        return repository.validateContentCredibility(content.id, content.getContentType())
    }

    /**
     * Report content issues
     */
    suspend fun reportContentIssue(
        content: DiscoverContent,
        issueType: String,
        description: String
    ): Result<Unit> {
        return repository.reportContentIssue(
            contentId = content.id,
            contentType = content.getContentType(),
            issueType = issueType,
            description = description
        )
    }

    /**
     * Sync content from remote sources
     */
    suspend fun syncContent(): Result<Unit> {
        return try {
            repository.syncContentFromRemote()
        } catch (e: Exception) {
            val error = errorHandler.handleContentLoadError(e)
            Result.Error(Exception(error.message))
        }
    }

    /**
     * Get cache statistics
     */
    suspend fun getCacheStatistics(): Result<com.example.health_assistant.features.discover.domain.repository.CacheStatistics> {
        return try {
            repository.getCacheStatistics()
        } catch (e: Exception) {
            Result.Error(Exception("Failed to get cache statistics: ${e.message}"))
        }
    }

    // Private helper methods

    private suspend fun validateFeedContent(feedData: DiscoverFeedData): Result<DiscoverFeedData> {
        return try {
            val validatedArticles = feedData.articles.filter { article ->
                val validation = repository.validateContentCredibility(article.id, article.getContentType())
                validation is Result.Success && validation.data.isCredible
            }
            
            val validatedFeed = feedData.copy(
                articles = validatedArticles,
                validationApplied = true
            )
            
            Result.Success(validatedFeed)
        } catch (e: Exception) {
            // Return original data if validation fails
            Result.Success(feedData)
        }
    }

    private fun createPersonalizedFeed(
        content: List<DiscoverContent>,
        preferredCategories: List<HealthContentCategory>
    ): DiscoverFeedData {
        val preferredKeys = preferredCategories.map { it.key }
        
        val personalizedContent = content.filter { item ->
            preferredKeys.isEmpty() || item.category in preferredKeys
        }.sortedByDescending { it.publishedDate }
        
        return DiscoverFeedData(
            articles = personalizedContent.filterIsInstance<DiscoverContent.Article>(),
            news = personalizedContent.filterIsInstance<DiscoverContent.News>(),
            videos = personalizedContent.filterIsInstance<DiscoverContent.Video>(),
            hasErrors = false,
            lastUpdated = System.currentTimeMillis(),
            isPersonalized = true
        )
    }

    private fun applySearchFilters(
        results: List<DiscoverContent>,
        filters: SearchFilters
    ): List<DiscoverContent> {
        var filteredResults = results
        
        // Filter by content types
        if (filters.contentTypes.isNotEmpty()) {
            filteredResults = filteredResults.filter { content ->
                content.getContentType() in filters.contentTypes
            }
        }
        
        // Filter by categories
        if (filters.categories.isNotEmpty()) {
            val categoryKeys = filters.categories.map { it.key }
            filteredResults = filteredResults.filter { content ->
                content.category in categoryKeys
            }
        }
        
        return filteredResults.take(filters.limit)
    }

    private fun getDefaultCategories(): List<HealthContentCategory> {
        return listOf(
            HealthContentCategory.GENERAL_HEALTH,
            HealthContentCategory.NUTRITION,
            HealthContentCategory.FITNESS,
            HealthContentCategory.PREVENTIVE_CARE
        )
    }
}

/**
 * Data class representing the complete discover feed
 */
data class DiscoverFeedData(
    val articles: List<DiscoverContent.Article>,
    val news: List<DiscoverContent.News>,
    val videos: List<DiscoverContent.Video>,
    val hasErrors: Boolean,
    val lastUpdated: Long,
    val validationApplied: Boolean = false,
    val isPersonalized: Boolean = false
) {
    val totalItems: Int get() = articles.size + news.size + videos.size
    val isEmpty: Boolean get() = totalItems == 0
    
    fun getAllContent(): List<DiscoverContent> {
        return (articles + news + videos).sortedByDescending { it.publishedDate }
    }
}

/**
 * Data class for search filters
 */
data class SearchFilters(
    val contentTypes: List<String> = listOf("article", "news", "video"),
    val categories: List<HealthContentCategory> = emptyList(),
    val limit: Int = 50
)

/**
 * Data class for search results
 */
data class SearchResults(
    val query: String,
    val results: List<DiscoverContent>,
    val totalCount: Int,
    val appliedFilters: SearchFilters
)

