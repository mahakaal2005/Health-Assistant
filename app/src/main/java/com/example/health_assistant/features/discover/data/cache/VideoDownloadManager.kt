package com.example.health_assistant.features.discover.data.cache

import android.content.Context
import android.util.Log
import com.example.health_assistant.core.util.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages video downloads for offline viewing
 * Provides download progress tracking and cache management
 */
@Singleton
class VideoDownloadManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private const val TAG = "VideoDownloadManager"
        private const val CACHE_DIR_NAME = "offline_videos"
        private const val MAX_CACHE_SIZE = 500 * 1024 * 1024L // 500MB
        private const val BUFFER_SIZE = 8192
    }

    // Download progress tracking
    private val _downloadProgress = MutableStateFlow<Map<String, DownloadProgress>>(emptyMap())
    val downloadProgress: Flow<Map<String, DownloadProgress>> = _downloadProgress.asStateFlow()

    // Offline video cache directory
    private val cacheDir: File by lazy {
        File(context.cacheDir, CACHE_DIR_NAME).apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }

    /**
     * Download video for offline viewing
     */
    suspend fun downloadVideo(videoId: String, videoUrl: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // Check if already downloaded
                if (isVideoDownloaded(videoId)) {
                    return@withContext Result.Success(Unit)
                }

                // Update progress to starting
                updateDownloadProgress(videoId, DownloadProgress.Starting)

                // Download video
                val success = downloadVideoFile(videoId, videoUrl)
                
                if (success) {
                    updateDownloadProgress(videoId, DownloadProgress.Completed)
                    Result.Success(Unit)
                } else {
                    updateDownloadProgress(videoId, DownloadProgress.Failed("Download failed"))
                    Result.Error(Exception("Video download failed"), "Failed to download video")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error downloading video: $videoId", e)
                updateDownloadProgress(videoId, DownloadProgress.Failed(e.message ?: "Unknown error"))
                Result.Error(e, "Video download failed: ${e.message}")
            }
        }
    }

    /**
     * Remove downloaded video
     */
    suspend fun removeDownloadedVideo(videoId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val videoFile = getVideoFile(videoId)
                if (videoFile.exists()) {
                    val deleted = videoFile.delete()
                    if (deleted) {
                        // Remove from progress tracking
                        val currentProgress = _downloadProgress.value.toMutableMap()
                        currentProgress.remove(videoId)
                        _downloadProgress.value = currentProgress
                        
                        Result.Success(Unit)
                    } else {
                        Result.Error(Exception("Failed to delete file"), "Could not remove video file")
                    }
                } else {
                    Result.Success(Unit) // Already removed
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error removing downloaded video: $videoId", e)
                Result.Error(e, "Failed to remove video: ${e.message}")
            }
        }
    }

    /**
     * Check if video is downloaded
     */
    fun isVideoDownloaded(videoId: String): Boolean {
        return getVideoFile(videoId).exists()
    }

    /**
     * Get local video file path for offline playback
     */
    fun getOfflineVideoPath(videoId: String): String? {
        val videoFile = getVideoFile(videoId)
        return if (videoFile.exists()) {
            videoFile.absolutePath
        } else {
            null
        }
    }

    /**
     * Get download progress for specific video
     */
    fun getDownloadProgress(videoId: String): DownloadProgress? {
        return _downloadProgress.value[videoId]
    }

    /**
     * Cancel ongoing download
     */
    suspend fun cancelDownload(videoId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // Update progress to cancelled
                updateDownloadProgress(videoId, DownloadProgress.Cancelled)
                
                // Remove partial file if exists
                val videoFile = getVideoFile(videoId)
                if (videoFile.exists()) {
                    videoFile.delete()
                }
                
                Result.Success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Error cancelling download: $videoId", e)
                Result.Error(e, "Failed to cancel download")
            }
        }
    }

    /**
     * Get cache information
     */
    suspend fun getCacheInfo(): VideoCacheInfo {
        return withContext(Dispatchers.IO) {
            try {
                val videoFiles = cacheDir.listFiles() ?: emptyArray()
                val totalSize = videoFiles.sumOf { it.length() }
                val videoCount = videoFiles.size

                VideoCacheInfo(
                    totalSize = totalSize,
                    videoCount = videoCount,
                    maxCacheSize = MAX_CACHE_SIZE,
                    availableSpace = MAX_CACHE_SIZE - totalSize
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error getting cache info", e)
                VideoCacheInfo()
            }
        }
    }

    /**
     * Cleanup old downloaded videos to manage cache size
     */
    suspend fun cleanupOldVideos(): Result<Int> {
        return withContext(Dispatchers.IO) {
            try {
                val videoFiles = cacheDir.listFiles() ?: return@withContext Result.Success(0)
                val totalSize = videoFiles.sumOf { it.length() }

                if (totalSize <= MAX_CACHE_SIZE) {
                    return@withContext Result.Success(0)
                }

                // Sort by last accessed time (oldest first)
                val sortedFiles = videoFiles.sortedBy { it.lastModified() }
                var currentSize = totalSize
                var deletedCount = 0

                for (file in sortedFiles) {
                    if (currentSize <= MAX_CACHE_SIZE * 0.8) break // Keep 80% of max size

                    if (file.delete()) {
                        currentSize -= file.length()
                        deletedCount++
                        Log.d(TAG, "Deleted old video: ${file.name}")
                    }
                }

                Result.Success(deletedCount)
            } catch (e: Exception) {
                Log.e(TAG, "Error cleaning up old videos", e)
                Result.Error(e, "Failed to cleanup videos")
            }
        }
    }

    /**
     * Clear all downloaded videos
     */
    suspend fun clearAllDownloads(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val videoFiles = cacheDir.listFiles() ?: return@withContext Result.Success(Unit)
                
                for (file in videoFiles) {
                    file.delete()
                }

                // Clear progress tracking
                _downloadProgress.value = emptyMap()
                
                Result.Success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing all downloads", e)
                Result.Error(e, "Failed to clear downloads")
            }
        }
    }

    // Private helper methods

    private suspend fun downloadVideoFile(videoId: String, videoUrl: String): Boolean {
        return try {
            val connection = URL(videoUrl).openConnection()
            connection.connectTimeout = 30000
            connection.readTimeout = 30000
            
            val contentLength = connection.contentLength
            val inputStream = connection.getInputStream()
            
            val videoFile = getVideoFile(videoId)
            val outputStream = FileOutputStream(videoFile)
            
            val buffer = ByteArray(BUFFER_SIZE)
            var totalBytesRead = 0
            var bytesRead: Int
            
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalBytesRead += bytesRead
                
                // Update progress
                if (contentLength > 0) {
                    val progress = (totalBytesRead * 100) / contentLength
                    updateDownloadProgress(videoId, DownloadProgress.InProgress(progress))
                }
            }
            
            inputStream.close()
            outputStream.close()
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading video file", e)
            false
        }
    }

    private fun updateDownloadProgress(videoId: String, progress: DownloadProgress) {
        val currentProgress = _downloadProgress.value.toMutableMap()
        currentProgress[videoId] = progress
        _downloadProgress.value = currentProgress
    }

    private fun getVideoFile(videoId: String): File {
        return File(cacheDir, "${videoId}.mp4")
    }
}

/**
 * Sealed class representing download progress states
 */
sealed class DownloadProgress {
    object Starting : DownloadProgress()
    data class InProgress(val percentage: Int) : DownloadProgress()
    object Completed : DownloadProgress()
    object Cancelled : DownloadProgress()
    data class Failed(val error: String) : DownloadProgress()
}

/**
 * Data class containing video cache information
 */
data class VideoCacheInfo(
    val totalSize: Long = 0L,
    val videoCount: Int = 0,
    val maxCacheSize: Long = 0L,
    val availableSpace: Long = 0L
)