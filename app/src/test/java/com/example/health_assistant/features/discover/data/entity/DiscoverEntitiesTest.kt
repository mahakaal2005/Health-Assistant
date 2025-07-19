package com.example.health_assistant.features.discover.data.entity

import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for Discover feature entities
 * Verifies entity creation and data integrity
 */
class DiscoverEntitiesTest {

    @Test
    fun `HealthArticleEntity creation with all fields`() {
        val article = HealthArticleEntity(
            id = "article_1",
            title = "Test Health Article",
            summary = "This is a test summary",
            content = "This is the full content of the article",
            category = "nutrition",
            authorName = "Dr. Test Author",
            authorCredentials = "MD, PhD",
            sourceUrl = "https://example.com/article",
            publishedDate = System.currentTimeMillis(),
            lastUpdated = System.currentTimeMillis(),
            readingTimeMinutes = 5,
            imageUrl = "https://example.com/image.jpg",
            tags = listOf("nutrition", "diet", "health"),
            isBookmarked = false,
            readProgress = 0.0f,
            credibilityScore = 4,
            userId = "user_123"
        )

        assertEquals("article_1", article.id)
        assertEquals("Test Health Article", article.title)
        assertEquals("nutrition", article.category)
        assertEquals(3, article.tags.size)
        assertEquals(4, article.credibilityScore)
        assertFalse(article.isBookmarked)
        assertEquals(0.0f, article.readProgress, 0.01f)
    }

    @Test
    fun `HealthNewsEntity creation with all fields`() {
        val news = HealthNewsEntity(
            id = "news_1",
            headline = "Breaking Health News",
            summary = "Important health update",
            fullContent = "Full news content here",
            category = "medical-news",
            sourcePublication = "Health Journal",
            sourceCredibility = "peer-reviewed",
            publishedDate = System.currentTimeMillis(),
            imageUrl = "https://example.com/news-image.jpg",
            externalUrl = "https://example.com/news",
            isBreakingNews = true,
            relevanceScore = 5,
            userId = "user_123"
        )

        assertEquals("news_1", news.id)
        assertEquals("Breaking Health News", news.headline)
        assertEquals("peer-reviewed", news.sourceCredibility)
        assertTrue(news.isBreakingNews)
        assertEquals(5, news.relevanceScore)
    }

    @Test
    fun `EducationalVideoEntity creation with all fields`() {
        val video = EducationalVideoEntity(
            id = "video_1",
            title = "Educational Health Video",
            description = "Learn about health topics",
            category = "fitness",
            thumbnailUrl = "https://example.com/thumbnail.jpg",
            videoUrl = "https://example.com/video.mp4",
            durationSeconds = 300,
            difficultyLevel = "beginner",
            expertName = "Dr. Video Expert",
            expertCredentials = "MD, Fitness Specialist",
            publishedDate = System.currentTimeMillis(),
            watchProgress = 0.5f,
            isDownloadedOffline = false,
            transcriptAvailable = true,
            userId = "user_123"
        )

        assertEquals("video_1", video.id)
        assertEquals("Educational Health Video", video.title)
        assertEquals("beginner", video.difficultyLevel)
        assertEquals(300, video.durationSeconds)
        assertEquals(0.5f, video.watchProgress, 0.01f)
        assertFalse(video.isDownloadedOffline)
        assertTrue(video.transcriptAvailable)
    }

    @Test
    fun `ContentBookmarkEntity creation with all fields`() {
        val bookmark = ContentBookmarkEntity(
            id = "bookmark_1",
            contentId = "article_1",
            contentType = "article",
            bookmarkedDate = System.currentTimeMillis(),
            userId = "user_123"
        )

        assertEquals("bookmark_1", bookmark.id)
        assertEquals("article_1", bookmark.contentId)
        assertEquals("article", bookmark.contentType)
        assertEquals("user_123", bookmark.userId)
    }

    @Test
    fun `HealthArticleEntity default values`() {
        val article = HealthArticleEntity(
            id = "article_2",
            title = "Test Article",
            summary = "Summary",
            content = "Content",
            category = "fitness",
            authorName = "Author",
            authorCredentials = "Credentials",
            sourceUrl = "https://example.com",
            publishedDate = 0L,
            lastUpdated = 0L,
            readingTimeMinutes = 1,
            imageUrl = null,
            tags = emptyList(),
            credibilityScore = 3
        )

        // Test default values
        assertFalse(article.isBookmarked)
        assertEquals(0.0f, article.readProgress, 0.01f)
        assertEquals("", article.userId)
        assertNull(article.imageUrl)
        assertTrue(article.tags.isEmpty())
    }

    @Test
    fun `EducationalVideoEntity default values`() {
        val video = EducationalVideoEntity(
            id = "video_2",
            title = "Test Video",
            description = "Description",
            category = "mental-health",
            thumbnailUrl = "https://example.com/thumb.jpg",
            videoUrl = "https://example.com/video.mp4",
            durationSeconds = 120,
            difficultyLevel = "intermediate",
            expertName = "Expert",
            expertCredentials = "Credentials",
            publishedDate = 0L
        )

        // Test default values
        assertEquals(0.0f, video.watchProgress, 0.01f)
        assertFalse(video.isDownloadedOffline)
        assertFalse(video.transcriptAvailable)
        assertEquals("", video.userId)
    }
}