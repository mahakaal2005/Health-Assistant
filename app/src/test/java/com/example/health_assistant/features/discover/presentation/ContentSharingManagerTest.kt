package com.example.health_assistant.features.discover.presentation

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import com.example.health_assistant.features.discover.domain.model.DiscoverContent
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ContentSharingManagerTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockPackageManager: PackageManager

    @Mock
    private lateinit var mockClipboardManager: ClipboardManager

    private lateinit var contentSharingManager: ContentSharingManager
    private lateinit var testArticle: DiscoverContent.Article
    private lateinit var testNews: DiscoverContent.News
    private lateinit var testVideo: DiscoverContent.Video

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        
        // Use real context for testing
        val context = ApplicationProvider.getApplicationContext<Context>()
        contentSharingManager = ContentSharingManager(context)
        
        // Create test content
        testArticle = DiscoverContent.Article(
            id = "article-1",
            title = "Understanding Heart Health",
            publishedDate = System.currentTimeMillis(),
            category = "cardiovascular",
            imageUrl = "https://example.com/image.jpg",
            userId = "user-1",
            summary = "A comprehensive guide to maintaining heart health through diet and exercise.",
            content = "Full article content here...",
            authorName = "Dr. Jane Smith",
            authorCredentials = "MD, Cardiologist",
            sourceUrl = "https://example.com/source",
            lastUpdated = System.currentTimeMillis(),
            readingTimeMinutes = 5,
            tags = listOf("heart", "cardiovascular", "prevention"),
            isBookmarked = false,
            readProgress = 0.3f,
            credibilityScore = 5
        )

        testNews = DiscoverContent.News(
            id = "news-1",
            title = "New Study Shows Benefits of Mediterranean Diet",
            publishedDate = System.currentTimeMillis(),
            category = "nutrition",
            imageUrl = "https://example.com/news-image.jpg",
            userId = "user-1",
            summary = "Recent research highlights the cardiovascular benefits of Mediterranean diet.",
            fullContent = "Full news content...",
            sourcePublication = "Health Journal",
            sourceCredibility = "peer-reviewed",
            externalUrl = "https://example.com/news",
            isBreakingNews = true,
            relevanceScore = 8
        )

        testVideo = DiscoverContent.Video(
            id = "video-1",
            title = "10-Minute Morning Yoga Routine",
            publishedDate = System.currentTimeMillis(),
            category = "fitness",
            imageUrl = "https://example.com/video-thumb.jpg",
            userId = "user-1",
            description = "Start your day with this energizing yoga routine.",
            thumbnailUrl = "https://example.com/thumbnail.jpg",
            videoUrl = "https://example.com/video.mp4",
            durationSeconds = 600,
            difficultyLevel = "beginner",
            expertName = "Sarah Johnson",
            expertCredentials = "Certified Yoga Instructor",
            watchProgress = 0.5f,
            isDownloadedOffline = false,
            transcriptAvailable = true
        )
    }

    @Test
    fun `createShareableLink should generate valid deep link for article`() {
        val link = contentSharingManager.createShareableLink(testArticle)
        
        assertNotNull(link)
        assertTrue(link.contains("article"))
        assertTrue(link.contains(testArticle.id))
    }

    @Test
    fun `createShareableLink should generate valid deep link for news`() {
        val link = contentSharingManager.createShareableLink(testNews)
        
        assertNotNull(link)
        assertTrue(link.contains("news"))
        assertTrue(link.contains(testNews.id))
    }

    @Test
    fun `createShareableLink should generate valid deep link for video`() {
        val link = contentSharingManager.createShareableLink(testVideo)
        
        assertNotNull(link)
        assertTrue(link.contains("video"))
        assertTrue(link.contains(testVideo.id))
    }

    @Test
    fun `formatContentForSharing should include article details`() {
        // Use reflection to access private method for testing
        val method = ContentSharingManager::class.java.getDeclaredMethod(
            "formatContentForSharing", 
            DiscoverContent::class.java
        )
        method.isAccessible = true
        val result = method.invoke(contentSharingManager, testArticle) as String
        
        assertTrue(result.contains(testArticle.title))
        assertTrue(result.contains(testArticle.authorName))
        assertTrue(result.contains(testArticle.summary))
        assertTrue(result.contains("Health Assistant"))
    }

    @Test
    fun `formatContentForSharing should include news details`() {
        val method = ContentSharingManager::class.java.getDeclaredMethod(
            "formatContentForSharing", 
            DiscoverContent::class.java
        )
        method.isAccessible = true
        val result = method.invoke(contentSharingManager, testNews) as String
        
        assertTrue(result.contains(testNews.title))
        assertTrue(result.contains(testNews.sourcePublication))
        assertTrue(result.contains(testNews.summary))
        assertTrue(result.contains("BREAKING NEWS"))
    }

    @Test
    fun `formatContentForSharing should include video details`() {
        val method = ContentSharingManager::class.java.getDeclaredMethod(
            "formatContentForSharing", 
            DiscoverContent::class.java
        )
        method.isAccessible = true
        val result = method.invoke(contentSharingManager, testVideo) as String
        
        assertTrue(result.contains(testVideo.title))
        assertTrue(result.contains(testVideo.expertName))
        assertTrue(result.contains(testVideo.description))
        assertTrue(result.contains("10:00")) // Duration formatting
    }

    @Test
    fun `formatContentForSocialMedia should respect character limits`() {
        val method = ContentSharingManager::class.java.getDeclaredMethod(
            "formatContentForSocialMedia", 
            DiscoverContent::class.java,
            ContentSharingManager.SocialPlatform::class.java
        )
        method.isAccessible = true
        
        // Test Twitter character limit
        val twitterResult = method.invoke(
            contentSharingManager, 
            testArticle, 
            ContentSharingManager.SocialPlatform.TWITTER
        ) as String
        
        assertTrue(twitterResult.length <= 280)
        assertTrue(twitterResult.contains(testArticle.title))
    }

    @Test
    fun `formatContentForEmail should include proper email structure`() {
        val method = ContentSharingManager::class.java.getDeclaredMethod(
            "formatContentForEmail", 
            DiscoverContent::class.java
        )
        method.isAccessible = true
        val result = method.invoke(contentSharingManager, testArticle) as String
        
        assertTrue(result.contains("Hello,"))
        assertTrue(result.contains("HEALTH ARTICLE"))
        assertTrue(result.contains("Title: ${testArticle.title}"))
        assertTrue(result.contains("Author: ${testArticle.authorName}"))
        assertTrue(result.contains("Best regards,"))
    }

    @Test
    fun `formatContentAsQuote should create proper quote format`() {
        val method = ContentSharingManager::class.java.getDeclaredMethod(
            "formatContentAsQuote", 
            DiscoverContent::class.java
        )
        method.isAccessible = true
        val result = method.invoke(contentSharingManager, testArticle) as String
        
        assertTrue(result.startsWith("\""))
        assertTrue(result.contains(testArticle.summary))
        assertTrue(result.contains("— ${testArticle.authorName}"))
    }

    @Test
    fun `formatContentCitation should create academic citation format`() {
        val method = ContentSharingManager::class.java.getDeclaredMethod(
            "formatContentCitation", 
            DiscoverContent::class.java
        )
        method.isAccessible = true
        val result = method.invoke(contentSharingManager, testArticle) as String
        
        assertTrue(result.contains(testArticle.authorName))
        assertTrue(result.contains(testArticle.title))
        assertTrue(result.contains("Health Assistant"))
    }

    @Test
    fun `getShareSubject should return appropriate subject for each content type`() {
        val method = ContentSharingManager::class.java.getDeclaredMethod(
            "getShareSubject", 
            DiscoverContent::class.java
        )
        method.isAccessible = true
        
        val articleSubject = method.invoke(contentSharingManager, testArticle) as String
        val newsSubject = method.invoke(contentSharingManager, testNews) as String
        val videoSubject = method.invoke(contentSharingManager, testVideo) as String
        
        assertTrue(articleSubject.contains("Health Article"))
        assertTrue(newsSubject.contains("Health News"))
        assertTrue(videoSubject.contains("Health Video"))
    }

    @Test
    fun `formatDuration should correctly format video duration`() {
        val method = ContentSharingManager::class.java.getDeclaredMethod(
            "formatDuration", 
            Int::class.java
        )
        method.isAccessible = true
        
        val result600 = method.invoke(contentSharingManager, 600) as String // 10 minutes
        val result90 = method.invoke(contentSharingManager, 90) as String   // 1:30
        val result30 = method.invoke(contentSharingManager, 30) as String   // 30 seconds
        
        assertEquals("10m", result600)
        assertEquals("1m 30s", result90)
        assertEquals("30s", result30)
    }

    @Test
    fun `formatCategory should properly format category names`() {
        val method = ContentSharingManager::class.java.getDeclaredMethod(
            "formatCategory", 
            String::class.java
        )
        method.isAccessible = true
        
        val result1 = method.invoke(contentSharingManager, "mental-health") as String
        val result2 = method.invoke(contentSharingManager, "preventive_care") as String
        val result3 = method.invoke(contentSharingManager, "nutrition") as String
        
        assertEquals("Mental Health", result1)
        assertEquals("Preventive Care", result2)
        assertEquals("Nutrition", result3)
    }

    @Test
    fun `formatCredibility should properly format credibility types`() {
        val method = ContentSharingManager::class.java.getDeclaredMethod(
            "formatCredibility", 
            String::class.java
        )
        method.isAccessible = true
        
        val result1 = method.invoke(contentSharingManager, "peer-reviewed") as String
        val result2 = method.invoke(contentSharingManager, "medical-journal") as String
        val result3 = method.invoke(contentSharingManager, "health-organization") as String
        
        assertEquals("Peer-Reviewed Study", result1)
        assertEquals("Medical Journal", result2)
        assertEquals("Health Organization", result3)
    }
}