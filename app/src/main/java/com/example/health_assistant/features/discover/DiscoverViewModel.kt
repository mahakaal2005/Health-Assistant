package com.example.health_assistant.features.discover

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.health_assistant.features.discover.data.DiscoverRepository
import com.example.health_assistant.features.discover.model.HealthTopic
import com.example.health_assistant.features.discover.model.QuickAction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Discover screen that manages UI state and user interactions
 */
class DiscoverViewModel : ViewModel() {
    private val TAG = "DiscoverViewModel"

    // Manually instantiate the repository (no DI)
    private val repository = DiscoverRepository()

    // Search query entered by the user
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Search results based on the current query
    private val _searchResults = MutableStateFlow<List<HealthTopic>>(emptyList())
    val searchResults: StateFlow<List<HealthTopic>> = _searchResults.asStateFlow()

    // Featured topics to display on the Discover screen
    private val _featuredTopics = MutableStateFlow<List<HealthTopic>>(emptyList())
    val featuredTopics: StateFlow<List<HealthTopic>> = _featuredTopics.asStateFlow()

    // Quick action buttons shown on the Discover screen
    private val _quickActions = MutableStateFlow<List<QuickAction>>(emptyList())
    val quickActions: StateFlow<List<QuickAction>> = _quickActions.asStateFlow()

    // User's recent searches
    private val _recentSearches = MutableStateFlow<List<String>>(emptyList())
    val recentSearches: StateFlow<List<String>> = _recentSearches.asStateFlow()

    // Loading state to show/hide progress indicators
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Error state to display error messages to the user
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        Log.d(TAG, "Initializing DiscoverViewModel")
        loadInitialData()
    }

    /**
     * Load initial data for the Discover screen
     */
    private fun loadInitialData() {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                // Load featured topics
                val topics = repository.getFeaturedTopics()
                _featuredTopics.value = topics
                Log.d(TAG, "Loaded ${topics.size} featured topics")

                // Load quick actions
                val actions = repository.getQuickActions()
                _quickActions.value = actions
                Log.d(TAG, "Loaded ${actions.size} quick actions")

                // Load recent searches
                val searches = repository.getRecentSearches()
                _recentSearches.value = searches
                Log.d(TAG, "Loaded ${searches.size} recent searches")

            } catch (e: Exception) {
                Log.e(TAG, "Error loading initial data", e)
                _error.value = "Failed to load data: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Update the current search query and perform search
     */
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        performSearch(query)
    }

    /**
     * Perform search based on the current query
     */
    private fun performSearch(query: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                if (query.isBlank()) {
                    _searchResults.value = emptyList()
                    return@launch
                }

                val results = repository.searchTopics(query)
                _searchResults.value = results
                Log.d(TAG, "Search completed for '$query', found ${results.size} results")

                // Update recent searches from repository
                _recentSearches.value = repository.getRecentSearches()

            } catch (e: Exception) {
                Log.e(TAG, "Error performing search", e)
                _error.value = "Search failed: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Clear recent searches
     */
    fun clearRecentSearches() {
        viewModelScope.launch {
            repository.clearRecentSearches()
            _recentSearches.value = emptyList()
            Log.d(TAG, "Cleared recent searches")
        }
    }

    /**
     * Clear any error message
     */
    fun clearError() {
        _error.value = null
    }
}