package com.example.health_assistant.features.discover.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.health_assistant.core.util.Result
import com.example.health_assistant.features.discover.domain.model.DiscoverContent
import com.example.health_assistant.features.discover.domain.model.ContentValidationResult
import com.example.health_assistant.features.discover.domain.repository.DiscoverRepository
import com.example.health_assistant.features.discover.domain.usecase.SimpleContentValidationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for the Article Reader screen
 */
data class ArticleReaderUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val message: String? = null
)

@HiltViewModel
class ArticleReaderViewModel @Inject constructor(
    private val discoverRepository: DiscoverRepository,
    private val contentValidationUseCase: SimpleContentValidationUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ArticleReaderUiState())
    val uiState: StateFlow<ArticleReaderUiState> = _uiState.asStateFlow()

    private val _article = MutableStateFlow<Result<DiscoverContent.Article>>(Result.Loading)
    val article: StateFlow<Result<DiscoverContent.Article>> = _article.asStateFlow()

    private val _isBookmarked = MutableStateFlow(false)
    val isBookmarked: StateFlow<Boolean> = _isBookmarked.asStateFlow()

    private val _contentValidation = MutableStateFlow<Result<ContentValidationResult>?>(null)
    val contentValidation: StateFlow<Result<ContentValidationResult>?> = _contentValidation.asStateFlow()

    /**
     * Load article by ID
     */
    fun loadArticle(articleId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val result = discoverRepository.getArticleById(articleId)
                when (result) {
                    is Result.Success -> {
                        if (result.data != null) {
                            _article.value = Result.Success(result.data)
                            _isBookmarked.value = result.data.isBookmarked
                            _uiState.value = _uiState.value.copy(isLoading = false)
                        } else {
                            _article.value = Result.Error(Exception("Article not found"))
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = "Article not found"
                            )
                        }
                    }
                    is Result.Error -> {
                        _article.value = result
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = result.exception?.message ?: "Failed to load article"
                        )
                    }
                    is Result.Loading -> {
                        _article.value = result
                    }
                }
            } catch (e: Exception) {
                _article.value = Result.Error(e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load article"
                )
            }
        }
    }

    /**
     * Toggle bookmark status for the current article
     */
    fun toggleBookmark(articleId: String) {
        viewModelScope.launch {
            try {
                val result = discoverRepository.toggleBookmark(articleId, "article")
                when (result) {
                    is Result.Success -> {
                        _isBookmarked.value = result.data
                        val message = if (result.data) {
                            "Article bookmarked"
                        } else {
                            "Bookmark removed"
                        }
                        _uiState.value = _uiState.value.copy(message = message)
                    }
                    is Result.Error -> {
                        _uiState.value = _uiState.value.copy(
                            error = "Failed to update bookmark: ${result.exception?.message}"
                        )
                    }
                    is Result.Loading -> {
                        // Handle loading state if needed
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to update bookmark: ${e.message}"
                )
            }
        }
    }

    /**
     * Update reading progress for the article
     */
    fun updateReadingProgress(articleId: String, progress: Float) {
        viewModelScope.launch {
            try {
                // Update progress in background without showing loading states
                discoverRepository.updateReadingProgress(articleId, progress)
            } catch (e: Exception) {
                // Handle progress update errors silently
                // We don't want to interrupt the reading experience
            }
        }
    }

    /**
     * Validate content credibility and generate warnings
     */
    fun validateContent(articleId: String) {
        viewModelScope.launch {
            try {
                _contentValidation.value = Result.Loading
                val result = contentValidationUseCase.validateContentCredibility(articleId, "article")
                _contentValidation.value = result
            } catch (e: Exception) {
                _contentValidation.value = Result.Error(e)
            }
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Clear message
     */
    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    /**
     * Report content issue
     */
    fun reportContentIssue(articleId: String, issueType: String, description: String) {
        viewModelScope.launch {
            try {
                val result = discoverRepository.reportContentIssue(
                    contentId = articleId,
                    contentType = "article",
                    issueType = issueType,
                    description = description
                )
                
                when (result) {
                    is Result.Success -> {
                        _uiState.value = _uiState.value.copy(
                            message = "Thank you for your feedback. We'll review this content."
                        )
                    }
                    is Result.Error -> {
                        _uiState.value = _uiState.value.copy(
                            error = "Failed to submit report: ${result.exception?.message}"
                        )
                    }
                    is Result.Loading -> {
                        // Handle loading state if needed
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to submit report: ${e.message}"
                )
            }
        }
    }

    /**
     * Refresh article content
     */
    fun refreshArticle(articleId: String) {
        loadArticle(articleId)
    }
}