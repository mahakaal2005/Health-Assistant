package com.example.health_assistant.features.discover.domain.analytics

import com.example.health_assistant.features.discover.data.AnalyticsDao
import com.example.health_assistant.features.discover.data.entity.ABTestEntity
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*
import org.junit.Assert.*

class ABTestManagerTest {

    @Mock
    private lateinit var analyticsDao: AnalyticsDao

    private lateinit var abTestManager: ABTestManager

    private val testUserId = "test-user-1"

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        abTestManager = ABTestManager(analyticsDao)
    }

    @Test
    fun `getTestVariant should return existing variant for assigned user`() = runTest {
        // Given
        val existingTest = ABTestEntity(
            id = "test-1",
            userId = testUserId,
            testName = "content_layout_v1",
            variant = "card_layout",
            isActive = true,
            assignedAt = System.currentTimeMillis(),
            startDate = System.currentTimeMillis() - 86400000L, // 1 day ago
            endDate = System.currentTimeMillis() + 86400000L // 1 day from now
        )

        whenever(analyticsDao.getActiveABTest(testUserId, "content_layout_v1"))
            .thenReturn(existingTest)

        // When
        val variant = abTestManager.getTestVariant(testUserId, "content_layout_v1")

        // Then
        assertEquals("card_layout", variant)
        verify(analyticsDao).getActiveABTest(testUserId, "content_layout_v1")
        verify(analyticsDao, never()).insertABTest(any())
    }

    @Test
    fun `getTestVariant should assign new variant for unassigned user`() = runTest {
        // Given
        whenever(analyticsDao.getActiveABTest(testUserId, "content_layout_v1"))
            .thenReturn(null)

        // When
        val variant = abTestManager.getTestVariant(testUserId, "content_layout_v1")

        // Then
        assertNotNull("Should assign a variant", variant)
        assertTrue("Should assign valid variant", 
            listOf("card_layout", "list_layout", "magazine_layout").contains(variant))
        verify(analyticsDao).insertABTest(any())
    }

    @Test
    fun `getTestVariant should return null for inactive test`() = runTest {
        // Given - test that hasn't started yet
        val futureTime = System.currentTimeMillis() + 86400000L // 1 day from now

        // When
        val variant = abTestManager.getTestVariant(testUserId, "future_test")

        // Then
        assertNull("Should return null for inactive test", variant)
        verify(analyticsDao, never()).insertABTest(any())
    }

    @Test
    fun `recordImpression should update impression count`() = runTest {
        // Given
        val activeTest = ABTestEntity(
            id = "test-1",
            userId = testUserId,
            testName = "content_layout_v1",
            variant = "card_layout",
            isActive = true,
            assignedAt = System.currentTimeMillis(),
            startDate = System.currentTimeMillis() - 86400000L,
            endDate = System.currentTimeMillis() + 86400000L
        )

        whenever(analyticsDao.getActiveABTest(testUserId, "content_layout_v1"))
            .thenReturn(activeTest)

        // When
        abTestManager.recordImpression(testUserId, "content_layout_v1")

        // Then
        verify(analyticsDao).recordABTestImpression(eq("test-1"), any())
    }

    @Test
    fun `recordClick should update click count`() = runTest {
        // Given
        val activeTest = ABTestEntity(
            id = "test-1",
            userId = testUserId,
            testName = "content_layout_v1",
            variant = "card_layout",
            isActive = true,
            assignedAt = System.currentTimeMillis(),
            startDate = System.currentTimeMillis() - 86400000L,
            endDate = System.currentTimeMillis() + 86400000L
        )

        whenever(analyticsDao.getActiveABTest(testUserId, "content_layout_v1"))
            .thenReturn(activeTest)

        // When
        abTestManager.recordClick(testUserId, "content_layout_v1")

        // Then
        verify(analyticsDao).recordABTestClick(eq("test-1"), any())
    }

    @Test
    fun `recordConversion should update conversion count for valid events`() = runTest {
        // Given
        val activeTest = ABTestEntity(
            id = "test-1",
            userId = testUserId,
            testName = "content_layout_v1",
            variant = "card_layout",
            isActive = true,
            assignedAt = System.currentTimeMillis(),
            startDate = System.currentTimeMillis() - 86400000L,
            endDate = System.currentTimeMillis() + 86400000L
        )

        whenever(analyticsDao.getActiveABTest(testUserId, "content_layout_v1"))
            .thenReturn(activeTest)

        // When
        abTestManager.recordConversion(testUserId, "content_layout_v1", "content_click")

        // Then
        verify(analyticsDao).recordABTestConversion(eq("test-1"), any())
    }

    @Test
    fun `recordConversion should not update for invalid events`() = runTest {
        // Given
        val activeTest = ABTestEntity(
            id = "test-1",
            userId = testUserId,
            testName = "content_layout_v1",
            variant = "card_layout",
            isActive = true,
            assignedAt = System.currentTimeMillis(),
            startDate = System.currentTimeMillis() - 86400000L,
            endDate = System.currentTimeMillis() + 86400000L
        )

        whenever(analyticsDao.getActiveABTest(testUserId, "content_layout_v1"))
            .thenReturn(activeTest)

        // When
        abTestManager.recordConversion(testUserId, "content_layout_v1", "invalid_event")

        // Then
        verify(analyticsDao, never()).recordABTestConversion(any(), any())
    }

    @Test
    fun `recordEngagementTime should update engagement time`() = runTest {
        // Given
        val activeTest = ABTestEntity(
            id = "test-1",
            userId = testUserId,
            testName = "reading_progress_indicator",
            variant = "progress_bar",
            isActive = true,
            assignedAt = System.currentTimeMillis(),
            startDate = System.currentTimeMillis() - 86400000L,
            endDate = System.currentTimeMillis() + 86400000L
        )

        whenever(analyticsDao.getActiveABTest(testUserId, "reading_progress_indicator"))
            .thenReturn(activeTest)

        val duration = 30000L

        // When
        abTestManager.recordEngagementTime(testUserId, "reading_progress_indicator", duration)

        // Then
        verify(analyticsDao).updateABTestEngagementTime(eq("test-1"), eq(duration), any())
    }

    @Test
    fun `getActiveTests should return all active tests for user`() = runTest {
        // Given
        val activeTests = listOf(
            ABTestEntity(
                id = "test-1",
                userId = testUserId,
                testName = "content_layout_v1",
                variant = "card_layout",
                isActive = true,
                assignedAt = System.currentTimeMillis(),
                startDate = System.currentTimeMillis() - 86400000L,
                endDate = System.currentTimeMillis() + 86400000L
            ),
            ABTestEntity(
                id = "test-2",
                userId = testUserId,
                testName = "recommendation_algorithm_v2",
                variant = "hybrid",
                isActive = true,
                assignedAt = System.currentTimeMillis(),
                startDate = System.currentTimeMillis() - 86400000L,
                endDate = System.currentTimeMillis() + 86400000L
            )
        )

        whenever(analyticsDao.getActiveABTests(testUserId)).thenReturn(activeTests)

        // When
        val result = abTestManager.getActiveTests(testUserId)

        // Then
        assertEquals(activeTests, result)
        verify(analyticsDao).getActiveABTests(testUserId)
    }

    @Test
    fun `deactivateExpiredTests should call DAO deactivation`() = runTest {
        // When
        abTestManager.deactivateExpiredTests()

        // Then
        verify(analyticsDao).deactivateExpiredABTests(any())
    }

    @Test
    fun `shouldShowFeature should return true for matching variant`() = runTest {
        // Given
        val activeTest = ABTestEntity(
            id = "test-1",
            userId = testUserId,
            testName = "reading_progress_indicator",
            variant = "progress_bar",
            isActive = true,
            assignedAt = System.currentTimeMillis(),
            startDate = System.currentTimeMillis() - 86400000L,
            endDate = System.currentTimeMillis() + 86400000L
        )

        whenever(analyticsDao.getActiveABTest(testUserId, "reading_progress_indicator"))
            .thenReturn(activeTest)

        // When
        val shouldShow = abTestManager.shouldShowFeature(testUserId, "reading_progress_indicator", "progress_bar")

        // Then
        assertTrue("Should show feature for matching variant", shouldShow)
    }

    @Test
    fun `shouldShowFeature should return false for non-matching variant`() = runTest {
        // Given
        val activeTest = ABTestEntity(
            id = "test-1",
            userId = testUserId,
            testName = "reading_progress_indicator",
            variant = "progress_bar",
            isActive = true,
            assignedAt = System.currentTimeMillis(),
            startDate = System.currentTimeMillis() - 86400000L,
            endDate = System.currentTimeMillis() + 86400000L
        )

        whenever(analyticsDao.getActiveABTest(testUserId, "reading_progress_indicator"))
            .thenReturn(activeTest)

        // When
        val shouldShow = abTestManager.shouldShowFeature(testUserId, "reading_progress_indicator", "percentage_text")

        // Then
        assertFalse("Should not show feature for non-matching variant", shouldShow)
    }

    @Test
    fun `getContentLayoutVariant should return default for unassigned user`() = runTest {
        // Given
        whenever(analyticsDao.getActiveABTest(testUserId, "content_layout_v1"))
            .thenReturn(null)

        // When
        val variant = abTestManager.getContentLayoutVariant(testUserId)

        // Then
        assertEquals("card_layout", variant)
    }

    @Test
    fun `getRecommendationAlgorithmVariant should return default for unassigned user`() = runTest {
        // Given
        whenever(analyticsDao.getActiveABTest(testUserId, "recommendation_algorithm_v2"))
            .thenReturn(null)

        // When
        val variant = abTestManager.getRecommendationAlgorithmVariant(testUserId)

        // Then
        assertEquals("hybrid", variant)
    }

    @Test
    fun `getReadingProgressVariant should return default for unassigned user`() = runTest {
        // Given
        whenever(analyticsDao.getActiveABTest(testUserId, "reading_progress_indicator"))
            .thenReturn(null)

        // When
        val variant = abTestManager.getReadingProgressVariant(testUserId)

        // Then
        assertEquals("control", variant)
    }
}