package com.example.health_assistant.features.discover.navigation

import androidx.navigation.NavController
import com.example.health_assistant.R
import com.example.health_assistant.features.discover.domain.model.DiscoverContent
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class DiscoverNavigationHelperTest {

    private lateinit var navigationHelper: DiscoverNavigationHelper
    private lateinit var mockNavController: NavController

    @Before
    fun setup() {
        navigationHelper = DiscoverNavigationHelper()
        mockNavController = mockk(relaxed = true)
    }

    @Test
    fun `navigateToArticleReader should navigate with correct arguments`() {
        // Given
        val articleId = "test-article-123"
        val contentType = "article"

        // When
        navigationHelper.navigateToArticleReader(mockNavController, articleId, contentType)

        // Then
        verify {
            mockNavController.navigate(
                R.id.articleReaderFragment,
                any(),
                any()
            )
        }
    }

    @Test
    fun `navigateToVideoPlayer should navigate with correct arguments`() {
        // Given
        val videoId = "test-video-123"
        val autoPlay = true

        // When
        navigationHelper.navigateToVideoPlayer(mockNavController, videoId, autoPlay)

        // Then
        verify {
            mockNavController.navigate(
                R.id.videoPlayerFragment,
                any(),
                any()
            )
        }
    }

    @Test
    fun `navigateToBookmarks should navigate to bookmarks fragment`() {
        // When
        navigationHelper.navigateToBookmarks(mockNavController)

        // Then
        verify {
            mockNavController.navigate(
                R.id.bookmarksFragment,
                null,
                any()
            )
        }
    }

    @Test
    fun `navigateToContent should navigate to article for Article content`() {
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

        // When
        navigationHelper.navigateToContent(mockNavController, article)

        // Then
        verify {
            mockNavController.navigate(
                R.id.articleReaderFragment,
                any(),
                any()
            )
        }
    }

    @Test
    fun `navigateToContent should navigate to video player for EducationalVideo content`() {
        // Given
        val video = DiscoverContent.EducationalVideo(
            id = "test-video",
            title = "Test Video",
            description = "Test Description",
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
        navigationHelper.navigateToContent(mockNavController, video)

        // Then
        verify {
            mockNavController.navigate(
                R.id.videoPlayerFragment,
                any(),
                any()
            )
        }
    }

    @Test
    fun `navigateToJournal should navigate to journal with correct options`() {
        // When
        navigationHelper.navigateToJournal(mockNavController)

        // Then
        verify {
            mockNavController.navigate(
                R.id.journalFragment,
                null,
                any()
            )
        }
    }

    @Test
    fun `navigateToProfile should navigate to profile with correct options`() {
        // When
        navigationHelper.navigateToProfile(mockNavController)

        // Then
        verify {
            mockNavController.navigate(
                R.id.profileFragment,
                null,
                any()
            )
        }
    }

    @Test
    fun `global navigation methods should use global actions`() {
        // Given
        val articleId = "test-article"
        val videoId = "test-video"

        // When
        navigationHelper.navigateToArticleReaderGlobal(mockNavController, articleId)
        navigationHelper.navigateToVideoPlayerGlobal(mockNavController, videoId)
        navigationHelper.navigateToBookmarksGlobal(mockNavController)
        navigationHelper.navigateToDiscoverGlobal(mockNavController)

        // Then
        verify {
            mockNavController.navigate(R.id.action_global_to_articleReaderFragment, any())
            mockNavController.navigate(R.id.action_global_to_videoPlayerFragment, any())
            mockNavController.navigate(R.id.action_global_to_bookmarksFragment)
            mockNavController.navigate(R.id.action_global_to_discoverFragment)
        }
    }
}