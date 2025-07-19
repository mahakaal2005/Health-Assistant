package com.example.health_assistant.features.discover.domain.usecase

import com.example.health_assistant.core.util.Result
import com.example.health_assistant.features.discover.domain.error.DiscoverErrorHandler
import com.example.health_assistant.features.discover.domain.model.DiscoverContent
import com.example.health_assistant.features.discover.domain.model.HealthContentCategory
import com.example.health_assistant.features.discover.domain.repository.DiscoverRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Simplified use case for content search and filtering operations
 * Implements core business logic for content discovery
 */
@Singleton
class SimpleSearchUseCase @Inject constructor(
    private val repository: DiscoverRepository,
    private val errorHandler: DiscoverErrorHandler
) {

    /**
     * Search content with basic filtering
     */
    suspend fun searchContent(
        query: String,
        contentTypes: List<String> = listOf("article", "news", "video"),
        limit: Int = 50
    ): Result<List<DiscoverContent>> {
        return try {
            if (query.isBlank()) {
                return Result.Error(Exception("Search query cannot be empty"))
            }

            if (query.length < 2) {
                return Result.Error(Exception("Search query must be at least 2 characters"))
            }

            val searchResult = repository.searchContent(
                query = query.trim(),
                contentTypes = contentTypes,
                limit = limit
            )

            when (searchResult) {
                is Result.Success -> {
                    val rankedResults = rankSearchResults(searchResult.data, query)
                    Result.Success(rankedResults)
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
     * Get search suggestions based on partial query
     */
    suspend fun getSearchSuggestions(
        partialQuery: String,
        limit: Int = 10
    ): Result<List<String>> {
        return try {
            if (partialQuery.length < 2) {
                return Result.Success(getPopularSearchTerms(limit))
            }

            val suggestions = mutableListOf<String>()
            
            HealthContentCategory.values().forEach { category ->
                if (category.displayName.contains(partialQuery, ignoreCase = true) ||
                    category.key.contains(partialQuery, ignoreCase = true)) {
                    suggestions.add(category.displayName)
                }
            }
            
            // Add common health terms that match the query
            val commonTerms = getCommonHealthTerms()
            commonTerms.filter { it.contains(partialQuery, ignoreCase = true) }
                .take(limit - suggestions.size)
                .forEach { suggestions.add(it) }
            
            Result.Success(suggestions.take(limit))
        } catch (e: Exception) {
            Result.Error(Exception("Failed to get search suggestions: ${e.message}"))
        }
    }

    /**
     * Filter content by category
     */
    suspend fun filterByCategory(
        category: HealthContentCategory,
        limit: Int = 30
    ): Result<List<DiscoverContent>> {
        return try {
            val contentResult = repository.getContentByCategory(category, limit)
            
            when (contentResult) {
                is Result.Success -> {
                    val sortedContent = contentResult.data.sortedByDescending { it.publishedDate }
                    Result.Success(sortedContent.take(limit))
                }
                is Result.Error -> contentResult
                is Result.Loading -> contentResult
            }
        } catch (e: Exception) {
            val error = errorHandler.handleContentLoadError(e)
            Result.Error(Exception(error.message))
        }
    }

    /**
     * Get trending content
     */
    fun getTrendingContent(
        limit: Int = 20
    ): Flow<Result<List<DiscoverContent>>> {
        return repository.getTrendingContent(limit)
            .map { result ->
                when (result) {
                    is Result.Success -> {
                        val recentContent = filterRecentContent(result.data)
                        Result.Success(recentContent)
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
     * Filter content by credibility score
     */
    suspend fun filterByCredibility(
        contentList: List<DiscoverContent>,
        minCredibilityScore: Int = 3
    ): Result<List<DiscoverContent>> {
        return try {
            val credibleContent = contentList.filter { content ->
                when (content) {
                    is DiscoverContent.Article -> content.credibilityScore >= minCredibilityScore
                    else -> true // Include non-article content by default
                }
            }
            
            Result.Success(credibleContent)
        } catch (e: Exception) {
            Result.Error(Exception("Credibility filtering failed: ${e.message}"))
        }
    }

    /**
     * Rank search results by relevance
     */
    private fun rankSearchResults(
        content: List<DiscoverContent>,
        query: String
    ): List<DiscoverContent> {
        val queryTerms = query.lowercase().split("\\s+".toRegex())
        
        return content.sortedByDescending { item ->
            calculateRelevanceScore(item, queryTerms)
        }
    }

    /**
     * Calculate relevance score for search ranking
     */
    private fun calculateRelevanceScore(content: DiscoverContent, queryTerms: List<String>): Double {
        var score = 0.0
        val title = content.title.lowercase()
        val summary = content.getContentSummary().lowercase()

        queryTerms.forEach { term ->
            // Title matches get higher score
            if (title.contains(term)) {
                score += if (title.startsWith(term)) 10.0 else 5.0
            }
            
            // Summary matches get medium score
            if (summary.contains(term)) {
                score += 2.0
            }
            
            // Category matches get lower score
            if (content.category.lowercase().contains(term)) {
                score += 1.0
            }
        }

        // Boost score based on credibility (for articles)
        if (content is DiscoverContent.Article) {
            score *= (1.0 + content.credibilityScore * 0.1)
        }

        // Boost recent content
        val daysSincePublished = (System.currentTimeMillis() - content.publishedDate) / (24 * 60 * 60 * 1000)
        if (daysSincePublished < 30) {
            score *= 1.2
        }

        return score
    }

    /**
     * Filter content to show only recent items
     */
    private fun filterRecentContent(content: List<DiscoverContent>): List<DiscoverContent> {
        val cutoffTime = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L) // 7 days
        return content.filter { it.publishedDate >= cutoffTime }
    }

    /**
     * Get popular search terms
     */
    private fun getPopularSearchTerms(limit: Int): List<String> {
        return listOf(
            "nutrition", "exercise", "mental health", "diabetes", "heart health",
            "weight loss", "sleep", "stress", "vitamins", "meditation",
            "fitness", "diet", "wellness", "prevention", "symptoms"
        ).take(limit)
    }

    /**
     * Get common health terms for suggestions
     */
    private fun getCommonHealthTerms(): List<String> {
        return listOf(
            "blood pressure", "cholesterol", "immune system", "metabolism",
            "inflammation", "antioxidants", "probiotics", "hydration",
            "protein", "carbohydrates", "fiber", "calcium", "iron",
            "vitamin D", "omega-3", "anxiety", "depression", "mindfulness",
            "yoga", "cardio", "strength training", "flexibility"
        )
    }
}