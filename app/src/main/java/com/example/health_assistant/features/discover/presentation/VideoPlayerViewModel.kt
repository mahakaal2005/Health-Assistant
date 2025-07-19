package com.example.health_assistant.features.discover.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.health_assistant.core.util.Result
import com.example.health_assistant.features.discover.domain.model.DiscoverContent
import com.example.health_assistant.features.discover.domain.repository.DiscoverRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for video player functionality
 * Handles video loading, playback progress, bookmarks, and offline downloads
 */
@HiltViewModel
class VideoPlayerViewModel @Inject constructor(
    private val discoverRepository: DiscoverRepository
) : ViewModel() {

    private val _currentVideo = MutableLiveData<DiscoverContent.Video?>()
    val currentVideo: LiveData<DiscoverContent.Video?> = _currentVideo

    private val _playbackPosition = MutableLiveData<Long>()
    val playbackPosition: LiveData<Long> = _playbackPosition

    private val _isBookmarked = MutableLiveData<Boolean>()
    val isBookmarked: LiveData<Boolean> = _isBookmarked

    private val _downloadState = MutableLiveData<DownloadState>()
    val downloadState: LiveData<DownloadState> = _downloadState

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    enum class DownloadState {
        NOT_DOWNLOADED,
        DOWNLOADING,
        DOWNLOADED,
        FAILED
    }

    /**
     * Load video by ID
     */
    fun loadVideo(videoId: String) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null

            try {
                val result = discoverRepository.getVideoById(videoId)
                when (result) {
                    is Result.Success -> {
                        val video = result.data
                        if (video != null) {
                            _currentVideo.value = video
                            _isBookmarked.value = checkBookmarkStatus(videoId)
                            _downloadState.value = if (discoverRepository.isContentAvailableOffline(videoId, "video")) {
                                DownloadState.DOWNLOADED
                            } else {
                                DownloadState.NOT_DOWNLOADED
                            }
                            
                            // Set playback position from saved progress (placeholder implementation)
                            _playbackPosition.value = 0L
                        } else {
                            _error.value = "Video not found"
                        }
                    }
                    is Result.Error -> {
                        _error.value = "Failed to load video: ${result.message}"
                    }
                    is Result.Loading -> {
                        // Loading state is already set above
                    }
                }
            } catch (e: Exception) {
                _error.value = "Failed to load video: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    /**
     * Update watch progress for the current video
     */
    fun updateWatchProgress(progress: Float) {
        val video = _currentVideo.value ?: return
        
        viewModelScope.launch {
            try {
                discoverRepository.updateWatchProgress(video.id, progress)
                
                // Update local video object (placeholder - actual implementation would depend on video model)
                _currentVideo.value = video
            } catch (e: Exception) {
                _error.value = "Failed to save progress: ${e.message}"
            }
        }
    }

    /**
     * Toggle bookmark status for current video
     */
    fun toggleBookmark() {
        val video = _currentVideo.value ?: return
        
        viewModelScope.launch {
            try {
                val currentBookmarkStatus = _isBookmarked.value ?: false
                
                if (currentBookmarkStatus) {
                    discoverRepository.removeBookmark(video.id)
                    _isBookmarked.value = false
                } else {
                    discoverRepository.addBookmark(video.id, "video")
                    _isBookmarked.value = true
                }
            } catch (e: Exception) {
                _error.value = "Failed to update bookmark: ${e.message}"
            }
        }
    }

    /**
     * Toggle offline download for current video
     */
    fun toggleOfflineDownload() {
        val video = _currentVideo.value ?: return
        
        viewModelScope.launch {
            try {
                when (_downloadState.value) {
                    DownloadState.NOT_DOWNLOADED -> {
                        _downloadState.value = DownloadState.DOWNLOADING
                        
                        val result = discoverRepository.downloadVideoForOffline(video.id)
                        when (result) {
                            is Result.Success -> {
                                _downloadState.value = DownloadState.DOWNLOADED
                                
                                // Update video object (placeholder implementation)
                                _currentVideo.value = video
                            }
                            is Result.Error -> {
                                _downloadState.value = DownloadState.FAILED
                                _error.value = "Download failed: ${result.message}"
                            }
                            is Result.Loading -> {
                                // Keep downloading state
                            }
                        }
                    }
                    DownloadState.DOWNLOADED -> {
                        // Remove offline download
                        val result = discoverRepository.removeOfflineVideo(video.id)
                        when (result) {
                            is Result.Success -> {
                                _downloadState.value = DownloadState.NOT_DOWNLOADED
                                
                                // Update video object (placeholder implementation)
                                _currentVideo.value = video
                            }
                            is Result.Error -> {
                                _error.value = "Failed to remove download: ${result.message}"
                            }
                            is Result.Loading -> {
                                // Keep current state
                            }
                        }
                    }
                    DownloadState.FAILED -> {
                        // Retry download
                        toggleOfflineDownload()
                    }
                    DownloadState.DOWNLOADING -> {
                        // Already downloading, do nothing
                    }
                    null -> {
                        // Initial state, start download
                        toggleOfflineDownload()
                    }
                }
            } catch (e: Exception) {
                _downloadState.value = DownloadState.FAILED
                _error.value = "Download operation failed: ${e.message}"
            }
        }
    }

    /**
     * Set playback position for resume functionality
     */
    fun setPlaybackPosition(position: Long) {
        _playbackPosition.value = position
    }

    /**
     * Check if video is bookmarked
     */
    private suspend fun checkBookmarkStatus(videoId: String): Boolean {
        return try {
            val result = discoverRepository.isContentBookmarked(videoId)
            when (result) {
                is Result.Success -> result.data
                is Result.Error -> false
                is Result.Loading -> false
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * Get video quality options based on network conditions
     */
    fun getAvailableQualities(): List<VideoQuality> {
        // In a real implementation, this would check network conditions
        // and return appropriate quality options
        return listOf(
            VideoQuality.AUTO,
            VideoQuality.HIGH,
            VideoQuality.MEDIUM,
            VideoQuality.LOW
        )
    }

    /**
     * Video quality options
     */
    enum class VideoQuality(val displayName: String, val bitrate: Int) {
        AUTO("Auto", -1),
        HIGH("High (720p)", 2500000),
        MEDIUM("Medium (480p)", 1000000),
        LOW("Low (360p)", 500000)
    }
}