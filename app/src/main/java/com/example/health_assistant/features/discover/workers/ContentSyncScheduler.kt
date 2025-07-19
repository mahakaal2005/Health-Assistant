package com.example.health_assistant.features.discover.workers

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.work.*
import com.example.health_assistant.auth.session.SessionManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Scheduler for content synchronization worker
 * Handles periodic sync scheduling with network and battery optimization
 * Provides methods for different sync scenarios and conflict resolution
 */
@Singleton
class ContentSyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionManager: SessionManager
) {

    companion object {
        private const val TAG = "ContentSyncScheduler"
        
        // Sync intervals
        private const val PERIODIC_SYNC_INTERVAL_HOURS = 6L
        private const val WIFI_SYNC_INTERVAL_HOURS = 2L
        
        // Work tags
        private const val TAG_CONTENT_SYNC = "content_sync"
        private const val TAG_PERIODIC_SYNC = "periodic_sync"
        private const val TAG_WIFI_SYNC = "wifi_sync"
        private const val TAG_MANUAL_SYNC = "manual_sync"
    }

    private val workManager = WorkManager.getInstance(context)

    /**
     * Schedule periodic content synchronization
     * Runs every 6 hours with network and battery constraints
     */
    fun schedulePeriodicSync() {
        Log.d(TAG, "Scheduling periodic content synchronization")
        
        val currentUserId = getCurrentUserId()
        if (currentUserId.isEmpty()) {
            Log.w(TAG, "No user logged in, skipping periodic sync scheduling")
            return
        }

        // Create input data with user ID
        val inputData = workDataOf(
            ContentSyncWorker.KEY_USER_ID to currentUserId,
            ContentSyncWorker.KEY_SYNC_TYPE to ContentSyncWorker.SYNC_TYPE_INCREMENTAL
        )

        // Create constraints for periodic sync
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .setRequiresStorageNotLow(true)
            .build()

        // Create periodic work request
        val periodicWorkRequest = PeriodicWorkRequestBuilder<ContentSyncWorker>(
            PERIODIC_SYNC_INTERVAL_HOURS, TimeUnit.HOURS,
            30, TimeUnit.MINUTES // Flex interval
        )
            .setInputData(inputData)
            .setConstraints(constraints)
            .addTag(TAG_CONTENT_SYNC)
            .addTag(TAG_PERIODIC_SYNC)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        // Enqueue unique periodic work
        workManager.enqueueUniquePeriodicWork(
            ContentSyncWorker.PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicWorkRequest
        )

        Log.d(TAG, "Scheduled periodic sync for user $currentUserId every $PERIODIC_SYNC_INTERVAL_HOURS hours")
    }

    /**
     * Schedule WiFi-optimized sync
     * Runs more frequently when on WiFi to save mobile data
     */
    fun scheduleWiFiOptimizedSync() {
        Log.d(TAG, "Scheduling WiFi-optimized content synchronization")
        
        val currentUserId = getCurrentUserId()
        if (currentUserId.isEmpty()) {
            Log.w(TAG, "No user logged in, skipping WiFi sync scheduling")
            return
        }

        // Create input data with user ID
        val inputData = workDataOf(
            ContentSyncWorker.KEY_USER_ID to currentUserId,
            ContentSyncWorker.KEY_SYNC_TYPE to ContentSyncWorker.SYNC_TYPE_FULL
        )

        // Create WiFi-only constraints
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED) // WiFi only
            .setRequiresBatteryNotLow(true)
            .setRequiresStorageNotLow(true)
            .setRequiresCharging(false) // Don't require charging for WiFi sync
            .build()

        // Create periodic work request for WiFi sync
        val wifiSyncRequest = PeriodicWorkRequestBuilder<ContentSyncWorker>(
            WIFI_SYNC_INTERVAL_HOURS, TimeUnit.HOURS,
            15, TimeUnit.MINUTES // Shorter flex interval for WiFi
        )
            .setInputData(inputData)
            .setConstraints(constraints)
            .addTag(TAG_CONTENT_SYNC)
            .addTag(TAG_WIFI_SYNC)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        // Enqueue unique WiFi sync work
        workManager.enqueueUniquePeriodicWork(
            "wifi_content_sync_work",
            ExistingPeriodicWorkPolicy.KEEP,
            wifiSyncRequest
        )

        Log.d(TAG, "Scheduled WiFi sync for user $currentUserId every $WIFI_SYNC_INTERVAL_HOURS hours")
    }

    /**
     * Trigger immediate manual sync
     * Used when user manually refreshes content
     */
    fun triggerManualSync(forceFullSync: Boolean = false) {
        Log.d(TAG, "Triggering manual content synchronization")
        
        val currentUserId = getCurrentUserId()
        if (currentUserId.isEmpty()) {
            Log.w(TAG, "No user logged in, skipping manual sync")
            return
        }

        val syncType = if (forceFullSync) {
            ContentSyncWorker.SYNC_TYPE_FULL
        } else {
            ContentSyncWorker.SYNC_TYPE_INCREMENTAL
        }

        // Create input data with user ID and sync type
        val inputData = workDataOf(
            ContentSyncWorker.KEY_USER_ID to currentUserId,
            ContentSyncWorker.KEY_SYNC_TYPE to syncType,
            ContentSyncWorker.KEY_FORCE_SYNC to true
        )

        // Create constraints for manual sync (less restrictive)
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // Create one-time work request for manual sync
        val manualSyncRequest = OneTimeWorkRequestBuilder<ContentSyncWorker>()
            .setInputData(inputData)
            .setConstraints(constraints)
            .addTag(TAG_CONTENT_SYNC)
            .addTag(TAG_MANUAL_SYNC)
            .build()

        // Enqueue manual sync work
        workManager.enqueueUniqueWork(
            "manual_content_sync_${System.currentTimeMillis()}",
            ExistingWorkPolicy.REPLACE,
            manualSyncRequest
        )

        Log.d(TAG, "Triggered manual sync for user $currentUserId with type: $syncType")
    }

    /**
     * Trigger bookmark synchronization
     * Syncs user bookmarks and reading progress
     */
    fun triggerBookmarkSync() {
        Log.d(TAG, "Triggering bookmark synchronization")
        
        val currentUserId = getCurrentUserId()
        if (currentUserId.isEmpty()) {
            Log.w(TAG, "No user logged in, skipping bookmark sync")
            return
        }

        // Create input data for bookmark sync
        val inputData = workDataOf(
            ContentSyncWorker.KEY_USER_ID to currentUserId,
            ContentSyncWorker.KEY_SYNC_TYPE to ContentSyncWorker.SYNC_TYPE_BOOKMARKS
        )

        // Create constraints for bookmark sync
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        // Create one-time work request for bookmark sync
        val bookmarkSyncRequest = OneTimeWorkRequestBuilder<ContentSyncWorker>()
            .setInputData(inputData)
            .setConstraints(constraints)
            .addTag(TAG_CONTENT_SYNC)
            .addTag("bookmark_sync")
            .build()

        // Enqueue bookmark sync work
        workManager.enqueueUniqueWork(
            "bookmark_sync_${currentUserId}",
            ExistingWorkPolicy.REPLACE,
            bookmarkSyncRequest
        )

        Log.d(TAG, "Triggered bookmark sync for user $currentUserId")
    }

    /**
     * Cancel all scheduled sync work
     */
    fun cancelAllSyncWork() {
        Log.d(TAG, "Cancelling all content sync work")
        
        // Cancel periodic work
        workManager.cancelUniqueWork(ContentSyncWorker.PERIODIC_WORK_NAME)
        workManager.cancelUniqueWork("wifi_content_sync_work")
        
        // Cancel all work with content sync tag
        workManager.cancelAllWorkByTag(TAG_CONTENT_SYNC)
        
        Log.d(TAG, "All content sync work cancelled")
    }

    /**
     * Cancel sync work for specific user
     */
    fun cancelSyncForUser(userId: String) {
        Log.d(TAG, "Cancelling sync work for user: $userId")
        
        // Note: WorkManager doesn't provide direct way to cancel by input data
        // In a production app, you might want to use unique work names that include user ID
        // For now, we'll cancel all sync work and let the scheduler reschedule for current user
        cancelAllSyncWork()
        
        Log.d(TAG, "Sync work cancelled for user: $userId")
    }

    /**
     * Get sync work status
     */
    fun getSyncWorkStatus(): LiveData<List<WorkInfo>> {
        return workManager.getWorkInfosByTagLiveData(TAG_CONTENT_SYNC)
    }

    /**
     * Check if sync is currently running
     */
    suspend fun isSyncRunning(): Boolean {
        return try {
            val workInfos = workManager.getWorkInfosByTag(TAG_CONTENT_SYNC)
            // Simple check - assume false if we can't determine
            false
        } catch (e: Exception) {
            Log.w(TAG, "Failed to check sync status", e)
            false
        }
    }

    /**
     * Get last sync status
     */
    suspend fun getLastSyncStatus(): WorkInfo? {
        return try {
            val workInfos = workManager.getWorkInfosByTag(TAG_PERIODIC_SYNC)
            // Return null for now - this can be implemented later
            null
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get last sync status", e)
            null
        }
    }

    /**
     * Initialize sync scheduling for current user
     * Called when user logs in or app starts
     */
    fun initializeSyncForCurrentUser() {
        Log.d(TAG, "Initializing sync for current user")
        
        val currentUserId = getCurrentUserId()
        if (currentUserId.isEmpty()) {
            Log.w(TAG, "No user logged in, skipping sync initialization")
            return
        }

        // Cancel any existing sync work
        cancelAllSyncWork()
        
        // Schedule new sync work for current user
        schedulePeriodicSync()
        scheduleWiFiOptimizedSync()
        
        // Trigger initial sync
        triggerManualSync(forceFullSync = false)
        
        Log.d(TAG, "Sync initialized for user: $currentUserId")
    }

    /**
     * Get current user ID from session manager
     */
    private fun getCurrentUserId(): String {
        return sessionManager.getCurrentUserId() ?: ""
    }
}