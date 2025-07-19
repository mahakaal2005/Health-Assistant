package com.example.health_assistant.features.discover.data.cache

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.util.LruCache
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages thumbnail caching for video content
 * Provides memory and disk caching for video thumbnails
 */
@Singleton
class ThumbnailCacheManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private const val TAG = "ThumbnailCacheManager"
        private const val CACHE_DIR_NAME = "video_thumbnails"
        private const val MAX_MEMORY_CACHE_SIZE = 10 * 1024 * 1024 // 10MB
        private const val MAX_DISK_CACHE_SIZE = 50 * 1024 * 1024L // 50MB
    }

    // Memory cache for quick access
    private val memoryCache = object : LruCache<String, Bitmap>(MAX_MEMORY_CACHE_SIZE) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount
        }
    }

    // Disk cache directory
    private val cacheDir: File by lazy {
        File(context.cacheDir, CACHE_DIR_NAME).apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }

    /**
     * Get thumbnail from cache or download if not available
     */
    suspend fun getThumbnail(videoId: String, thumbnailUrl: String): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                // Check memory cache first
                memoryCache.get(videoId)?.let { return@withContext it }

                // Check disk cache
                val diskCachedBitmap = loadFromDiskCache(videoId)
                if (diskCachedBitmap != null) {
                    // Add to memory cache for quick access
                    memoryCache.put(videoId, diskCachedBitmap)
                    return@withContext diskCachedBitmap
                }

                // Download and cache thumbnail
                downloadAndCacheThumbnail(videoId, thumbnailUrl)
            } catch (e: Exception) {
                Log.e(TAG, "Error getting thumbnail for video: $videoId", e)
                null
            }
        }
    }

    /**
     * Preload thumbnail for offline access
     */
    suspend fun preloadThumbnail(videoId: String, thumbnailUrl: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Check if already cached
                if (isThumbnailCached(videoId)) {
                    return@withContext true
                }

                // Download and cache
                val bitmap = downloadAndCacheThumbnail(videoId, thumbnailUrl)
                bitmap != null
            } catch (e: Exception) {
                Log.e(TAG, "Error preloading thumbnail for video: $videoId", e)
                false
            }
        }
    }

    /**
     * Check if thumbnail is cached
     */
    fun isThumbnailCached(videoId: String): Boolean {
        return memoryCache.get(videoId) != null || getDiskCacheFile(videoId).exists()
    }

    /**
     * Clear thumbnail cache for specific video
     */
    suspend fun clearThumbnail(videoId: String) {
        withContext(Dispatchers.IO) {
            try {
                // Remove from memory cache
                memoryCache.remove(videoId)

                // Remove from disk cache
                val cacheFile = getDiskCacheFile(videoId)
                if (cacheFile.exists()) {
                    cacheFile.delete()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing thumbnail for video: $videoId", e)
            }
        }
    }

    /**
     * Clear all cached thumbnails
     */
    suspend fun clearAllThumbnails() {
        withContext(Dispatchers.IO) {
            try {
                // Clear memory cache
                memoryCache.evictAll()

                // Clear disk cache
                cacheDir.listFiles()?.forEach { file ->
                    if (file.isFile) {
                        file.delete()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing all thumbnails", e)
            }
        }
    }

    /**
     * Get cache size information
     */
    suspend fun getCacheInfo(): ThumbnailCacheInfo {
        return withContext(Dispatchers.IO) {
            try {
                val diskCacheSize = cacheDir.listFiles()?.sumOf { it.length() } ?: 0L
                val memoryCacheSize = memoryCache.size()
                val thumbnailCount = cacheDir.listFiles()?.size ?: 0

                ThumbnailCacheInfo(
                    diskCacheSize = diskCacheSize,
                    memoryCacheSize = memoryCacheSize,
                    thumbnailCount = thumbnailCount,
                    maxDiskCacheSize = MAX_DISK_CACHE_SIZE,
                    maxMemoryCacheSize = MAX_MEMORY_CACHE_SIZE
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error getting cache info", e)
                ThumbnailCacheInfo()
            }
        }
    }

    /**
     * Cleanup old thumbnails to manage cache size
     */
    suspend fun cleanupOldThumbnails() {
        withContext(Dispatchers.IO) {
            try {
                val cacheFiles = cacheDir.listFiles() ?: return@withContext
                val totalSize = cacheFiles.sumOf { it.length() }

                if (totalSize > MAX_DISK_CACHE_SIZE) {
                    // Sort by last modified time (oldest first)
                    val sortedFiles = cacheFiles.sortedBy { it.lastModified() }
                    var currentSize = totalSize

                    for (file in sortedFiles) {
                        if (currentSize <= MAX_DISK_CACHE_SIZE * 0.8) break // Keep 80% of max size

                        if (file.delete()) {
                            currentSize -= file.length()
                            Log.d(TAG, "Deleted old thumbnail: ${file.name}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error cleaning up old thumbnails", e)
            }
        }
    }

    // Private helper methods

    private suspend fun downloadAndCacheThumbnail(videoId: String, thumbnailUrl: String): Bitmap? {
        return try {
            // Download bitmap
            val bitmap = downloadBitmap(thumbnailUrl) ?: return null

            // Save to disk cache
            saveToDiskCache(videoId, bitmap)

            // Add to memory cache
            memoryCache.put(videoId, bitmap)

            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading and caching thumbnail", e)
            null
        }
    }

    private suspend fun downloadBitmap(url: String): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                val connection = URL(url).openConnection()
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                connection.connect()

                val inputStream = connection.getInputStream()
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream.close()

                bitmap
            } catch (e: Exception) {
                Log.e(TAG, "Error downloading bitmap from: $url", e)
                null
            }
        }
    }

    private fun saveToDiskCache(videoId: String, bitmap: Bitmap) {
        try {
            val cacheFile = getDiskCacheFile(videoId)
            val outputStream = FileOutputStream(cacheFile)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            outputStream.close()
        } catch (e: IOException) {
            Log.e(TAG, "Error saving bitmap to disk cache", e)
        }
    }

    private fun loadFromDiskCache(videoId: String): Bitmap? {
        return try {
            val cacheFile = getDiskCacheFile(videoId)
            if (cacheFile.exists()) {
                BitmapFactory.decodeFile(cacheFile.absolutePath)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading bitmap from disk cache", e)
            null
        }
    }

    private fun getDiskCacheFile(videoId: String): File {
        return File(cacheDir, "${videoId}.jpg")
    }
}

/**
 * Data class containing thumbnail cache information
 */
data class ThumbnailCacheInfo(
    val diskCacheSize: Long = 0L,
    val memoryCacheSize: Int = 0,
    val thumbnailCount: Int = 0,
    val maxDiskCacheSize: Long = 0L,
    val maxMemoryCacheSize: Int = 0
)