package com.example.health_assistant.features.discover.domain.usecase

import com.example.health_assistant.core.util.Result
import com.example.health_assistant.features.discover.domain.error.DiscoverErrorHandler
import com.example.health_assistant.features.discover.domain.model.ContentIssueType
import com.example.health_assistant.features.discover.domain.model.ContentValidationResult
import com.example.health_assistant.features.discover.domain.model.DiscoverContent
import com.example.health_assistant.features.discover.domain.repository.DiscoverRepository
import com.example.health_assistant.features.discover.domain.validation.ContentCredibilityValidator
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Simplified use case for content validation and credibility management
 * Implements core business logic for content quality assessment and issue reporting
 */
@Singleton
class SimpleContentValidationUseCase @Inject constructor(
    private val repository: DiscoverRepository,
    private val credibilityValidator: ContentCredibilityValidator,
    private val errorHandler: DiscoverErrorHandler
) {

    /**
     * Validate content credibility
     */
    suspend fun validateContentCredibility(
        contentId: String,
        contentType: String
    ): Result<ContentValidationResult> {
        return try {
            repository.validateContentCredibility(contentId, contentType)
        } catch (e: Exception) {
            val error = errorHandler.handleContentLoadError(e)
            Result.Error(Exception(error.message))
        }
    }

    /**
     * Validate article credibility using domain validator
     */
    suspend fun validateArticleCredibility(article: DiscoverContent.Article): Result<ContentValidationResult> {
        return try {
            val validationResult = credibilityValidator.validateArticleCredibility(article)
            Result.Success(validationResult)
        } catch (e: Exception) {
            Result.Error(Exception("Article validation failed: ${e.message}"))
        }
    }

    /**
     * Validate news credibility using domain validator
     */
    suspend fun validateNewsCredibility(news: DiscoverContent.News): Result<ContentValidationResult> {
        return try {
            val validationResult = credibilityValidator.validateNewsCredibility(news)
            Result.Success(validationResult)
        } catch (e: Exception) {
            Result.Error(Exception("News validation failed: ${e.message}"))
        }
    }

    /**
     * Validate video credibility using domain validator
     */
    suspend fun validateVideoCredibility(video: DiscoverContent.Video): Result<ContentValidationResult> {
        return try {
            val validationResult = credibilityValidator.validateVideoCredibility(video)
            Result.Success(validationResult)
        } catch (e: Exception) {
            Result.Error(Exception("Video validation failed: ${e.message}"))
        }
    }

    /**
     * Validate any content type
     */
    suspend fun validateContent(content: DiscoverContent): Result<ContentValidationResult> {
        return when (content) {
            is DiscoverContent.Article -> validateArticleCredibility(content)
            is DiscoverContent.News -> validateNewsCredibility(content)
            is DiscoverContent.Video -> validateVideoCredibility(content)
        }
    }

    /**
     * Get content credibility information
     */
    suspend fun getContentCredibilityInfo(
        contentId: String,
        contentType: String
    ): Result<ContentValidationResult> {
        return try {
            repository.getContentCredibilityInfo(contentId, contentType)
        } catch (e: Exception) {
            val error = errorHandler.handleContentLoadError(e)
            Result.Error(Exception(error.message))
        }
    }

    /**
     * Report content issue
     */
    suspend fun reportContentIssue(
        contentId: String,
        contentType: String,
        issueType: ContentIssueType,
        description: String
    ): Result<Unit> {
        return try {
            // Validate input parameters
            if (contentId.isBlank()) {
                return Result.Error(Exception("Content ID cannot be empty"))
            }
            if (description.isBlank()) {
                return Result.Error(Exception("Issue description cannot be empty"))
            }
            if (description.length < 10) {
                return Result.Error(Exception("Issue description must be at least 10 characters"))
            }

            repository.reportContentIssue(
                contentId = contentId,
                contentType = contentType,
                issueType = issueType.name,
                description = description
            )
        } catch (e: Exception) {
            val error = errorHandler.handleContentLoadError(e)
            Result.Error(Exception(error.message))
        }
    }

    /**
     * Filter content by credibility score
     */
    suspend fun filterContentByCredibility(
        contentList: List<DiscoverContent>,
        minCredibilityScore: Int = 3
    ): Result<List<DiscoverContent>> {
        return try {
            val validatedContent = mutableListOf<DiscoverContent>()

            contentList.forEach { content ->
                when (val validationResult = validateContent(content)) {
                    is Result.Success -> {
                        if (validationResult.data.credibilityScore >= minCredibilityScore) {
                            validatedContent.add(content)
                        }
                    }
                    is Result.Error -> {
                        // Log error but continue processing other content
                    }
                    is Result.Loading -> {
                        // Skip loading content for now
                    }
                }
            }

            Result.Success(validatedContent)
        } catch (e: Exception) {
            Result.Error(Exception("Content filtering failed: ${e.message}"))
        }
    }
}