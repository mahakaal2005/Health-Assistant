package com.example.health_assistant.features.discover.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.health_assistant.core.util.Result
import com.example.health_assistant.features.discover.domain.model.DiscoverContent
import com.example.health_assistant.features.discover.domain.repository.DiscoverRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing bookmarks and reading history
 * Handles content filtering, sorting, and synchronization
 */
@HiltViewModel
class BookmarksViewModel @Inject constructor(
    private val discoverRepository: DiscoverRepository
) : ViewModel() {

    private val _bookmarksState = MutableStateFlow<BookmarksState>(BookmarksState.Loading)
    val bookmarksState: StateFlow<BookmarksState> = _bookmarksState.asStateFlow()

    private val _readingHistoryState = MutableStateFlow<ReadingHistoryState>(ReadingHistoryState.Loading)
    val readingHistoryState: StateFlow<ReadingHistoryState> = _readingHistoryState.asStateFlow()

    private val _currentFilter = MutableStateFlow<String?>(null)
    private val _currentSort = MutableStateFlow(SortOption.DATE_ADDED)
    private val _showReadingHistory = MutableStateFlow(false)

    private var allBookmarks: List<DiscoverContent> = emptyList()
    private var readingHistory: Map<String, ReadingHistoryItem> = emptyMap()

    init {
        observeBookmarks()
        observeReadingHistory()
    }

    private fun observeBookmarks() {
        viewModelScope.launch {
            combine(
                discoverRepository.getBookmarkedContent(),
                _currentFilter,
                _currentSort
            ) { bookmarksResult, filter, sort ->
                Triple(bookmarksResult, filter, sort)
            }.collect { (bookmarksResult, filter, sort) ->
                when (bookmarksResult) {
                    is Result.Success -> {
                        allBookmarks = bookmarksResult.data
                        val filteredAndSorted = filterAndSortBookmarks(allBookmarks, filter, sort)
                        
                        _bookmarksState.value = if (filteredAndSorted.isEmpty()) {
                            BookmarksState.Empty
                        } else {
                            BookmarksState.Success(filteredAndSorted)
                        }
                    }
                    is Result.Error -> {
                        _bookmarksState.value = BookmarksState.Error(
                            bookmarksResult.message ?: "Failed to load bookmarks"
                        )
                    }
                    is Result.Loading -> {
                        _bookmarksState.value = BookmarksState.Loading
                    }
                }
            }
        }
    }

    private fun observeReadingHistory() {
        viewModelScope.launch {
            // Collect reading progress for articles and videos
            val articlesFlow = discoverRepository.getHealthArticles()
            val videosFlow = discoverRepository.getEducationalVideos()

            combine(articlesFlow, videosFlow) { articlesResult, videosResult ->
                val history = mutableMapOf<String, ReadingHistoryItem>()
                
                if (articlesResult is Result.Success) {
                    articlesResult.data.forEach { article ->
                        if (article.readProgress > 0f) {
                            history[article.id] = ReadingHistoryItem(
                                contentId = article.id,
                                contentType = "article",
                                progress = article.readProgress,
                                lastAccessTime = System.currentTimeMillis(), // This should come from actual tracking
                                isCompleted = article.readProgress >= 0.95f
                            )
                        }
                    }
                }
                
                if (videosResult is Result.Success) {
                    videosResult.data.forEach { video ->
                        if (video.watchProgress > 0f) {
                            history[video.id] = ReadingHistoryItem(
                                contentId = video.id,
                                contentType = "video",
                                progress = video.watchProgress,
                                lastAccessTime = System.currentTimeMillis(), // This should come from actual tracking
                                isCompleted = video.watchProgress >= 0.95f
                            )
                        }
                    }
                }
                
                history
            }.collect { history ->
                readingHistory = history
                _readingHistoryState.value = ReadingHistoryState.Success(history)
            }
        }
    }

    fun loadBookmarks() {
        _bookmarksState.value = BookmarksState.Loading
        // The observeBookmarks() flow will handle the actual loading
    }

    fun refreshBookmarks() {
        viewModelScope.launch {
            discoverRepository.syncContentFromRemote()
        }
    }

    fun filterByContentType(contentType: String?) {
        _currentFilter.value = contentType
    }

    fun sortBookmarks(sortOption: SortOption) {
        _currentSort.value = sortOption
    }

    fun removeBookmark(content: DiscoverContent) {
        viewModelScope.launch {
            val result = discoverRepository.removeBookmark(content.id)
            if (result is Result.Error) {
                // Handle error - could show a snackbar or toast
                _bookmarksState.value = BookmarksState.Error(
                    result.message ?: "Failed to remove bookmark"
                )
            }
        }
    }

    fun clearAllBookmarks() {
        viewModelScope.launch {
            allBookmarks.forEach { content ->
                discoverRepository.removeBookmark(content.id)
            }
        }
    }

    fun toggleReadingHistoryView() {
        _showReadingHistory.value = !_showReadingHistory.value
    }

    private fun filterAndSortBookmarks(
        bookmarks: List<DiscoverContent>,
        filter: String?,
        sort: SortOption
    ): List<DiscoverContent> {
        var filtered = bookmarks
        
        // Apply content type filter
        if (filter != null) {
            filtered = bookmarks.filter { content ->
                when (filter) {
                    "article" -> content is DiscoverContent.Article
                    "news" -> content is DiscoverContent.News
                    "video" -> content is DiscoverContent.Video
                    else -> true
                }
            }
        }
        
        // Apply sorting
        return when (sort) {
            SortOption.DATE_ADDED -> {
                // Sort by bookmark date (most recent first)
                // Note: This would require storing bookmark timestamp in the entity
                filtered.sortedByDescending { it.publishedDate }
            }
            SortOption.DATE_PUBLISHED -> {
                filtered.sortedByDescending { it.publishedDate }
            }
            SortOption.CONTENT_TYPE -> {
                filtered.sortedWith(compareBy<DiscoverContent> { 
                    when (it) {
                        is DiscoverContent.Article -> 0
                        is DiscoverContent.News -> 1
                        is DiscoverContent.Video -> 2
                    }
                }.thenByDescending { it.publishedDate })
            }
            SortOption.CATEGORY -> {
                filtered.sortedWith(compareBy<DiscoverContent> { it.category }
                    .thenByDescending { it.publishedDate })
            }
        }
    }

    sealed class BookmarksState {
        object Loading : BookmarksState()
        data class Success(val bookmarks: List<DiscoverContent>) : BookmarksState()
        object Empty : BookmarksState()
        data class Error(val message: String) : BookmarksState()
    }

    sealed class ReadingHistoryState {
        object Loading : ReadingHistoryState()
        data class Success(val history: Map<String, ReadingHistoryItem>) : ReadingHistoryState()
        data class Error(val message: String) : ReadingHistoryState()
    }

    enum class SortOption {
        DATE_ADDED,
        DATE_PUBLISHED,
        CONTENT_TYPE,
        CATEGORY
    }

    data class ReadingHistoryItem(
        val contentId: String,
        val contentType: String,
        val progress: Float,
        val lastAccessTime: Long,
        val isCompleted: Boolean
    )
}