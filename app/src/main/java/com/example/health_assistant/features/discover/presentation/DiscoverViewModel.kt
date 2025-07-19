package com.example.health_assistant.features.discover.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.health_assistant.core.util.Result
import com.example.health_assistant.features.discover.domain.DiscoverManager
import com.example.health_assistant.features.discover.domain.DiscoverFeedData
import com.example.health_assistant.features.discover.domain.SearchResults
import com.example.health_assistant.features.discover.domain.SearchFilters
import com.example.health_assistant.features.discover.domain.model.DiscoverContent
import com.example.health_assistant.features.discover.domain.model.HealthContentCategory
import com.example.health_assistant.features.discover.domain.model.ContentValidationResult
import com.example.health_assistant.features.discover.domain.model.ContentReportType
import com.example.health_assistant.features.discover.domain.error.DiscoverError
import com.example.health_assistant.features.discover.domain.error.ErrorMapper
import com.example.health_assistant.features.discover.domain.error.RetryManager
import com.example.health_assistant.features.discover.domain.usecase.ReportContentUseCase
import com.example.health_assistant.features.discover.domain.usecase.AnalyticsTrackingUseCase
import com.example.health_assistant.features.discover.data.entity.ContentRecommendationEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import android.util.Log
import javax.inject.Inject

/**
 * ViewModel for Discover section with reactive state management
 * Manages UI state, content loading, search, and user interactions
 */
@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val discoverManager: DiscoverManager,
    private val reportContentUseCase: ReportContentUseCase,
    private val analyticsTrackingUseCase: AnalyticsTrackingUseCase,
    private val errorMapper: ErrorMapper,
    private val retryManager: RetryManager
) : ViewModel() {

    // ==================== UI STATE ====================

    private val _uiState = MutableStateFlow(DiscoverUiState())
    val uiState: StateFlow<DiscoverUiState> = _uiState.asStateFlow()

    private val _contentFeed = MutableStateFlow<Result<DiscoverFeedData>>(Result.Loading)
    val contentFeed: StateFlow<Result<DiscoverFeedData>> = _contentFeed.asStateFlow()

    private val _searchResults = MutableStateFlow<Result<SearchResults>?>(null)
    val searchResults: StateFlow<Result<SearchResults>?> = _searchResults.asStateFlow()

    private val _bookmarkedContent = MutableStateFlow<Result<List<DiscoverContent>>>(Result.Loading)
    val bookmarkedContent: StateFlow<Result<List<DiscoverContent>>> = _bookmarkedContent.asStateFlow()

    private val _trendingContent = MutableStateFlow<Result<List<DiscoverContent>>>(Result.Loading)
    val trendingContent: StateFlow<Result<List<DiscoverContent>>> = _trendingContent.asStateFlow()

    // ==================== SEARCH STATE ====================

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive: StateFlow<Boolean> = _isSearchActive.asStateFlow()

    private val _searchFilters = MutableStateFlow(SearchFilters())
    val searchFilters: StateFlow<SearchFilters> = _searchFilters.asStateFlow()

    private val _searchSuggestions = MutableStateFlow<List<String>>(emptyList())
    val searchSuggestions: StateFlow<List<String>> = _searchSuggestions.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    // ==================== ERROR HANDLING STATE ====================

    private val _errorState = MutableStateFlow<ErrorState>(ErrorState.None)
    val errorState: StateFlow<ErrorState> = _errorState.asStateFlow()

    private val _retryState = MutableStateFlow(RetryState())
    val retryState: StateFlow<RetryState> = _retryState.asStateFlow()

    // ==================== CONTENT REPORTING STATE ====================

    private val _reportState = MutableStateFlow<ReportState>(ReportState.None)
    val reportState: StateFlow<ReportState> = _reportState.asStateFlow()

    // ==================== ANALYTICS STATE ====================

    private val _recommendations = MutableStateFlow<List<ContentRecommendationEntity>>(emptyList())
    val recommendations: StateFlow<List<ContentRecommendationEntity>> = _recommendations.asStateFlow()

    private val _contentLayoutVariant = MutableStateFlow("card_layout")
    val contentLayoutVariant: StateFlow<String> = _contentLayoutVariant.asStateFlow()

    private val _readingProgressVariant = MutableStateFlow("control")
    val readingProgressVariant: StateFlow<String> = _readingProgressVariant.asStateFlow()

    // Debouncing for search
    private var searchJob: Job? = null
    private var suggestionJob: Job? = null

    // Reading session tracking
    private var currentReadingSession: ReadingSession? = null

    // ==================== FILTER STATE ====================

    private val _selectedCategory = MutableStateFlow<HealthContentCategory?>(null)
    val selectedCategory: StateFlow<HealthContentCategory?> = _selectedCategory.asStateFlow()

    private val _availableCategories = MutableStateFlow(HealthContentCategory.values().toList())
    val availableCategories: StateFlow<List<HealthContentCategory>> = _availableCategories.asStateFlow()

    init {
        loadInitialContent()
        observeContentChanges()
        initializeAnalytics()
    }

    // ==================== CONTENT LOADING ====================

    /**
     * Load initial content feed
     */
    private fun loadInitialContent() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // Load main content feed
                discoverManager.getContentFeed().collect { result ->
                    _contentFeed.value = result
                    updateUiStateFromResult(result)
                }
            } catch (e: Exception) {
                handleError("Failed to load content", e)
            }
        }
        
        // Load trending content separately
        loadTrendingContent()
        
        // Load bookmarked content
        loadBookmarkedContent()
    }

    /**
     * Refresh all content
     */
    fun refreshContent() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)
            
            try {
                // Sync content from remote
                discoverManager.syncContent()
                
                // Reload content feed
                discoverManager.getContentFeed(_selectedCategory.value).collect { result ->
                    _contentFeed.value = result
                    updateUiStateFromResult(result)
                    _uiState.value = _uiState.value.copy(isRefreshing = false)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isRefreshing = false)
                handleError("Failed to refresh content", e)
            }
        }
    }

    /**
     * Load content for specific category
     */
    fun loadContentByCategory(category: HealthContentCategory?) {
        viewModelScope.launch {
            _selectedCategory.value = category
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                discoverManager.getContentFeed(category).collect { result ->
                    _contentFeed.value = result
                    updateUiStateFromResult(result)
                }
            } catch (e: Exception) {
                handleError("Failed to load category content", e)
            }
        }
    }

    /**
     * Load trending content
     */
    private fun loadTrendingContent() {
        viewModelScope.launch {
            try {
                discoverManager.getTrendingContent(15).collect { result ->
                    _trendingContent.value = result
                }
            } catch (e: Exception) {
                _trendingContent.value = Result.Error(Exception("Failed to load trending content"))
            }
        }
    }

    /**
     * Load bookmarked content
     */
    private fun loadBookmarkedContent() {
        viewModelScope.launch {
            try {
                discoverManager.getBookmarkedContent().collect { result ->
                    _bookmarkedContent.value = result
                }
            } catch (e: Exception) {
                _bookmarkedContent.value = Result.Error(Exception("Failed to load bookmarks"))
            }
        }
    }

    // ==================== SEARCH FUNCTIONALITY ====================

    /**
     * Perform content search with debouncing
     */
    fun searchContent(query: String) {
        if (query.isBlank()) {
            clearSearch()
            return
        }
        
        _searchQuery.value = query
        _isSearchActive.value = true
        
        // Cancel previous search job
        searchJob?.cancel()
        
        // Start new debounced search
        searchJob = viewModelScope.launch {
            _isSearching.value = true
            delay(300) // 300ms debounce delay
            
            try {
                _searchResults.value = Result.Loading
                val result = discoverManager.searchContent(query.trim(), _searchFilters.value)
                _searchResults.value = result
            } catch (e: Exception) {
                _searchResults.value = Result.Error(Exception("Search failed: ${e.message}"))
            } finally {
                _isSearching.value = false
            }
        }
    }

    /**
     * Update search query and trigger debounced search
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        
        if (query.isBlank()) {
            clearSearch()
            return
        }
        
        // Update suggestions with debouncing
        suggestionJob?.cancel()
        suggestionJob = viewModelScope.launch {
            delay(150) // Shorter delay for suggestions
            loadSearchSuggestions(query)
        }
        
        // Trigger search if query is long enough
        if (query.length >= 2) {
            searchContent(query)
        }
    }

    /**
     * Load search suggestions
     */
    private suspend fun loadSearchSuggestions(query: String) {
        try {
            // Get suggestions from popular terms and categories
            val suggestions = mutableListOf<String>()
            
            // Add matching categories
            HealthContentCategory.values().forEach { category ->
                if (category.displayName.contains(query, ignoreCase = true)) {
                    suggestions.add(category.displayName)
                }
            }
            
            // Add popular health terms
            val popularTerms = listOf(
                "nutrition", "exercise", "mental health", "diabetes", "heart health",
                "weight loss", "sleep", "stress", "vitamins", "meditation",
                "fitness", "diet", "wellness", "prevention", "symptoms",
                "blood pressure", "cholesterol", "immune system", "metabolism"
            )
            
            popularTerms.filter { it.contains(query, ignoreCase = true) }
                .take(10 - suggestions.size)
                .forEach { suggestions.add(it) }
            
            _searchSuggestions.value = suggestions.take(8)
        } catch (e: Exception) {
            _searchSuggestions.value = emptyList()
        }
    }

    /**
     * Select a search suggestion
     */
    fun selectSearchSuggestion(suggestion: String) {
        searchContent(suggestion)
        _searchSuggestions.value = emptyList()
    }

    /**
     * Update search filters
     */
    fun updateSearchFilters(filters: SearchFilters) {
        _searchFilters.value = filters
        
        // Re-run search if active
        if (_isSearchActive.value && _searchQuery.value.isNotBlank()) {
            searchContent(_searchQuery.value)
        }
    }

    /**
     * Clear search and return to main feed
     */
    fun clearSearch() {
        _searchQuery.value = ""
        _isSearchActive.value = false
        _searchResults.value = null
    }

    // ==================== CONTENT INTERACTIONS ====================

    /**
     * Toggle bookmark for content
     */
    fun toggleBookmark(content: DiscoverContent) {
        viewModelScope.launch {
            try {
                val result = discoverManager.toggleBookmark(content)
                when (result) {
                    is Result.Success -> {
                        // Refresh bookmarked content
                        loadBookmarkedContent()
                        
                        // Update UI state
                        _uiState.value = _uiState.value.copy(
                            lastBookmarkAction = BookmarkAction(
                                contentId = content.id,
                                isBookmarked = result.data,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    }
                    is Result.Error -> {
                        handleError("Failed to update bookmark", result.exception ?: Exception("Unknown error"))
                    }
                    is Result.Loading -> { /* Handle loading if needed */ }
                }
            } catch (e: Exception) {
                handleError("Failed to toggle bookmark", e)
            }
        }
    }

    /**
     * Update reading/watching progress
     */
    fun updateProgress(content: DiscoverContent, progress: Float) {
        viewModelScope.launch {
            try {
                discoverManager.updateProgress(content.id, content.getContentType(), progress)
            } catch (e: Exception) {
                // Silent failure for progress updates
            }
        }
    }

    /**
     * Validate content credibility
     */
    fun validateContent(content: DiscoverContent) {
        viewModelScope.launch {
            try {
                val result = discoverManager.validateContent(content)
                when (result) {
                    is Result.Success -> {
                        _uiState.value = _uiState.value.copy(
                            contentValidation = mapOf(content.id to result.data)
                        )
                    }
                    is Result.Error -> {
                        handleError("Content validation failed", result.exception ?: Exception("Unknown error"))
                    }
                    is Result.Loading -> { /* Handle loading if needed */ }
                }
            } catch (e: Exception) {
                handleError("Failed to validate content", e)
            }
        }
    }

    /**
     * Report content issue
     */
    fun reportContentIssue(content: DiscoverContent, issueType: String, description: String) {
        viewModelScope.launch {
            try {
                val result = discoverManager.reportContentIssue(content, issueType, description)
                when (result) {
                    is Result.Success -> {
                        _uiState.value = _uiState.value.copy(
                            message = "Content reported successfully"
                        )
                    }
                    is Result.Error -> {
                        handleError("Failed to report content", result.exception ?: Exception("Unknown error"))
                    }
                    is Result.Loading -> { /* Handle loading if needed */ }
                }
            } catch (e: Exception) {
                handleError("Failed to report content", e)
            }
        }
    }

    // ==================== ERROR HANDLING ====================

    /**
     * Handle errors with comprehensive error mapping and retry logic
     */
    private fun handleErrorWithRetry(operation: String, throwable: Throwable) {
        val discoverError = errorMapper.mapThrowableToDiscoverError(throwable)
        
        Log.e("DiscoverViewModel", "$operation failed", throwable)
        
        _errorState.value = ErrorState.Error(
            discoverError = discoverError,
            isRetrying = false,
            retryAttempt = 0
        )
        
        // Update UI state with user-friendly error
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            isRefreshing = false,
            error = discoverError.userMessage
        )
    }

    /**
     * Retry failed operation with exponential backoff
     */
    fun retryOperation() {
        val currentErrorState = _errorState.value
        if (currentErrorState !is ErrorState.Error || !currentErrorState.discoverError.isRetryable) {
            return
        }

        val currentRetryState = _retryState.value
        if (!currentRetryState.canRetry) {
            Log.w("DiscoverViewModel", "Max retry attempts reached")
            return
        }

        viewModelScope.launch {
            val newRetryAttempt = currentRetryState.retryAttempt + 1
            val retryDelay = retryManager.getRetryDelay(currentErrorState.discoverError, newRetryAttempt - 1)
            
            // Update retry state
            _retryState.value = currentRetryState.copy(
                isRetrying = true,
                retryAttempt = newRetryAttempt,
                nextRetryDelayMs = retryDelay
            )
            
            // Update error state to show retrying
            _errorState.value = ErrorState.Error(
                discoverError = currentErrorState.discoverError,
                isRetrying = true,
                retryAttempt = newRetryAttempt
            )
            
            try {
                // Wait for retry delay
                delay(retryDelay)
                
                // Retry the operation based on current state
                when {
                    _isSearchActive.value -> {
                        searchContent(_searchQuery.value)
                    }
                    _selectedCategory.value != null -> {
                        loadContentByCategory(_selectedCategory.value)
                    }
                    else -> {
                        refreshContent()
                    }
                }
                
                // Clear error state on successful retry
                _errorState.value = ErrorState.None
                _retryState.value = RetryState()
                
            } catch (e: Exception) {
                // Handle retry failure
                val retryError = errorMapper.mapThrowableToDiscoverError(e)
                
                _errorState.value = ErrorState.Error(
                    discoverError = retryError,
                    isRetrying = false,
                    retryAttempt = newRetryAttempt
                )
                
                _retryState.value = currentRetryState.copy(
                    isRetrying = false,
                    retryAttempt = newRetryAttempt
                )
                
                Log.e("DiscoverViewModel", "Retry attempt $newRetryAttempt failed", e)
            }
        }
    }

    /**
     * Handle partial content loading failures
     */
    private fun handlePartialContentError(
        loadedCount: Int,
        totalCount: Int,
        failedOperations: Map<String, Throwable>
    ) {
        val partialError = errorMapper.mapPartialLoadFailure(loadedCount, totalCount, failedOperations)
        
        _errorState.value = ErrorState.PartialError(
            discoverError = partialError,
            isRetrying = false
        )
        
        Log.w("DiscoverViewModel", "Partial content load: $loadedCount/$totalCount succeeded")
    }

    /**
     * Clear error state
     */
    fun clearErrorState() {
        _errorState.value = ErrorState.None
        _retryState.value = RetryState()
        clearError()
    }

    /**
     * Handle network offline state
     */
    fun handleOfflineState(hasCache: Boolean) {
        _errorState.value = ErrorState.NetworkOffline(hasCache = hasCache)
        
        if (hasCache) {
            // Load cached content
            loadInitialContent()
        }
    }

    // ==================== CONTENT REPORTING ====================

    /**
     * Report content using ContentReport model
     */
    fun reportContent(
        contentId: String,
        contentType: String,
        contentTitle: String,
        reportType: ContentReportType,
        description: String
    ) {
        viewModelScope.launch {
            _reportState.value = ReportState.Loading
            
            try {
                val result = reportContentUseCase(
                    contentId = contentId,
                    contentType = contentType,
                    reportType = reportType,
                    description = description,
                    userId = getCurrentUserId() // You'll need to implement this
                )
                
                when (result) {
                    is Result.Success -> {
                        _reportState.value = ReportState.Success
                        _uiState.value = _uiState.value.copy(
                            message = "Thank you for your report. We'll review this content."
                        )
                        
                        Log.d("DiscoverViewModel", "Content report submitted successfully: $contentId")
                    }
                    is Result.Error -> {
                        _reportState.value = ReportState.Error(result.message)
                        Log.e("DiscoverViewModel", "Failed to submit content report: ${result.message}")
                    }
                    is Result.Loading -> {
                        // Loading state is already handled above
                    }
                }
            } catch (e: Exception) {
                val error = errorMapper.mapThrowableToDiscoverError(e)
                _reportState.value = ReportState.Error(error.userMessage)
                Log.e("DiscoverViewModel", "Exception submitting content report", e)
            }
        }
    }

    /**
     * Clear report state
     */
    fun clearReportState() {
        _reportState.value = ReportState.None
    }

    /**
     * Get user's submitted reports
     */
    fun loadUserReports() {
        viewModelScope.launch {
            try {
                val result = reportContentUseCase.getUserReports(getCurrentUserId())
                when (result) {
                    is Result.Success -> {
                        // Handle user reports if needed for UI
                        Log.d("DiscoverViewModel", "Loaded ${result.data.size} user reports")
                    }
                    is Result.Error -> {
                        Log.e("DiscoverViewModel", "Failed to load user reports: ${result.message}")
                    }
                    is Result.Loading -> {
                        // Loading state not expected for this operation
                    }
                }
            } catch (e: Exception) {
                Log.e("DiscoverViewModel", "Exception loading user reports", e)
            }
        }
    }

    // ==================== HELPER METHODS ====================

    /**
     * Get current user ID - implement based on your authentication system
     */
    private fun getCurrentUserId(): String {
        // TODO: Implement based on your authentication system
        return "current_user_id" // Placeholder
    }

    // ==================== UI STATE MANAGEMENT ====================

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
     * Set view mode (list/grid)
     */
    fun setViewMode(mode: ViewMode) {
        _uiState.value = _uiState.value.copy(viewMode = mode)
    }

    // ==================== PRIVATE HELPERS ====================

    private fun observeContentChanges() {
        // Observe category changes and reload content
        viewModelScope.launch {
            _selectedCategory.collect { category ->
                if (category != null) {
                    loadContentByCategory(category)
                }
            }
        }
    }

    private fun updateUiStateFromResult(result: Result<DiscoverFeedData>) {
        when (result) {
            is Result.Success -> {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = null,
                    isEmpty = result.data.isEmpty,
                    lastUpdated = result.data.lastUpdated
                )
            }
            is Result.Error -> {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = result.exception?.message ?: "Unknown error"
                )
            }
            is Result.Loading -> {
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    error = null
                )
            }
        }
    }

    private fun handleError(message: String, exception: Throwable) {
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            isRefreshing = false,
            error = "$message: ${exception.message}"
        )
    }

    // ==================== ANALYTICS FUNCTIONALITY ====================

    /**
     * Initialize analytics and A/B testing
     */
    private fun initializeAnalytics() {
        viewModelScope.launch {
            try {
                // Start new analytics session
                analyticsTrackingUseCase.startNewAnalyticsSession()
                
                // Load A/B test variants
                val userId = getCurrentUserId()
                _contentLayoutVariant.value = analyticsTrackingUseCase.getContentLayoutVariant(userId)
                _readingProgressVariant.value = analyticsTrackingUseCase.getReadingProgressVariant(userId)
                
                // Load personalized recommendations
                loadRecommendations()
                
                // Record A/B test impressions
                analyticsTrackingUseCase.recordABTestImpression(userId, "content_layout_v1")
                analyticsTrackingUseCase.recordABTestImpression(userId, "reading_progress_indicator")
                
            } catch (e: Exception) {
                Log.e("DiscoverViewModel", "Failed to initialize analytics", e)
            }
        }
    }

    /**
     * Track content view
     */
    fun trackContentView(content: DiscoverContent, source: String = "feed") {
        viewModelScope.launch {
            try {
                analyticsTrackingUseCase.trackContentView(content, source)
                
                // Record A/B test click if from recommendation
                if (source == "recommendation") {
                    val userId = getCurrentUserId()
                    analyticsTrackingUseCase.recordABTestClick(userId, "recommendation_algorithm_v2")
                }
            } catch (e: Exception) {
                Log.e("DiscoverViewModel", "Failed to track content view", e)
            }
        }
    }

    /**
     * Start reading session tracking
     */
    fun startReadingSession(content: DiscoverContent) {
        currentReadingSession = ReadingSession(
            contentId = content.id,
            contentType = content.getContentType(),
            startTime = System.currentTimeMillis()
        )
        
        viewModelScope.launch {
            try {
                analyticsTrackingUseCase.trackReadingStart(content)
            } catch (e: Exception) {
                Log.e("DiscoverViewModel", "Failed to track reading start", e)
            }
        }
    }

    /**
     * Update reading progress
     */
    fun trackReadingProgress(content: DiscoverContent, progress: Float) {
        val session = currentReadingSession
        if (session != null && session.contentId == content.id) {
            val duration = System.currentTimeMillis() - session.startTime
            
            viewModelScope.launch {
                try {
                    analyticsTrackingUseCase.trackReadingProgress(content, progress, duration)
                    
                    // Update content progress in repository
                    updateProgress(content, progress)
                    
                    // Record A/B test engagement time
                    val userId = getCurrentUserId()
                    analyticsTrackingUseCase.recordABTestEngagementTime(userId, "reading_progress_indicator", duration)
                    
                } catch (e: Exception) {
                    Log.e("DiscoverViewModel", "Failed to track reading progress", e)
                }
            }
        }
    }

    /**
     * Complete reading session
     */
    fun completeReadingSession(content: DiscoverContent) {
        val session = currentReadingSession
        if (session != null && session.contentId == content.id) {
            val totalDuration = System.currentTimeMillis() - session.startTime
            
            viewModelScope.launch {
                try {
                    analyticsTrackingUseCase.trackReadingComplete(content, totalDuration)
                    
                    // Record A/B test conversion
                    val userId = getCurrentUserId()
                    analyticsTrackingUseCase.recordABTestConversion(userId, "reading_progress_indicator", "reading_complete")
                    
                } catch (e: Exception) {
                    Log.e("DiscoverViewModel", "Failed to track reading completion", e)
                }
            }
            
            currentReadingSession = null
        }
    }

    /**
     * Track bookmark action with analytics
     */
    fun toggleBookmarkWithAnalytics(content: DiscoverContent) {
        viewModelScope.launch {
            try {
                val result = discoverManager.toggleBookmark(content)
                when (result) {
                    is Result.Success -> {
                        // Track bookmark analytics
                        analyticsTrackingUseCase.trackBookmark(content, result.data)
                        
                        // Record A/B test conversion if bookmarked
                        if (result.data) {
                            val userId = getCurrentUserId()
                            analyticsTrackingUseCase.recordABTestConversion(userId, "content_layout_v1", "bookmark")
                        }
                        
                        // Refresh bookmarked content
                        loadBookmarkedContent()
                        
                        // Update UI state
                        _uiState.value = _uiState.value.copy(
                            lastBookmarkAction = BookmarkAction(
                                contentId = content.id,
                                isBookmarked = result.data,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    }
                    is Result.Error -> {
                        handleError("Failed to update bookmark", result.exception ?: Exception("Unknown error"))
                    }
                    is Result.Loading -> { /* Handle loading if needed */ }
                }
            } catch (e: Exception) {
                handleError("Failed to toggle bookmark", e)
            }
        }
    }

    /**
     * Track content sharing
     */
    fun trackContentShare(content: DiscoverContent, shareMethod: String) {
        viewModelScope.launch {
            try {
                analyticsTrackingUseCase.trackShare(content, shareMethod)
                
                // Record A/B test conversion
                val userId = getCurrentUserId()
                analyticsTrackingUseCase.recordABTestConversion(userId, "content_layout_v1", "share")
                
            } catch (e: Exception) {
                Log.e("DiscoverViewModel", "Failed to track content share", e)
            }
        }
    }

    /**
     * Track search with analytics
     */
    fun searchContentWithAnalytics(query: String) {
        if (query.isBlank()) {
            clearSearch()
            return
        }
        
        _searchQuery.value = query
        _isSearchActive.value = true
        
        // Cancel previous search job
        searchJob?.cancel()
        
        // Start new debounced search
        searchJob = viewModelScope.launch {
            _isSearching.value = true
            delay(300) // 300ms debounce delay
            
            try {
                _searchResults.value = Result.Loading
                val result = discoverManager.searchContent(query.trim(), _searchFilters.value)
                _searchResults.value = result
                
                // Track search analytics
                val resultsCount = when (result) {
                    is Result.Success -> result.data.results.size
                    else -> 0
                }
                analyticsTrackingUseCase.trackSearch(getCurrentUserId(), query, resultsCount)
                
            } catch (e: Exception) {
                _searchResults.value = Result.Error(Exception("Search failed: ${e.message}"))
            } finally {
                _isSearching.value = false
            }
        }
    }

    /**
     * Load personalized recommendations
     */
    private fun loadRecommendations() {
        viewModelScope.launch {
            try {
                val userId = getCurrentUserId()
                val recommendations = analyticsTrackingUseCase.generateRecommendations(userId, 10)
                _recommendations.value = recommendations
                
                // Track recommendation impressions
                recommendations.forEach { recommendation ->
                    analyticsTrackingUseCase.trackRecommendationPerformance(recommendation.id, "shown")
                }
                
            } catch (e: Exception) {
                Log.e("DiscoverViewModel", "Failed to load recommendations", e)
                _recommendations.value = emptyList()
            }
        }
    }

    /**
     * Track recommendation click
     */
    fun trackRecommendationClick(recommendation: ContentRecommendationEntity) {
        viewModelScope.launch {
            try {
                analyticsTrackingUseCase.trackRecommendationPerformance(recommendation.id, "clicked")
                
                // Record A/B test click
                val userId = getCurrentUserId()
                analyticsTrackingUseCase.recordABTestClick(userId, "recommendation_algorithm_v2")
                
            } catch (e: Exception) {
                Log.e("DiscoverViewModel", "Failed to track recommendation click", e)
            }
        }
    }

    /**
     * Get user engagement statistics
     */
    fun getUserEngagementStats() {
        viewModelScope.launch {
            try {
                val userId = getCurrentUserId()
                val stats = analyticsTrackingUseCase.getUserEngagementStats(userId)
                
                // Update UI state with engagement stats if needed
                Log.d("DiscoverViewModel", "User engagement stats: ${stats.size} categories")
                
            } catch (e: Exception) {
                Log.e("DiscoverViewModel", "Failed to load engagement stats", e)
            }
        }
    }

    /**
     * Perform analytics cleanup
     */
    fun performAnalyticsCleanup() {
        viewModelScope.launch {
            try {
                analyticsTrackingUseCase.performAnalyticsCleanup()
                Log.d("DiscoverViewModel", "Analytics cleanup completed")
            } catch (e: Exception) {
                Log.e("DiscoverViewModel", "Failed to perform analytics cleanup", e)
            }
        }
    }
}

/**
 * UI State data class for Discover section
 */
data class DiscoverUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val isEmpty: Boolean = false,
    val lastUpdated: Long = 0L,
    val viewMode: ViewMode = ViewMode.LIST,
    val lastBookmarkAction: BookmarkAction? = null,
    val contentValidation: Map<String, ContentValidationResult> = emptyMap()
)

/**
 * Bookmark action data class
 */
data class BookmarkAction(
    val contentId: String,
    val isBookmarked: Boolean,
    val timestamp: Long
)

/**
 * View mode enum
 */
enum class ViewMode {
    LIST, GRID
}

/**
 * Reading session data class for tracking user engagement
 */
data class ReadingSession(
    val contentId: String,
    val contentType: String,
    val startTime: Long
)