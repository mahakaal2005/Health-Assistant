package com.example.health_assistant.features.discover.workers

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.health_assistant.auth.session.SessionManager
import com.example.health_assistant.core.util.Result
import com.example.health_assistant.features.discover.domain.repository.DiscoverRepository
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for ContentSyncWorker
 * Tests background content synchronization functionality
 */
class ContentSyncWorkerTest {

    private lateinit var context: Context
    private lateinit var workerParameters: WorkerParameters
    private lateinit var discoverRepository: DiscoverRepository
    private lateinit var sessionManager: SessionManager
    private lateinit var syncStatusManager: SyncStatusManager
    private lateinit var contentSyncWorker: ContentSyncWorker

    @BeforeEach
    fun setup() {
        context = mockk(relaxed = true)
        workerParameters = mockk(relaxed = true)
        discoverRepository = mockk(relaxed = true)
        sessionManager = mockk(relaxed = true)
        syncStatusManager = mockk(relaxed = true)

        // Default mock behaviors
        every { sessionManager.getCurrentUserId() } returns "test_user_123"
        coEvery { syncStatusManager.shouldAttemptSync() } returns true
        coEvery { syncStatusManager.markSyncStarted() } just Runs
        coEvery { syncStatusManager.markSyncCompleted() } just Runs
        coEvery { syncStatusManager.markSyncFailed(any()) } just Runs
        coEvery { syncStatusManager.updateContentTypeSyncTime(any()) } just Runs
    }

    private fun createWorker(inputData: androidx.work.Data = workDataOf()): ContentSyncWorker {
        every { workerParameters.inputData } returns inputData
        return ContentSyncWorker(
            context = context,
            params = workerParameters,
            discoverRepository = discoverRepository,
            sessionManager = sessionManager,
            syncStatusManager = syncStatusManager
        )
    }

    @Test
    fun `doWork should skip sync when no user ID available`() = runTest {
        // Given
        every { sessionManager.getCurrentUserId() } returns null
        val worker = createWorker()

        // When
        val result = worker.doWork()

        // Then
        assertTrue(result is ListenableWorker.Result.Success)
        val outputData = (result as ListenableWorker.Result.Success).outputData
        assertEquals("skipped", outputData.getString("status"))
        assertEquals("no_user", outputData.getString("reason"))

        // Verify sync status manager was not called
        coVerify(exactly = 0) { syncStatusManager.markSyncStarted() }
    }

    @Test
    fun `doWork should block sync when shouldAttemptSync returns false`() = runTest {
        // Given
        coEvery { syncStatusManager.shouldAttemptSync() } returns false
        val worker = createWorker()

        // When
        val result = worker.doWork()

        // Then
        assertTrue(result is ListenableWorker.Result.Success)
        val outputData = (result as ListenableWorker.Result.Success).outputData
        assertEquals("blocked", outputData.getString("status"))
        assertEquals("recent_errors", outputData.getString("reason"))

        // Verify sync was not started
        coVerify(exactly = 0) { syncStatusManager.markSyncStarted() }
    }

    @Test
    fun `doWork should perform full sync successfully`() = runTest {
        // Given
        val inputData = workDataOf(
            ContentSyncWorker.KEY_USER_ID to "test_user_123",
            ContentSyncWorker.KEY_SYNC_TYPE to ContentSyncWorker.SYNC_TYPE_FULL
        )
        coEvery { discoverRepository.syncContentFromRemote() } returns Result.Success(Unit)
        coEvery { discoverRepository.cleanupOldContent(any()) } returns Result.Success(5)
        val worker = createWorker(inputData)

        // When
        val result = worker.doWork()

        // Then
        assertTrue(result is ListenableWorker.Result.Success)
        val outputData = (result as ListenableWorker.Result.Success).outputData
        assertEquals("success", outputData.getString("status"))
        assertEquals(ContentSyncWorker.SYNC_TYPE_FULL, outputData.getString("sync_type"))
        assertEquals("test_user_123", outputData.getString("user_id"))

        // Verify sync lifecycle
        coVerifyOrder {
            syncStatusManager.shouldAttemptSync()
            syncStatusManager.markSyncStarted()
            discoverRepository.syncContentFromRemote()
            syncStatusManager.markSyncCompleted()
            syncStatusManager.updateContentTypeSyncTime("articles")
            syncStatusManager.updateContentTypeSyncTime("news")
            syncStatusManager.updateContentTypeSyncTime("videos")
            syncStatusManager.updateContentTypeSyncTime("bookmarks")
            discoverRepository.cleanupOldContent(30)
        }
    }

    @Test
    fun `doWork should perform incremental sync successfully`() = runTest {
        // Given
        val inputData = workDataOf(
            ContentSyncWorker.KEY_USER_ID to "test_user_123",
            ContentSyncWorker.KEY_SYNC_TYPE to ContentSyncWorker.SYNC_TYPE_INCREMENTAL
        )
        coEvery { discoverRepository.getCacheStatistics() } returns Result.Success(
            mockk {
                every { lastSyncTime } returns System.currentTimeMillis() - 3600000 // 1 hour ago
            }
        )
        coEvery { discoverRepository.syncContentFromRemote() } returns Result.Success(Unit)
        coEvery { discoverRepository.cleanupOldContent(any()) } returns Result.Success(3)
        val worker = createWorker(inputData)

        // When
        val result = worker.doWork()

        // Then
        assertTrue(result is ListenableWorker.Result.Success)
        val outputData = (result as ListenableWorker.Result.Success).outputData
        assertEquals("success", outputData.getString("status"))
        assertEquals(ContentSyncWorker.SYNC_TYPE_INCREMENTAL, outputData.getString("sync_type"))

        // Verify incremental sync flow
        coVerifyOrder {
            syncStatusManager.markSyncStarted()
            discoverRepository.getCacheStatistics()
            discoverRepository.syncContentFromRemote()
            syncStatusManager.markSyncCompleted()
        }
    }

    @Test
    fun `doWork should perform bookmark sync successfully`() = runTest {
        // Given
        val inputData = workDataOf(
            ContentSyncWorker.KEY_USER_ID to "test_user_123",
            ContentSyncWorker.KEY_SYNC_TYPE to ContentSyncWorker.SYNC_TYPE_BOOKMARKS
        )
        coEvery { discoverRepository.cleanupOrphanedBookmarks() } returns Result.Success(2)
        coEvery { discoverRepository.cleanupOldContent(any()) } returns Result.Success(1)
        val worker = createWorker(inputData)

        // When
        val result = worker.doWork()

        // Then
        assertTrue(result is ListenableWorker.Result.Success)
        val outputData = (result as ListenableWorker.Result.Success).outputData
        assertEquals("success", outputData.getString("status"))
        assertEquals(ContentSyncWorker.SYNC_TYPE_BOOKMARKS, outputData.getString("sync_type"))

        // Verify bookmark sync flow
        coVerifyOrder {
            syncStatusManager.markSyncStarted()
            discoverRepository.cleanupOrphanedBookmarks()
            syncStatusManager.markSyncCompleted()
            syncStatusManager.updateContentTypeSyncTime("bookmarks")
        }
    }

    @Test
    fun `doWork should handle sync failure and retry`() = runTest {
        // Given
        val inputData = workDataOf(
            ContentSyncWorker.KEY_USER_ID to "test_user_123",
            ContentSyncWorker.KEY_SYNC_TYPE to ContentSyncWorker.SYNC_TYPE_FULL,
            ContentSyncWorker.KEY_RETRY_COUNT to 1
        )
        val errorMessage = "Network connection failed"
        coEvery { discoverRepository.syncContentFromRemote() } returns Result.Error(message = errorMessage)
        val worker = createWorker(inputData)

        // When
        val result = worker.doWork()

        // Then
        assertTrue(result is ListenableWorker.Result.Retry)

        // Verify error handling
        coVerifyOrder {
            syncStatusManager.markSyncStarted()
            discoverRepository.syncContentFromRemote()
            syncStatusManager.markSyncFailed(errorMessage)
        }
    }

    @Test
    fun `doWork should fail after max retry attempts`() = runTest {
        // Given
        val inputData = workDataOf(
            ContentSyncWorker.KEY_USER_ID to "test_user_123",
            ContentSyncWorker.KEY_SYNC_TYPE to ContentSyncWorker.SYNC_TYPE_FULL,
            ContentSyncWorker.KEY_RETRY_COUNT to 3 // Max attempts reached
        )
        val errorMessage = "Persistent network error"
        coEvery { discoverRepository.syncContentFromRemote() } returns Result.Error(message = errorMessage)
        val worker = createWorker(inputData)

        // When
        val result = worker.doWork()

        // Then
        assertTrue(result is ListenableWorker.Result.Failure)
        val outputData = (result as ListenableWorker.Result.Failure).outputData
        assertEquals("failed", outputData.getString("status"))
        assertEquals(errorMessage, outputData.getString("error"))
        assertEquals(3, outputData.getInt("retry_count", -1))

        // Verify failure handling
        coVerify { syncStatusManager.markSyncFailed(errorMessage) }
    }

    @Test
    fun `doWork should handle unexpected exceptions`() = runTest {
        // Given
        val inputData = workDataOf(
            ContentSyncWorker.KEY_USER_ID to "test_user_123",
            ContentSyncWorker.KEY_SYNC_TYPE to ContentSyncWorker.SYNC_TYPE_FULL
        )
        val exception = RuntimeException("Unexpected error")
        coEvery { discoverRepository.syncContentFromRemote() } throws exception
        val worker = createWorker(inputData)

        // When
        val result = worker.doWork()

        // Then
        assertTrue(result is ListenableWorker.Result.Failure)
        val outputData = (result as ListenableWorker.Result.Failure).outputData
        assertEquals("error", outputData.getString("status"))
        assertEquals("Unexpected error", outputData.getString("error"))

        // Verify exception handling
        coVerify { syncStatusManager.markSyncFailed("Unexpected error") }
    }

    @Test
    fun `doWork should default to incremental sync for unknown sync type`() = runTest {
        // Given
        val inputData = workDataOf(
            ContentSyncWorker.KEY_USER_ID to "test_user_123",
            ContentSyncWorker.KEY_SYNC_TYPE to "unknown_type"
        )
        coEvery { discoverRepository.getCacheStatistics() } returns Result.Success(
            mockk { every { lastSyncTime } returns System.currentTimeMillis() }
        )
        coEvery { discoverRepository.syncContentFromRemote() } returns Result.Success(Unit)
        coEvery { discoverRepository.cleanupOldContent(any()) } returns Result.Success(0)
        val worker = createWorker(inputData)

        // When
        val result = worker.doWork()

        // Then
        assertTrue(result is ListenableWorker.Result.Success)

        // Verify incremental sync was performed (cache statistics checked)
        coVerify { discoverRepository.getCacheStatistics() }
        coVerify { discoverRepository.syncContentFromRemote() }
    }

    @Test
    fun `incremental sync should fallback to full sync when cache statistics unavailable`() = runTest {
        // Given
        val inputData = workDataOf(
            ContentSyncWorker.KEY_USER_ID to "test_user_123",
            ContentSyncWorker.KEY_SYNC_TYPE to ContentSyncWorker.SYNC_TYPE_INCREMENTAL
        )
        coEvery { discoverRepository.getCacheStatistics() } returns Result.Error(message = "Cache stats unavailable")
        coEvery { discoverRepository.syncContentFromRemote() } returns Result.Success(Unit)
        coEvery { discoverRepository.cleanupOldContent(any()) } returns Result.Success(0)
        val worker = createWorker(inputData)

        // When
        val result = worker.doWork()

        // Then
        assertTrue(result is ListenableWorker.Result.Success)

        // Verify fallback to full sync
        coVerify { discoverRepository.getCacheStatistics() }
        coVerify { discoverRepository.syncContentFromRemote() }
    }

    @Test
    fun `updateContentTypeSyncTimes should handle exceptions gracefully`() = runTest {
        // Given
        val inputData = workDataOf(
            ContentSyncWorker.KEY_USER_ID to "test_user_123",
            ContentSyncWorker.KEY_SYNC_TYPE to ContentSyncWorker.SYNC_TYPE_FULL
        )
        coEvery { discoverRepository.syncContentFromRemote() } returns Result.Success(Unit)
        coEvery { syncStatusManager.updateContentTypeSyncTime(any()) } throws RuntimeException("Update failed")
        coEvery { discoverRepository.cleanupOldContent(any()) } returns Result.Success(0)
        val worker = createWorker(inputData)

        // When
        val result = worker.doWork()

        // Then - Should still succeed despite update failure
        assertTrue(result is ListenableWorker.Result.Success)

        // Verify sync completed despite update failure
        coVerify { syncStatusManager.markSyncCompleted() }
    }
}