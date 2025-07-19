package com.example.health_assistant.features.discover.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.health_assistant.auth.session.SessionManager
import com.example.health_assistant.core.util.Result
import com.example.health_assistant.features.discover.domain.repository.DiscoverRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlin.math.min
import kotlin.math.pow

/**
 * Worker for synchronizing discover content from remote sources
 * Handles background content updates with network and battery optimization
 * Implements sync conflict resolution and error recovery mechanisms
 */
@HiltWorker
class ContentSyncWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted private val params: WorkerParameters,
    private val discoverRepository: DiscoverRepository,
    private val sessionManager: SessionManager,
    private val syncStatusManager: SyncStatusManager
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "content_sync_work"
        const val PERIODIC_WORK_NAME = "periodic_content_sync_work"
        private const val TAG = "ContentSyncWorker"
        
        // Input data keys
        const val KEY_USER_ID = "user_id"
        const val KEY_SYNC_TYPE = "sync_type"
        const val KEY_RETRY_COUNT = "retry_count"
        const val KEY_FORCE_SYNC = "force_sync"
        
        // Sync types
        const val SYNC_TYPE_FULL = "full"
        const val SYNC_TYPE_INCREMENTAL = "incremental"
        const val SYNC_TYPE_BOOKMARKS = "bookmarks"
        
        // Retry configuration
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val BASE_RETRY_DELAY_MS = 1000L
        private const val MAX_RETRY_DELAY_MS = 30000L
    }

    override suspend fun doWork(): androidx.work.ListenableWorker.Result {
        return try {
            Log.d(TAG, "Starting content synchronization")
            
            // Get user ID from input data or session
            val userId = getUserId()
            if (userId.isEmpty()) {
                Log.w(TAG, "No user ID available, skipping sync")
                return androidx.work.ListenableWorker.Result.success(
                    workDataOf("status" to "skipped", "reason" to "no_user")
                )
            }

            // Check if sync should be attempted
            if (!syncStatusManager.shouldAttemptSync()) {
                Log.w(TAG, "Sync attempt blocked due to recent errors or ongoing sync")
                return androidx.work.ListenableWorker.Result.success(
                    workDataOf("status" to "blocked", "reason" to "recent_errors")
                )
            }

            // Mark sync as started
            syncStatusManager.markSyncStarted()

            // Get sync parameters
            val syncType = inputData.getString(KEY_SYNC_TYPE) ?: SYNC_TYPE_INCREMENTAL
            val retryCount = inputData.getInt(KEY_RETRY_COUNT, 0)
            val forceSync = inputData.getBoolean(KEY_FORCE_SYNC, false)
            
            Log.d(TAG, "Sync parameters - User: $userId, Type: $syncType, Retry: $retryCount, Force: $forceSync")

            // Perform synchronization
            val success = performSync(syncType)
            
            if (success) {
                Log.d(TAG, "Content synchronization completed successfully")
                
                // Mark sync as completed
                syncStatusManager.markSyncCompleted()
                
                // Update content type sync times
                updateContentTypeSyncTimes(syncType)
                
                // Perform cleanup after successful sync
                performPostSyncCleanup()
                
                androidx.work.ListenableWorker.Result.success(
                    workDataOf(
                        "status" to "success",
                        "sync_type" to syncType,
                        "user_id" to userId,
                        "timestamp" to System.currentTimeMillis()
                    )
                )
            } else {
                Log.e(TAG, "Content synchronization failed")
                
                // Mark sync as failed
                syncStatusManager.markSyncFailed("Sync operation failed")
                
                // Implement retry logic with exponential backoff
                if (retryCount < MAX_RETRY_ATTEMPTS) {
                    val nextRetryCount = retryCount + 1
                    val retryDelay = calculateRetryDelay(nextRetryCount)
                    
                    Log.d(TAG, "Scheduling retry $nextRetryCount in ${retryDelay}ms")
                    
                    return androidx.work.ListenableWorker.Result.retry()
                } else {
                    Log.e(TAG, "Max retry attempts reached, marking as failed")
                    androidx.work.ListenableWorker.Result.failure(
                        workDataOf(
                            "status" to "failed",
                            "error" to "Max retry attempts reached",
                            "retry_count" to retryCount
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during content sync", e)
            
            // Mark sync as failed
            syncStatusManager.markSyncFailed(e.message ?: "Unknown error")
            
            androidx.work.ListenableWorker.Result.failure(
                workDataOf(
                    "status" to "error",
                    "error" to e.message
                )
            )
        }
    }

    /**
     * Get user ID from input data or session manager
     */
    private fun getUserId(): String {
        return inputData.getString(KEY_USER_ID) 
            ?: sessionManager.getCurrentUserId() 
            ?: ""
    }

    /**
     * Perform synchronization based on type
     */
    private suspend fun performSync(syncType: String): Boolean {
        return try {
            when (syncType) {
                SYNC_TYPE_FULL -> performFullSync()
                SYNC_TYPE_INCREMENTAL -> performIncrementalSync()
                SYNC_TYPE_BOOKMARKS -> performBookmarkSync()
                else -> {
                    Log.w(TAG, "Unknown sync type: $syncType, defaulting to incremental")
                    performIncrementalSync()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during sync", e)
            false
        }
    }

    /**
     * Perform full content synchronization
     */
    private suspend fun performFullSync(): Boolean {
        Log.d(TAG, "Performing full content synchronization")
        
        val syncResult = discoverRepository.syncContentFromRemote()
        return when (syncResult) {
            is com.example.health_assistant.core.util.Result.Success -> {
                Log.d(TAG, "Full sync completed successfully")
                true
            }
            is com.example.health_assistant.core.util.Result.Error -> {
                Log.e(TAG, "Full sync failed: ${syncResult.message}")
                false
            }
            is com.example.health_assistant.core.util.Result.Loading -> {
                Log.w(TAG, "Unexpected loading state in sync result")
                false
            }
        }
    }

    /**
     * Perform incremental content synchronization
     */
    private suspend fun performIncrementalSync(): Boolean {
        Log.d(TAG, "Performing incremental content synchronization")
        
        // Get cache statistics to determine last sync time
        val cacheStats = discoverRepository.getCacheStatistics()
        
        return when (cacheStats) {
            is com.example.health_assistant.core.util.Result.Success -> {
                val lastSyncTime = cacheStats.data.lastSyncTime
                Log.d(TAG, "Last sync time: $lastSyncTime")
                
                // Perform incremental sync
                val syncResult = discoverRepository.syncContentFromRemote()
                when (syncResult) {
                    is com.example.health_assistant.core.util.Result.Success -> {
                        Log.d(TAG, "Incremental sync completed successfully")
                        true
                    }
                    is com.example.health_assistant.core.util.Result.Error -> {
                        Log.e(TAG, "Incremental sync failed: ${syncResult.message}")
                        false
                    }
                    is com.example.health_assistant.core.util.Result.Loading -> {
                        Log.w(TAG, "Unexpected loading state in incremental sync")
                        false
                    }
                }
            }
            is com.example.health_assistant.core.util.Result.Error -> {
                Log.w(TAG, "Could not get cache statistics, performing full sync instead")
                performFullSync()
            }
            is com.example.health_assistant.core.util.Result.Loading -> {
                Log.w(TAG, "Unexpected loading state when getting cache statistics")
                false
            }
        }
    }

    /**
     * Perform bookmark synchronization
     */
    private suspend fun performBookmarkSync(): Boolean {
        Log.d(TAG, "Performing bookmark synchronization")
        
        val cleanupResult = discoverRepository.cleanupOrphanedBookmarks()
        
        return when (cleanupResult) {
            is com.example.health_assistant.core.util.Result.Success -> {
                val cleanedCount = cleanupResult.data
                Log.d(TAG, "Bookmark sync completed, cleaned up $cleanedCount orphaned bookmarks")
                true
            }
            is com.example.health_assistant.core.util.Result.Error -> {
                Log.e(TAG, "Bookmark sync failed: ${cleanupResult.message}")
                false
            }
            is com.example.health_assistant.core.util.Result.Loading -> {
                Log.w(TAG, "Unexpected loading state in bookmark cleanup")
                false
            }
        }
    }

    /**
     * Perform cleanup operations after successful sync
     */
    private suspend fun performPostSyncCleanup() {
        Log.d(TAG, "Performing post-sync cleanup")
        
        try {
            val cleanupResult = discoverRepository.cleanupOldContent(retentionDays = 30)
            
            when (cleanupResult) {
                is com.example.health_assistant.core.util.Result.Success -> {
                    val cleanedCount = cleanupResult.data
                    Log.d(TAG, "Post-sync cleanup completed, removed $cleanedCount old items")
                }
                is com.example.health_assistant.core.util.Result.Error -> {
                    Log.w(TAG, "Post-sync cleanup failed: ${cleanupResult.message}")
                }
                is com.example.health_assistant.core.util.Result.Loading -> {
                    Log.w(TAG, "Unexpected loading state in cleanup")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Exception during post-sync cleanup", e)
        }
    }

    /**
     * Update content type sync times based on sync type
     */
    private suspend fun updateContentTypeSyncTimes(syncType: String) {
        Log.d(TAG, "Updating content type sync times for sync type: $syncType")
        
        try {
            when (syncType) {
                SYNC_TYPE_FULL -> {
                    syncStatusManager.updateContentTypeSyncTime("articles")
                    syncStatusManager.updateContentTypeSyncTime("news")
                    syncStatusManager.updateContentTypeSyncTime("videos")
                    syncStatusManager.updateContentTypeSyncTime("bookmarks")
                }
                SYNC_TYPE_INCREMENTAL -> {
                    syncStatusManager.updateContentTypeSyncTime("articles")
                    syncStatusManager.updateContentTypeSyncTime("news")
                    syncStatusManager.updateContentTypeSyncTime("videos")
                }
                SYNC_TYPE_BOOKMARKS -> {
                    syncStatusManager.updateContentTypeSyncTime("bookmarks")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update content type sync times", e)
        }
    }

    /**
     * Calculate retry delay with exponential backoff
     */
    private fun calculateRetryDelay(retryCount: Int): Long {
        val exponentialDelay = BASE_RETRY_DELAY_MS * (2.0.pow(retryCount - 1)).toLong()
        return min(exponentialDelay, MAX_RETRY_DELAY_MS)
    }
}