package com.example.health_assistant.features.discover.presentation

import com.example.health_assistant.features.discover.domain.model.DiscoverContent
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * Unit tests for DiscoverContentAdapter
 */
class DiscoverContentAdapterTest {

    @Test
    fun `DiffCallback areItemsTheSame returns true for same id and type`() {
        // Given
        val article1 = createTestArticle("1", "Title 1")
        val article2 = createTestArticle("1", "Title 2")
        val diffCallback = DiscoverContentDiffCallback()

        // When
        val result = diffCallback.areItemsTheSame(article1, article2)

        // Then
        assertTrue(result)
    }

    @Test
    fun `DiffCallback areItemsTheSame returns false for different id`() {
        // Given
        val article1 = createTestArticle("1", "Title 1")
        val article2 = createTestArticle("2", "Title 1")
        val diffCallback = DiscoverContentDiffCallback()

        // When
        val result = diffCallback.areItemsTheSame(article1, article2)

        // Then
        assertFalse(result)
    }

    @Test
    fun `DiffCallback areItemsTheSame returns false for different content types`() {
        // Given
        val article = createTestArticle("1", "Title")
        val news = createTestNews("1", "Title")
        val diffCallback = DiscoverContentDiffCallback()

        // When
        val result = diffCallback.areItemsTheSame(article, news)

        // Then
        assertFalse(result)
    }

    @Test
    fun `DiffCallback areContentsTheSame returns true for identical articles`() {
        // Given
        val article1 = createTestArticle("1", "Title")
        val article2 = createTestArticle("1", "Title")
        val diffCallback = DiscoverContentDiffCallback()

        // When
        val result = diffCallback.areContentsTheSame(article1, article2)

        // Then
        assertTrue(result)
    }

    @Test
    fun `DiffCallback areContentsTheSame returns false for different bookmark status`() {
        // Given
        val article1 = createTestArticle("1", "Title", isBookmarked = false)
        val article2 = createTestArticle("1", "Title", isBookmarked = true)
        val diffCallback = DiscoverContentDiffCallback()

        // When
        val result = diffCallback.areContentsTheSame(article1, article2)

        // Then
        assertFalse(result)
    }

    @Test
    fun `DiffCallback getChangePayload returns bookmark change for article bookmark difference`() {
        // Given
        val article1 = createTestArticle("1", "Title", isBookmarked = false)
        val article2 = createTestArticle("1", "Title", isBookmarked = true)
        val diffCallback = DiscoverContentDiffCallback()

        // When
        val payload = diffCallback.getChangePayload(article1, article2)

        // Then
        assertNotNull(payload)
        assertTrue((payload as List<*>).contains("bookmark"))
    }

    @Test
    fun `DiffCallback getChangePayload returns progress change for article progress difference`() {
        // Given
        val article1 = createTestArticle("1", "Title", readProgress = 0.0f)
        val article2 = createTestArticle("1", "Title", readProgress = 0.5f)
        val diffCallback = DiscoverContentDiffCallback()

        // When
        val payload = diffCallback.getChangePayload(article1, article2)

        // Then
        assertNotNull(payload)
        assertTrue((payload as List<*>).contains("progress"))
    }

    @Test
    fun `DiffCallback getChangePayload returns progress change for video progress difference`() {
        // Given
        val video1 = createTestVideo("1", "Title", watchProgress = 0.0f)
        val video2 = createTestVideo("1", "Title", watchProgress = 0.3f)
        val diffCallback = DiscoverContentDiffCallback()

        // When
        val payload = diffCallback.getChangePayload(video1, video2)

        // Then
        assertNotNull(payload)
        assertTrue((payload as List<*>).contains("progress"))
    }

    @Test
    fun `DiffCallback getChangePayload returns offline change for video offline difference`() {
        // Given
        val video1 = createTestVideo("1", "Title", isDownloadedOffline = false)
        val video2 = createTestVideo("1", "Title", isDownloadedOffline = true)
        val diffCallback = DiscoverContentDiffCallback()

        // When
        val payload = diffCallback.getChangePayload(video1, video2)

        // Then
        assertNotNull(payload)
        assertTrue((payload as List<*>).contains("offline"))
    }

    @Test
    fun `DiffCallback getChangePayload returns null for identical content`() {
        // Given
        val article1 = createTestArticle("1", "Title")
        val article2 = createTestArticle("1", "Title")
        val diffCallback = DiscoverContentDiffCallback()

        // When
        val payload = diffCallback.getChangePayload(article1, article2)

        // Then
        assertNull(payload)
    }

    // Helper methods to create test data
    private fun createTestArticle(
        id: String,
        title: String,
        isBookmarked: Boolean = false,
        readProgress: Float = 0.0f
    ): DiscoverContent.Article {
        return DiscoverContent.Article(
            id = id,
            title = title,
            publishedDate = System.currentTimeMillis(),
            category = "health",
            imageUrl = null,
            userId = "test_user",
            summary = "Test summary",
            content = "Test content",
            authorName = "Test Author",
            authorCredentials = "MD",
            sourceUrl = "https://test.com",
            lastUpdated = System.currentTimeMillis(),
            readingTimeMinutes = 5,
            tags = listOf("health", "test"),
            isBookmarked = isBookmarked,
            readProgress = readProgress,
            credibilityScore = 4
        )
    }

    private fun createTestNews(
        id: String,
        title: String,
        isBreakingNews: Boolean = false
    ): DiscoverContent.News {
        return DiscoverContent.News(
            id = id,
            title = title,
            publishedDate = System.currentTimeMillis(),
            category = "health",
            imageUrl = null,
            userId = "test_user",
            summary = "Test summary",
            fullContent = "Test content",
            sourcePublication = "Test Publication",
            sourceCredibility = "peer-reviewed",
            externalUrl = "https://test.com",
            isBreakingNews = isBreakingNews,
            relevanceScore = 5
        )
    }

    private fun createTestVideo(
        id: String,
        title: String,
        watchProgress: Float = 0.0f,
        isDownloadedOffline: Boolean = false
    ): DiscoverContent.Video {
        return DiscoverContent.Video(
            id = id,
            title = title,
            publishedDate = System.currentTimeMillis(),
            category = "fitness",
            imageUrl = null,
            userId = "test_user",
            description = "Test description",
            thumbnailUrl = "https://test.com/thumb.jpg",
            videoUrl = "https://test.com/video.mp4",
            durationSeconds = 300,
            difficultyLevel = "beginner",
            expertName = "Test Expert",
            expertCredentials = "Certified Trainer",
            watchProgress = watchProgress,
            isDownloadedOffline = isDownloadedOffline,
            transcriptAvailable = false
        )
    }
}