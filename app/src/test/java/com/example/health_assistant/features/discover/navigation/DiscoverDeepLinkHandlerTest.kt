package com.example.health_assistant.features.discover.navigation

import android.content.Intent
import android.net.Uri
import androidx.navigation.NavController
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class DiscoverDeepLinkHandlerTest {

    private lateinit var deepLinkHandler: DiscoverDeepLinkHandler
    private lateinit var mockNavigationHelper: DiscoverNavigationHelper
    private lateinit var mockNavController: NavController
    private lateinit var mockIntent: Intent

    @Before
    fun setup() {
        mockNavigationHelper = mockk(relaxed = true)
        mockNavController = mockk(relaxed = true)
        mockIntent = mockk(relaxed = true)
        deepLinkHandler = DiscoverDeepLinkHandler(mockNavigationHelper)
    }

    @Test
    fun `handleDeepLink should return false when intent has no data`() {
        // Given
        every { mockIntent.data } returns null

        // When
        val result = deepLinkHandler.handleDeepLink(mockIntent, mockNavController)

        // Then
        assertFalse(result)
    }

    @Test
    fun `handleDeepLink should handle article deep link correctly`() {
        // Given
        val uri = Uri.parse("healthassistant://discover/article/test-article-123")
        every { mockIntent.data } returns uri

        // When
        val result = deepLinkHandler.handleDeepLink(mockIntent, mockNavController)

        // Then
        assertTrue(result)
        verify {
            mockNavigationHelper.navigateToArticleReaderGlobal(
                mockNavController,
                "test-article-123",
                "article"
            )
        }
    }

    @Test
    fun `handleDeepLink should handle news deep link correctly`() {
        // Given
        val uri = Uri.parse("healthassistant://discover/news/test-news-456")
        every { mockIntent.data } returns uri

        // When
        val result = deepLinkHandler.handleDeepLink(mockIntent, mockNavController)

        // Then
        assertTrue(result)
        verify {
            mockNavigationHelper.navigateToArticleReaderGlobal(
                mockNavController,
                "test-news-456",
                "news"
            )
        }
    }

    @Test
    fun `handleDeepLink should handle video deep link correctly`() {
        // Given
        val uri = Uri.parse("healthassistant://discover/video/test-video-789")
        every { mockIntent.data } returns uri

        // When
        val result = deepLinkHandler.handleDeepLink(mockIntent, mockNavController)

        // Then
        assertTrue(result)
        verify {
            mockNavigationHelper.navigateToVideoPlayerGlobal(
                mockNavController,
                "test-video-789",
                true
            )
        }
    }

    @Test
    fun `handleDeepLink should handle video deep link with autoPlay parameter`() {
        // Given
        val uri = Uri.parse("healthassistant://discover/video/test-video-789?autoPlay=false")
        every { mockIntent.data } returns uri

        // When
        val result = deepLinkHandler.handleDeepLink(mockIntent, mockNavController)

        // Then
        assertTrue(result)
        verify {
            mockNavigationHelper.navigateToVideoPlayerGlobal(
                mockNavController,
                "test-video-789",
                false
            )
        }
    }

    @Test
    fun `handleDeepLink should handle bookmarks deep link correctly`() {
        // Given
        val uri = Uri.parse("healthassistant://discover/bookmarks")
        every { mockIntent.data } returns uri

        // When
        val result = deepLinkHandler.handleDeepLink(mockIntent, mockNavController)

        // Then
        assertTrue(result)
        verify {
            mockNavigationHelper.navigateToBookmarksGlobal(mockNavController)
        }
    }

    @Test
    fun `handleDeepLink should handle discover main deep link correctly`() {
        // Given
        val uri = Uri.parse("healthassistant://discover")
        every { mockIntent.data } returns uri

        // When
        val result = deepLinkHandler.handleDeepLink(mockIntent, mockNavController)

        // Then
        assertTrue(result)
        verify {
            mockNavigationHelper.navigateToDiscoverGlobal(mockNavController)
        }
    }

    @Test
    fun `handleDeepLink should handle web deep links correctly`() {
        // Given
        val uri = Uri.parse("https://healthassistant.app/discover/article/web-article-123")
        every { mockIntent.data } returns uri

        // When
        val result = deepLinkHandler.handleDeepLink(mockIntent, mockNavController)

        // Then
        assertTrue(result)
        verify {
            mockNavigationHelper.navigateToArticleReaderGlobal(
                mockNavController,
                "web-article-123",
                "article"
            )
        }
    }

    @Test
    fun `handleDeepLink should return false for non-discover deep links`() {
        // Given
        val uri = Uri.parse("healthassistant://journal/entry/123")
        every { mockIntent.data } returns uri

        // When
        val result = deepLinkHandler.handleDeepLink(mockIntent, mockNavController)

        // Then
        assertFalse(result)
    }

    @Test
    fun `generateArticleShareLink should return correct URL`() {
        // When
        val shareLink = deepLinkHandler.generateArticleShareLink("test-article-123", "article")

        // Then
        assertEquals("https://healthassistant.app/discover/article/test-article-123", shareLink)
    }

    @Test
    fun `generateVideoShareLink should return correct URL`() {
        // When
        val shareLink = deepLinkHandler.generateVideoShareLink("test-video-456")

        // Then
        assertEquals("https://healthassistant.app/discover/video/test-video-456", shareLink)
    }

    @Test
    fun `generateBookmarksShareLink should return correct URL`() {
        // When
        val shareLink = deepLinkHandler.generateBookmarksShareLink()

        // Then
        assertEquals("https://healthassistant.app/discover/bookmarks", shareLink)
    }
}