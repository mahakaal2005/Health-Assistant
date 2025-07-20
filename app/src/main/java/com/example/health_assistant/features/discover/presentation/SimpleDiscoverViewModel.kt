package com.example.health_assistant.features.discover.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.health_assistant.core.util.Result
import com.example.health_assistant.features.discover.domain.model.DiscoverSections
import com.example.health_assistant.features.discover.domain.model.HealthContent
import com.example.health_assistant.features.discover.domain.repository.DiscoverRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Simple ViewModel for discover screen with basic state management
 */
@HiltViewModel
class SimpleDiscoverViewModel @Inject constructor(
    private val repository: DiscoverRepository
) : ViewModel() {

    companion object {
        private const val TAG = "SimpleDiscoverViewModel"
    }

    private val _uiState = MutableStateFlow(DiscoverUiState())
    val uiState: StateFlow<DiscoverUiState> = _uiState.asStateFlow()

    init {
        loadContent()
    }

    /**
     * Load content from repository
     */
    fun loadContent() {
        Log.d(TAG, "Loading discover content")
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )

            when (val result = repository.getDiscoverContent()) {
                is Result.Success -> {
                    Log.d(TAG, "Content loaded successfully")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        sections = result.data,
                        error = null
                    )
                }
                is Result.Error -> {
                    Log.e(TAG, "Failed to load content", result.exception)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.exception?.message ?: "Failed to load content"
                    )
                }
                is Result.Loading -> {
                    // Keep loading state
                }
            }
        }
    }

    /**
     * Refresh content from APIs
     */
    fun refreshContent() {
        Log.d(TAG, "Refreshing discover content")
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isRefreshing = true,
                error = null
            )

            when (val result = repository.refreshContent()) {
                is Result.Success -> {
                    Log.d(TAG, "Content refreshed successfully")
                    _uiState.value = _uiState.value.copy(
                        isRefreshing = false,
                        sections = result.data,
                        error = null
                    )
                }
                is Result.Error -> {
                    Log.e(TAG, "Failed to refresh content", result.exception)
                    _uiState.value = _uiState.value.copy(
                        isRefreshing = false,
                        error = result.exception?.message ?: "Failed to refresh content"
                    )
                }
                is Result.Loading -> {
                    // Keep refreshing state
                }
            }
        }
    }

    /**
     * Handle content click - open in browser
     */
    fun onContentClick(content: HealthContent) {
        Log.d(TAG, "Content clicked: ${content.title}")
        // This will be handled by the Fragment to open browser
        // ViewModel just logs the interaction
    }

    /**
     * Clear error state
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Retry loading content after error
     */
    fun retry() {
        Log.d(TAG, "Retrying content load")
        loadContent()
    }
}

/**
 * UI state for discover screen
 */
data class DiscoverUiState(
    val isLoading: Boolean = false,
    val sections: DiscoverSections? = null,
    val error: String? = null,
    val isRefreshing: Boolean = false
) {
    val hasContent: Boolean
        get() = sections != null && (
            sections.articles.isNotEmpty() || 
            sections.news.isNotEmpty() || 
            sections.videos.isNotEmpty()
        )
    
    val isEmpty: Boolean
        get() = sections != null && 
            sections.articles.isEmpty() && 
            sections.news.isEmpty() && 
            sections.videos.isEmpty()
}