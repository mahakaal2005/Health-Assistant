package com.example.health_assistant.features.discover.workers

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.health_assistant.auth.session.SessionManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for tracking content synchronization status and handling conflicts
 * Provides sync status reporting and error recovery mechanisms
 */
@Singleton
class SyncStatusManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionManager: SessionManager
) {

    companion object {
        private const val TAG = "SyncStatusManager"
        private const val SYNC_STATUS_DATASTORE = "sync_status_datastore"
        
        // DataStore keys
        private val LAST_SYNC_TIME = longPreferencesKey("last_sync_time")
        private val LAST_SUCCESSFUL_SYNC = longPreferencesKey("last_successful_sync")
        private val SYNC_IN_PROGRESS = booleanPreferencesKey("sync_in_progress")
        private val SYNC_ERROR_COUNT = intPreferencesKey("sync_error_count")
        private val LAST_SYNC_ERROR = stringPreferencesKey("last_sync_error")
        private val ARTICLES_SYNC_TIME = longPreferencesKey("articles_sync_time")
        private val NEWS_SYNC_TIME = longPreferencesKey("news_sync_time")
        private val VIDEOS_SYNC_TIME = longPreferencesKey("videos_sync_time")
        private val BOOKMARKS_SYNC_TIME = longPreferencesKey("bookmarks_sync_time")
        private val SYNC_CONFLICT_COUNT = intPreferencesKey("sync_conflict_count")
        private val LAST_CONFLICT_RESOLUTION = longPreferencesKey("last_conflict_resolution")
    }

    // DataStore extension
    private val Context.syncStatusDataStore: DataStore<Preferences> by preferencesDataStore(
        name = SYNC_STATUS_DATASTORE
    )

    private val dataStore = context.syncStatusDataStore

    /**
     * Data class for sync status information
     */
    data class SyncStatus(
        val lastSyncTime: Long = 0L,
        val lastSuccessfulSync: Long = 0L,
        val syncInProgress: Boolean = false,
        val errorCount: Int = 0,
        val lastError: String? = null,
        val articlesSyncTime: Long = 0L,
        val newsSyncTime: Long = 0L,
        val videosSyncTime: Long = 0L,
        val bookmarksSyncTime: Long = 0L,
        val conflictCount: Int = 0,
        val lastConflictResolution: Long = 0L
    ) {
        /**
         * Check if sync is overdue (more than 8 hours since last successful sync)
         */
        fun isSyncOverdue(): Boolean {
            val eightHoursAgo = System.currentTimeMillis() - (8 * 60 * 60 * 1000)
            return lastSuccessfulSync < eightHoursAgo
        }

        /**
         * Check if there are recent sync errors
         */
        fun hasRecentErrors(): Boolean {
            return errorCount > 0 && lastError != null
        }

        /**
         * Check if sync conflicts need attention
         */
        fun hasUnresolvedConflicts(): Boolean {
            return conflictCount > 0 && lastConflictResolution < (System.currentTimeMillis() - (24 * 60 * 60 * 1000))
        }

        /**
         * Get sync health status
         */
        fun getSyncHealth(): SyncHealth {
            return when {
                syncInProgress -> SyncHealth.SYNCING
                hasRecentErrors() && errorCount > 3 -> SyncHealth.ERROR
                isSyncOverdue() -> SyncHealth.STALE
                hasUnresolvedConflicts() -> SyncHealth.CONFLICTS
                else -> SyncHealth.HEALTHY
            }
        }
    }

    /**
     * Enum for sync health status
     */
    enum class SyncHealth {
        HEALTHY,
        SYNCING,
        STALE,
        ERROR,
        CONFLICTS
    }

    /**
     * Get current sync status as Flow
     */
    fun getSyncStatus(): Flow<SyncStatus> {
        return dataStore.data.map { preferences ->
            SyncStatus(
                lastSyncTime = preferences[LAST_SYNC_TIME] ?: 0L,
                lastSuccessfulSync = preferences[LAST_SUCCESSFUL_SYNC] ?: 0L,
                syncInProgress = preferences[SYNC_IN_PROGRESS] ?: false,
                errorCount = preferences[SYNC_ERROR_COUNT] ?: 0,
                lastError = preferences[LAST_SYNC_ERROR],
                articlesSyncTime = preferences[ARTICLES_SYNC_TIME] ?: 0L,
                newsSyncTime = preferences[NEWS_SYNC_TIME] ?: 0L,
                videosSyncTime = preferences[VIDEOS_SYNC_TIME] ?: 0L,
                bookmarksSyncTime = preferences[BOOKMARKS_SYNC_TIME] ?: 0L,
                conflictCount = preferences[SYNC_CONFLICT_COUNT] ?: 0,
                lastConflictResolution = preferences[LAST_CONFLICT_RESOLUTION] ?: 0L
            )
        }
    }

    /**
     * Get current sync status (suspend function)
     */
    suspend fun getCurrentSyncStatus(): SyncStatus {
        return getSyncStatus().first()
    }

    /**
     * Mark sync as started
     */
    suspend fun markSyncStarted() {
        Log.d(TAG, "Marking sync as started")
        dataStore.edit { preferences ->
            preferences[SYNC_IN_PROGRESS] = true
            preferences[LAST_SYNC_TIME] = System.currentTimeMillis()
        }
    }

    /**
     * Mark sync as completed successfully
     */
    suspend fun markSyncCompleted() {
        Log.d(TAG, "Marking sync as completed successfully")
        val currentTime = System.currentTimeMillis()
        dataStore.edit { preferences ->
            preferences[SYNC_IN_PROGRESS] = false
            preferences[LAST_SUCCESSFUL_SYNC] = currentTime
            preferences[SYNC_ERROR_COUNT] = 0
            preferences[LAST_SYNC_ERROR] = ""
        }
    }

    /**
     * Mark sync as failed with error
     */
    suspend fun markSyncFailed(error: String) {
        Log.e(TAG, "Marking sync as failed: $error")
        dataStore.edit { preferences ->
            preferences[SYNC_IN_PROGRESS] = false
            val currentErrorCount = preferences[SYNC_ERROR_COUNT] ?: 0
            preferences[SYNC_ERROR_COUNT] = currentErrorCount + 1
            preferences[LAST_SYNC_ERROR] = error
        }
    }

    /**
     * Update sync time for specific content type
     */
    suspend fun updateContentTypeSyncTime(contentType: String) {
        Log.d(TAG, "Updating sync time for content type: $contentType")
        val currentTime = System.currentTimeMillis()
        dataStore.edit { preferences ->
            when (contentType.lowercase()) {
                "article", "articles" -> preferences[ARTICLES_SYNC_TIME] = currentTime
                "news" -> preferences[NEWS_SYNC_TIME] = currentTime
                "video", "videos" -> preferences[VIDEOS_SYNC_TIME] = currentTime
                "bookmark", "bookmarks" -> preferences[BOOKMARKS_SYNC_TIME] = currentTime
            }
        }
    }

    /**
     * Record sync conflict
     */
    suspend fun recordSyncConflict(conflictType: String, contentId: String) {
        Log.w(TAG, "Recording sync conflict - Type: $conflictType, Content: $contentId")
        dataStore.edit { preferences ->
            val currentConflictCount = preferences[SYNC_CONFLICT_COUNT] ?: 0
            preferences[SYNC_CONFLICT_COUNT] = currentConflictCount + 1
        }
    }

    /**
     * Mark conflict as resolved
     */
    suspend fun markConflictResolved() {
        Log.d(TAG, "Marking sync conflicts as resolved")
        dataStore.edit { preferences ->
            preferences[SYNC_CONFLICT_COUNT] = 0
            preferences[LAST_CONFLICT_RESOLUTION] = System.currentTimeMillis()
        }
    }

    /**
     * Reset sync status (used when user logs out or switches accounts)
     */
    suspend fun resetSyncStatus() {
        Log.d(TAG, "Resetting sync status")
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    /**
     * Get sync status summary for UI display
     */
    suspend fun getSyncStatusSummary(): String {
        val status = getCurrentSyncStatus()
        return when (status.getSyncHealth()) {
            SyncHealth.HEALTHY -> "Content is up to date"
            SyncHealth.SYNCING -> "Syncing content..."
            SyncHealth.STALE -> "Content may be outdated"
            SyncHealth.ERROR -> "Sync failed: ${status.lastError}"
            SyncHealth.CONFLICTS -> "Sync conflicts detected"
        }
    }

    /**
     * Check if incremental sync is possible
     * Returns true if we have recent sync timestamps for content types
     */
    suspend fun canPerformIncrementalSync(): Boolean {
        val status = getCurrentSyncStatus()
        val oneDayAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
        
        return status.lastSuccessfulSync > oneDayAgo &&
                status.articlesSyncTime > oneDayAgo &&
                status.newsSyncTime > oneDayAgo &&
                status.videosSyncTime > oneDayAgo
    }

    /**
     * Get content types that need sync based on their last sync time
     */
    suspend fun getContentTypesNeedingSync(): List<String> {
        val status = getCurrentSyncStatus()
        val sixHoursAgo = System.currentTimeMillis() - (6 * 60 * 60 * 1000)
        val contentTypesNeedingSync = mutableListOf<String>()

        if (status.articlesSyncTime < sixHoursAgo) {
            contentTypesNeedingSync.add("articles")
        }
        if (status.newsSyncTime < sixHoursAgo) {
            contentTypesNeedingSync.add("news")
        }
        if (status.videosSyncTime < sixHoursAgo) {
            contentTypesNeedingSync.add("videos")
        }
        if (status.bookmarksSyncTime < sixHoursAgo) {
            contentTypesNeedingSync.add("bookmarks")
        }

        return contentTypesNeedingSync
    }

    /**
     * Handle sync conflict resolution
     * Implements simple conflict resolution strategy: remote wins
     */
    suspend fun resolveSyncConflict(
        contentId: String,
        contentType: String,
        localVersion: Any,
        remoteVersion: Any
    ): Any {
        Log.d(TAG, "Resolving sync conflict for $contentType:$contentId")
        
        // Record the conflict
        recordSyncConflict("version_conflict", contentId)
        
        // Simple resolution strategy: remote wins
        // In a production app, you might want more sophisticated conflict resolution
        Log.d(TAG, "Conflict resolved: using remote version for $contentType:$contentId")
        
        return remoteVersion
    }

    /**
     * Get sync retry delay based on error count
     */
    suspend fun getSyncRetryDelay(): Long {
        val status = getCurrentSyncStatus()
        val baseDelay = 1000L // 1 second
        val maxDelay = 300000L // 5 minutes
        
        val exponentialDelay = baseDelay * (1L shl minOf(status.errorCount, 8))
        return minOf(exponentialDelay, maxDelay)
    }

    /**
     * Check if sync should be attempted based on error history
     */
    suspend fun shouldAttemptSync(): Boolean {
        val status = getCurrentSyncStatus()
        
        // Don't attempt if already syncing
        if (status.syncInProgress) {
            return false
        }
        
        // Don't attempt if too many recent errors (more than 5)
        if (status.errorCount > 5) {
            val lastErrorTime = status.lastSyncTime
            val oneHourAgo = System.currentTimeMillis() - (60 * 60 * 1000)
            
            // Only retry after an hour if there are too many errors
            return lastErrorTime < oneHourAgo
        }
        
        return true
    }
}