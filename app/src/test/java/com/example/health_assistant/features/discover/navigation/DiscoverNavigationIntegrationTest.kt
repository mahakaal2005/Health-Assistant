package com.example.health_assistant.features.discover.navigation

import android.content.Intent
import android.net.Uri
import androidx.navigation.NavController
import com.example.health_assistant.features.discover.domain.model.DiscoverContent
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

/**
 * Integration test for Discover navigation components
 * Tests the interaction between NavigationHelper and DeepLinkHandler
 */
class DiscoverNavigationIntegrationTest {

    private lateinit var navigationHelper: DiscoverNavigationHelper
    private lateinit var deepLinkHandler: DiscoverDeepLinkHandler
    private lateinit var mockNavController: NavController

    @Before
    fun setup() {
        navigationHelper = DiscoverNavigationHelper()
        deepLinkHandler = DiscoverDeepLinkHandler(navigationHelper)
        mockNavController = mockk(relaxed = true)
    }

    @Test
    fun `deep link to article should trigger correct navigation`() {
        // Given
        val intent = Intent().apply {
            data = Uri.parse("healthassistant://discover/article/integration-test-article")
        }

        // When
        val handled = deepLinkHandler.handleDeepLink(intent, mockNavController)

        // Then
        assert(handled)
        verify {
            mockNavController.navigate(
                com.example.health_assistant.R.id.action_global_to_articleReaderFragment,
                any()
            )
        }
    }

    @Test
    fun `deep link to video should trigger correct navigation`() {
        // Given
        val intent = Intent().apply {
            data = Uri.parse("healthassistant://discover/video/integration-test-video")
        }

        // When
        val handled = deepLinkHandler.handleDeepLink(intent, mockNavController)

        // Then
        assert(handled)
        verify {
            mockNavController.navigate(
                com.example.health_assistant.R.id.action_global_to_videoPlayerFragment,
                any()
            )
        }
    }

    @Test
    fun `navigation helper should handle different content types correctly`() {
        // Given
        val article = DiscoverContent.Article(
            id = "test-article",
            title = "Test Article",
            summary = "Test Summary",
            content = "Test Content",
            author = "Test Author",
            publishedDate = "2024-01-01",
            imageUrl = "test-url",
            sourceUrl = "test-source",
            tags = listOf("health"),
            readingTimeMinutes = 5,
            credibilityScore = 0.9f,
            isBookmarked = false
        )

        val news = DiscoverContent.HealthNews(
            id = "test-news",
            title = "Test News",
            summary = "Test News Summary",
            content = "Test News Content",
            source = "Test Source",
            publishedDate = "2024-01-01",
            imageUrl = "test-news-url",
            sourceUrl = "test-news-source",
            tags = listOf("news"),
            credibilityScore = 0.8f,
            isBookmarked = false
        )

        val video = DiscoverContent.EducationalVideo(
            id = "test-video",
            title = "Test Video",
            description = "Test Video Description",
            thumbnailUrl = "test-thumbnail",
            videoUrl = "test-video-url",
            duration = "10:00",
            instructor = "Test Instructor",
            publishedDate = "2024-01-01",
            tags = listOf("fitness"),
            viewCount = 1000,
            credibilityScore = 0.8f,
            isBookmarked = false
        )

        // When
        navigationHelper.navigateToContent(mockNavController, article)
        navigationHelper.navigateToContent(mockNavController, news)
        navigationHelper.navigateToContent(mockNavController, video)

        // Then
        verify(exactly = 2) {
            mockNavController.navigate(
                com.example.health_assistant.R.id.articleReaderFragment,
                any(),
                any()
            )
        }
        verify(exactly = 1) {
            mockNavController.navigate(
                com.example.health_assistant.R.id.videoPlayerFragment,
                any(),
                any()
            )
        }
    }

    @Test
    fun `share link generation should create valid URLs`() {
        // When
        val articleLink = deepLinkHandler.generateArticleShareLink("test-article", "article")
        val newsLink = deepLinkHandler.generateArticleShareLink("test-news", "news")
        val videoLink = deepLinkHandler.generateVideoShareLink("test-video")
        val bookmarksLink = deepLinkHandler.generateBookmarksShareLink()

        // Then
        assert(articleLink == "https://healthassistant.app/discover/article/test-article")
        assert(newsLink == "https://healthassistant.app/discover/news/test-news")
        assert(videoLink == "https://healthassistant.app/discover/video/test-video")
        assert(bookmarksLink == "https://healthassistant.app/discover/bookmarks")
    }

    @Test
    fun `cross-feature navigation should work correctly`() {
        // When
        navigationHelper.navigateToJournal(mockNavController)
        navigationHelper.navigateToProfile(mockNavController)

        // Then
        verify {
            mockNavController.navigate(
                com.example.health_assistant.R.id.journalFragment,
                null,
                any()
            )
        }
        verify {
            mockNavController.navigate(
                com.example.health_assistant.R.id.profileFragment,
                null,
                any()
            )
        }
    }
}