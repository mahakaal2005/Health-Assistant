package com.example.health_assistant.features.discover.presentation

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.health_assistant.features.discover.domain.model.DiscoverContent
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class DeepLinkManagerTest {

    private lateinit var deepLinkManager: DeepLinkManager
    private lateinit var context: Context
    private lateinit var testArticle: DiscoverContent.Article
    private lateinit var testNews: DiscoverContent.News
    private lateinit var testVideo: DiscoverContent.Video

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        deepLinkManager = DeepLinkManager(context)
        
        // Create test content
        testArticle = DiscoverContent.Article(
            id = "article-123",
            title = "Test Article",
            publishedDate = System.currentTimeMillis(),
            category = "health",
            imageUrl = null,
            userId = "user-1",
            summary = "Test summary",
            content = "Test content",
            authorName = "Test Author",
            authorCredentials = "MD",
            sourceUrl = "https://example.com",
            lastUpdated = System.currentTimeMillis(),
            readingTimeMinutes = 5,
            tags = listOf("test"),
            isBookmarked = false,
            readProgress = 0f,
            credibilityScore = 4
        )

        testNews = DiscoverContent.News(
            id = "news-456",
            title = "Test News",
            publishedDate = System.currentTimeMillis(),
            category = "health",
            imageUrl = null,
            userId = "user-1",
            summary = "Test news summary",
            fullContent = "Test news content",
            sourcePublication = "Test Publication",
            sourceCredibility = "verified",
            externalUrl = "https://example.com/news",
            isBreakingNews = false,
            relevanceScore = 7
        )

        testVideo = DiscoverContent.Video(
            id = "video-789",
            title = "Test Video",
            publishedDate = System.currentTimeMillis(),
            category = "fitness",
            imageUrl = null,
            userId = "user-1",
            description = "Test video description",
            thumbnailUrl = "https://example.com/thumb.jpg",
            videoUrl = "https://example.com/video.mp4",
            durationSeconds = 300,
            difficultyLevel = "beginner",
            expertName = "Test Expert",
            expertCredentials = "Certified Trainer",
            watchProgress = 0f,
            isDownloadedOffline = false,
            transcriptAvailable = false
        )
    }

    @Test
    fun `createContentDeepLink should generate correct deep link for article`() {
        val deepLink = deepLinkManager.createContentDeepLink(testArticle)
        
        assertEquals("healthassistant://discover/article/article-123", deepLink)
    }

    @Test
    fun `createContentDeepLink should generate correct deep link for news`() {
        val deepLink = deepLinkManager.createContentDeepLink(testNews)
        
        assertEquals("healthassistant://discover/news/news-456", deepLink)
    }

    @Test
    fun `createContentDeepLink should generate correct deep link for video`() {
        val deepLink = deepLinkManager.createContentDeepLink(testVideo)
        
        assertEquals("healthassistant://discover/video/video-789", deepLink)
    }

    @Test
    fun `createWebFallbackLink should generate correct web link for article`() {
        val webLink = deepLinkManager.createWebFallbackLink(testArticle)
        
        assertEquals("https://healthassistant.app/article/article-123", webLink)
    }

    @Test
    fun `createWebFallbackLink should generate correct web link for news`() {
        val webLink = deepLinkManager.createWebFallbackLink(testNews)
        
        assertEquals("https://healthassistant.app/news/news-456", webLink)
    }

    @Test
    fun `createWebFallbackLink should generate correct web link for video`() {
        val webLink = deepLinkManager.createWebFallbackLink(testVideo)
        
        assertEquals("https://healthassistant.app/video/video-789", webLink)
    }

    @Test
    fun `createUniversalLink should combine web link with deep link parameter`() {
        val universalLink = deepLinkManager.createUniversalLink(testArticle)
        
        assertTrue(universalLink.startsWith("https://healthassistant.app/article/article-123"))
        assertTrue(universalLink.contains("deeplink="))
        assertTrue(universalLink.contains("healthassistant%3A%2F%2Fdiscover%2Farticle%2Farticle-123"))
    }

    @Test
    fun `createAppStoreLink should include package name`() {
        val appStoreLink = deepLinkManager.createAppStoreLink()
        
        assertTrue(appStoreLink.startsWith("https://play.google.com/store/apps/details?id="))
        assertTrue(appStoreLink.contains(context.packageName))
    }

    @Test
    fun `parseDeepLink should correctly parse valid article deep link`() {
        val uri = Uri.parse("healthassistant://discover/article/article-123")
        val result = deepLinkManager.parseDeepLink(uri)
        
        assertNotNull(result)
        assertEquals("article", result.contentType)
        assertEquals("article-123", result.contentId)
        assertTrue(result.parameters.isEmpty())
    }

    @Test
    fun `parseDeepLink should correctly parse valid news deep link`() {
        val uri = Uri.parse("healthassistant://discover/news/news-456")
        val result = deepLinkManager.parseDeepLink(uri)
        
        assertNotNull(result)
        assertEquals("news", result.contentType)
        assertEquals("news-456", result.contentId)
    }

    @Test
    fun `parseDeepLink should correctly parse valid video deep link`() {
        val uri = Uri.parse("healthassistant://discover/video/video-789")
        val result = deepLinkManager.parseDeepLink(uri)
        
        assertNotNull(result)
        assertEquals("video", result.contentType)
        assertEquals("video-789", result.contentId)
    }

    @Test
    fun `parseDeepLink should handle query parameters`() {
        val uri = Uri.parse("healthassistant://discover/article/article-123?source=share&utm_campaign=social")
        val result = deepLinkManager.parseDeepLink(uri)
        
        assertNotNull(result)
        assertEquals("article", result.contentType)
        assertEquals("article-123", result.contentId)
        assertEquals("share", result.parameters["source"])
        assertEquals("social", result.parameters["utm_campaign"])
    }

    @Test
    fun `parseDeepLink should return null for invalid scheme`() {
        val uri = Uri.parse("https://example.com/discover/article/123")
        val result = deepLinkManager.parseDeepLink(uri)
        
        assertNull(result)
    }

    @Test
    fun `parseDeepLink should return null for invalid path`() {
        val uri = Uri.parse("healthassistant://invalid/article/123")
        val result = deepLinkManager.parseDeepLink(uri)
        
        assertNull(result)
    }

    @Test
    fun `parseDeepLink should return null for insufficient path segments`() {
        val uri = Uri.parse("healthassistant://discover/article")
        val result = deepLinkManager.parseDeepLink(uri)
        
        assertNull(result)
    }

    @Test
    fun `createContentIntent should create proper intent for article`() {
        val intent = deepLinkManager.createContentIntent(testArticle)
        
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals("healthassistant://discover/article/article-123", intent.data.toString())
        assertEquals(context.packageName, intent.`package`)
    }

    @Test
    fun `canHandleDeepLink should return true for valid deep links`() {
        val validUri1 = Uri.parse("healthassistant://discover/article/123")
        val validUri2 = Uri.parse("healthassistant://discover/news/456")
        val validUri3 = Uri.parse("healthassistant://discover/video/789")
        
        assertTrue(deepLinkManager.canHandleDeepLink(validUri1))
        assertTrue(deepLinkManager.canHandleDeepLink(validUri2))
        assertTrue(deepLinkManager.canHandleDeepLink(validUri3))
    }

    @Test
    fun `canHandleDeepLink should return false for invalid deep links`() {
        val invalidUri1 = Uri.parse("https://example.com/article/123")
        val invalidUri2 = Uri.parse("healthassistant://invalid/article/123")
        val invalidUri3 = Uri.parse("healthassistant://discover/article")
        
        assertFalse(deepLinkManager.canHandleDeepLink(invalidUri1))
        assertFalse(deepLinkManager.canHandleDeepLink(invalidUri2))
        assertFalse(deepLinkManager.canHandleDeepLink(invalidUri3))
    }

    @Test
    fun `generateAttributionText should return proper attribution`() {
        val attribution = deepLinkManager.generateAttributionText()
        
        assertTrue(attribution.contains("Health Assistant"))
        assertTrue(attribution.contains("Shared via"))
    }

    @Test
    fun `generateAppPromotionText should include app store link`() {
        val promotion = deepLinkManager.generateAppPromotionText()
        
        assertTrue(promotion.contains("Download Health Assistant"))
        assertTrue(promotion.contains("https://play.google.com/store/apps/details?id="))
        assertTrue(promotion.contains(context.packageName))
    }
}