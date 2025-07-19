package com.example.health_assistant.features.discover.workers

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.example.health_assistant.auth.session.SessionManager
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.*

/**
 * Simplified unit tests for SyncStatusManager
 * Tests basic sync status tracking functionality
 */
class SyncStatusManagerTest {

    private lateinit var context: Context
    private lateinit var sessionManager: SessionManager
    private lateinit var syncStatusManager: SyncStatusManager

    @BeforeEach
    fun setup() {
        context = mockk(relaxed = true)
        sessionManager = mockk(relaxed = true)

        // Default mock behaviors
        every { sessionManager.getCurrentUserId() } returns "test_user_123"

        syncStatusManager = SyncStatusManager(context, sessionManager)
    }

    @Test
    fun `SyncStatus should have correct default values`() {
        // Given
        val syncStatus = SyncStatusManager.SyncStatus()

        // Then
        assertEquals(0L, syncStatus.lastSyncTime)
        assertEquals(0L, syncStatus.lastSuccessfulSync)
        assertFalse(syncStatus.syncInProgress)
        assertEquals(0, syncStatus.errorCount)
        assertNull(syncStatus.lastError)
        assertEquals(0L, syncStatus.articlesSyncTime)
        assertEquals(0L, syncStatus.newsSyncTime)
        assertEquals(0L, syncStatus.videosSyncTime)
        assertEquals(0L, syncStatus.bookmarksSyncTime)
        assertEquals(0, syncStatus.conflictCount)
        assertEquals(0L, syncStatus.lastConflictResolution)
    }

    @Test
    fun `SyncStatus isSyncOverdue should return true when last sync is over 8 hours ago`() {
        // Given
        val nineHoursAgo = System.currentTimeMillis() - (9 * 60 * 60 * 1000)
        val syncStatus = SyncStatusManager.SyncStatus(lastSuccessfulSync = nineHoursAgo)

        // When
        val isOverdue = syncStatus.isSyncOverdue()

        // Then
        assertTrue(isOverdue)
    }

    @Test
    fun `SyncStatus isSyncOverdue should return false when last sync is recent`() {
        // Given
        val oneHourAgo = System.currentTimeMillis() - (1 * 60 * 60 * 1000)
        val syncStatus = SyncStatusManager.SyncStatus(lastSuccessfulSync = oneHourAgo)

        // When
        val isOverdue = syncStatus.isSyncOverdue()

        // Then
        assertFalse(isOverdue)
    }

    @Test
    fun `SyncStatus hasRecentErrors should return true when there are errors`() {
        // Given
        val syncStatus = SyncStatusManager.SyncStatus(
            errorCount = 2,
            lastError = "Network error"
        )

        // When
        val hasErrors = syncStatus.hasRecentErrors()

        // Then
        assertTrue(hasErrors)
    }

    @Test
    fun `SyncStatus hasRecentErrors should return false when no errors`() {
        // Given
        val syncStatus = SyncStatusManager.SyncStatus(
            errorCount = 0,
            lastError = null
        )

        // When
        val hasErrors = syncStatus.hasRecentErrors()

        // Then
        assertFalse(hasErrors)
    }

    @Test
    fun `SyncStatus getSyncHealth should return correct health status`() {
        // Test SYNCING
        val syncingStatus = SyncStatusManager.SyncStatus(syncInProgress = true)
        assertEquals(SyncStatusManager.SyncHealth.SYNCING, syncingStatus.getSyncHealth())

        // Test ERROR
        val errorStatus = SyncStatusManager.SyncStatus(errorCount = 5, lastError = "Error")
        assertEquals(SyncStatusManager.SyncHealth.ERROR, errorStatus.getSyncHealth())

        // Test STALE
        val tenHoursAgo = System.currentTimeMillis() - (10 * 60 * 60 * 1000)
        val staleStatus = SyncStatusManager.SyncStatus(lastSuccessfulSync = tenHoursAgo)
        assertEquals(SyncStatusManager.SyncHealth.STALE, staleStatus.getSyncHealth())

        // Test CONFLICTS
        val twoDaysAgo = System.currentTimeMillis() - (2 * 24 * 60 * 60 * 1000)
        val conflictStatus = SyncStatusManager.SyncStatus(
            conflictCount = 1,
            lastConflictResolution = twoDaysAgo
        )
        assertEquals(SyncStatusManager.SyncHealth.CONFLICTS, conflictStatus.getSyncHealth())

        // Test HEALTHY
        val recentTime = System.currentTimeMillis() - (1 * 60 * 60 * 1000)
        val healthyStatus = SyncStatusManager.SyncStatus(lastSuccessfulSync = recentTime)
        assertEquals(SyncStatusManager.SyncHealth.HEALTHY, healthyStatus.getSyncHealth())
    }

    @Test
    fun `resolveSyncConflict should return remote version`() = runTest {
        // Given
        val localVersion = "local_data"
        val remoteVersion = "remote_data"

        // When
        val result = syncStatusManager.resolveSyncConflict(
            "article_123",
            "article",
            localVersion,
            remoteVersion
        )

        // Then
        assertEquals(remoteVersion, result)
    }

    @Test
    fun `getSyncRetryDelay should return base delay for first retry`() = runTest {
        // When
        val delay = syncStatusManager.getSyncRetryDelay()

        // Then
        assertEquals(1000L, delay) // Base delay
    }

    @Test
    fun `shouldAttemptSync should return true by default`() = runTest {
        // When
        val shouldAttempt = syncStatusManager.shouldAttemptSync()

        // Then - Should return true by default since we can't easily mock DataStore
        assertTrue(shouldAttempt)
    }
}