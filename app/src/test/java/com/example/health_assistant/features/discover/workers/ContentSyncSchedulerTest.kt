package com.example.health_assistant.features.discover.workers

import android.content.Context
import com.example.health_assistant.auth.session.SessionManager
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

/**
 * Simplified unit tests for ContentSyncScheduler
 * Tests basic functionality without complex WorkManager mocking
 */
class ContentSyncSchedulerTest {

    private lateinit var context: Context
    private lateinit var sessionManager: SessionManager
    private lateinit var contentSyncScheduler: ContentSyncScheduler

    @BeforeEach
    fun setup() {
        context = mockk(relaxed = true)
        sessionManager = mockk(relaxed = true)

        // Default mock behaviors
        every { sessionManager.getCurrentUserId() } returns "test_user_123"

        contentSyncScheduler = ContentSyncScheduler(context, sessionManager)
    }

    @Test
    fun `ContentSyncScheduler should be created successfully`() {
        // Then
        assertNotNull(contentSyncScheduler)
    }

    @Test
    fun `isSyncRunning should return false by default`() = runTest {
        // When
        val isRunning = contentSyncScheduler.isSyncRunning()

        // Then
        assertFalse(isRunning)
    }

    @Test
    fun `getCurrentUserId should return user ID from session manager`() {
        // Given
        every { sessionManager.getCurrentUserId() } returns "test_user_123"

        // When - Test through a public method that uses getCurrentUserId
        contentSyncScheduler.schedulePeriodicSync()

        // Then - Verify sessionManager was called
        verify { sessionManager.getCurrentUserId() }
    }

    @Test
    fun `schedulePeriodicSync should handle null user ID gracefully`() {
        // Given
        every { sessionManager.getCurrentUserId() } returns null

        // When - Should not throw exception
        contentSyncScheduler.schedulePeriodicSync()

        // Then - Should complete without error
        verify { sessionManager.getCurrentUserId() }
    }

    @Test
    fun `triggerManualSync should handle null user ID gracefully`() {
        // Given
        every { sessionManager.getCurrentUserId() } returns null

        // When - Should not throw exception
        contentSyncScheduler.triggerManualSync()

        // Then - Should complete without error
        verify { sessionManager.getCurrentUserId() }
    }
}