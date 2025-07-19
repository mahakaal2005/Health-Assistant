package com.example.health_assistant.features.discover.presentation

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.health_assistant.core.util.Result
import com.example.health_assistant.features.discover.domain.model.DiscoverContent
import com.example.health_assistant.features.discover.domain.repository.DiscoverRepository
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class BookmarksViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var mockRepository: DiscoverRepository
    private lateinit var viewModel: BookmarksViewModel

    private val testArticle = DiscoverContent.Article(
        id = "article_1",
        title = "Test Article",
        summary = "Test summary",
        content = "Test content",
        category = "health",
        authorName = "Dr. Test",
        authorCredentials = "MD",
        sourceUrl = "https://test.com",
        publishedDate = System.currentTimeMillis(),
        lastUpdated = System.currentTimeMillis(),
        readingTimeMinutes = 5,
        imageUrl = "https://test.com/image.jpg",
        tags = listOf("health", "test"),
        readProgress = 0.5f,
        credibilityScore = 4,
        userId = "test_user_123",
        isBookmarked = true
    )

    private val testNews = DiscoverContent.News(
        id = "news_1",
        title = "Test News",
        summary = "Test news summary",
        fullContent = "Test news content",
        category = "health",
        sourcePublication = "Test Publication",
        sourceCredibility = "peer-reviewed",
        publishedDate = System.currentTimeMillis(),
        imageUrl = "https://test.com/news.jpg",
        externalUrl = "https://test.com/news",
        isBreakingNews = false,
        relevanceScore = 3,
        userId = "test_user_123"
    )

    private val testVideo = DiscoverContent.Video(
        id = "video_1",
        title = "Test Video",
        description = "Test video description",
        category = "fitness",
        thumbnailUrl = "https://test.com/thumb.jpg",
        videoUrl = "https://test.com/video.mp4",
        durationSeconds = 300,
        difficultyLevel = "beginner",
        expertName = "Dr. Video",
        expertCredentials = "PhD",
        publishedDate = System.currentTimeMillis(),
        watchProgress = 0.3f,
        isDownloadedOffline = false,
        transcriptAvailable = true,
        imageUrl = "https://test.com/video.jpg",
        userId = "test_user_123"
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockRepository = mockk()
        viewModel = BookmarksViewModel(mockRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    @Test
    fun `loadBookmarks emits success state with bookmarked content`() = runTest {
        // Given
        val bookmarkedContent = listOf(testArticle, testNews, testVideo)
        coEvery { mockRepository.getBookmarkedContent() } returns flowOf(Result.Success(bookmarkedContent))
        coEvery { mockRepository.getHealthArticles() } returns flowOf(Result.Success(listOf(testArticle)))
        coEvery { mockRepository.getEducationalVideos() } returns flowOf(Result.Success(listOf(testVideo)))

        // When
        viewModel.loadBookmarks()

        // Then
        val state = viewModel.bookmarksState.value
        assertTrue(state is BookmarksViewModel.BookmarksState.Success)
        assertEquals(3, state.bookmarks.size)
        assertEquals(testArticle.id, state.bookmarks[0].id)
    }

    @Test
    fun `loadBookmarks emits empty state when no bookmarks exist`() = runTest {
        // Given
        coEvery { mockRepository.getBookmarkedContent() } returns flowOf(Result.Success(emptyList()))
        coEvery { mockRepository.getHealthArticles() } returns flowOf(Result.Success(emptyList()))
        coEvery { mockRepository.getEducationalVideos() } returns flowOf(Result.Success(emptyList()))

        // When
        viewModel.loadBookmarks()

        // Then
        val state = viewModel.bookmarksState.value
        assertTrue(state is BookmarksViewModel.BookmarksState.Empty)
    }

    @Test
    fun `loadBookmarks emits error state when repository fails`() = runTest {
        // Given
        val errorMessage = "Failed to load bookmarks"
        coEvery { mockRepository.getBookmarkedContent() } returns flowOf(Result.Error(Exception(errorMessage), errorMessage))
        coEvery { mockRepository.getHealthArticles() } returns flowOf(Result.Success(emptyList()))
        coEvery { mockRepository.getEducationalVideos() } returns flowOf(Result.Success(emptyList()))

        // When
        viewModel.loadBookmarks()

        // Then
        val state = viewModel.bookmarksState.value
        assertTrue(state is BookmarksViewModel.BookmarksState.Error)
        assertEquals(errorMessage, state.message)
    }

    @Test
    fun `filterByContentType filters bookmarks correctly`() = runTest {
        // Given
        val bookmarkedContent = listOf(testArticle, testNews, testVideo)
        coEvery { mockRepository.getBookmarkedContent() } returns flowOf(Result.Success(bookmarkedContent))
        coEvery { mockRepository.getHealthArticles() } returns flowOf(Result.Success(listOf(testArticle)))
        coEvery { mockRepository.getEducationalVideos() } returns flowOf(Result.Success(listOf(testVideo)))

        // When
        viewModel.filterByContentType("article")

        // Then
        val state = viewModel.bookmarksState.value
        assertTrue(state is BookmarksViewModel.BookmarksState.Success)
        assertEquals(1, state.bookmarks.size)
        assertTrue(state.bookmarks[0] is DiscoverContent.Article)
    }

    @Test
    fun `sortBookmarks sorts by date published correctly`() = runTest {
        // Given
        val olderArticle = testArticle.copy(id = "old_article", publishedDate = System.currentTimeMillis() - 86400000) // 1 day ago
        val newerArticle = testArticle.copy(id = "new_article", publishedDate = System.currentTimeMillis())
        val bookmarkedContent = listOf(olderArticle, newerArticle)
        
        coEvery { mockRepository.getBookmarkedContent() } returns flowOf(Result.Success(bookmarkedContent))
        coEvery { mockRepository.getHealthArticles() } returns flowOf(Result.Success(listOf(olderArticle, newerArticle)))
        coEvery { mockRepository.getEducationalVideos() } returns flowOf(Result.Success(emptyList()))

        // When
        viewModel.sortBookmarks(BookmarksViewModel.SortOption.DATE_PUBLISHED)

        // Then
        val state = viewModel.bookmarksState.value
        assertTrue(state is BookmarksViewModel.BookmarksState.Success)
        assertEquals(2, state.bookmarks.size)
        assertEquals("new_article", state.bookmarks[0].id) // Newer article should be first
        assertEquals("old_article", state.bookmarks[1].id)
    }

    @Test
    fun `sortBookmarks sorts by content type correctly`() = runTest {
        // Given
        val bookmarkedContent = listOf(testVideo, testNews, testArticle) // Mixed order
        coEvery { mockRepository.getBookmarkedContent() } returns flowOf(Result.Success(bookmarkedContent))
        coEvery { mockRepository.getHealthArticles() } returns flowOf(Result.Success(listOf(testArticle)))
        coEvery { mockRepository.getEducationalVideos() } returns flowOf(Result.Success(listOf(testVideo)))

        // When
        viewModel.sortBookmarks(BookmarksViewModel.SortOption.CONTENT_TYPE)

        // Then
        val state = viewModel.bookmarksState.value
        assertTrue(state is BookmarksViewModel.BookmarksState.Success)
        assertEquals(3, state.bookmarks.size)
        // Should be sorted: Article (0), News (1), Video (2)
        assertTrue(state.bookmarks[0] is DiscoverContent.Article)
        assertTrue(state.bookmarks[1] is DiscoverContent.News)
        assertTrue(state.bookmarks[2] is DiscoverContent.Video)
    }

    @Test
    fun `removeBookmark calls repository removeBookmark`() = runTest {
        // Given
        coEvery { mockRepository.removeBookmark(any()) } returns Result.Success(Unit)
        coEvery { mockRepository.getBookmarkedContent() } returns flowOf(Result.Success(emptyList()))
        coEvery { mockRepository.getHealthArticles() } returns flowOf(Result.Success(emptyList()))
        coEvery { mockRepository.getEducationalVideos() } returns flowOf(Result.Success(emptyList()))

        // When
        viewModel.removeBookmark(testArticle)

        // Then
        coVerify { mockRepository.removeBookmark(testArticle.id) }
    }

    @Test
    fun `removeBookmark handles error correctly`() = runTest {
        // Given
        val errorMessage = "Failed to remove bookmark"
        coEvery { mockRepository.removeBookmark(any()) } returns Result.Error(Exception(errorMessage), errorMessage)
        coEvery { mockRepository.getBookmarkedContent() } returns flowOf(Result.Success(emptyList()))
        coEvery { mockRepository.getHealthArticles() } returns flowOf(Result.Success(emptyList()))
        coEvery { mockRepository.getEducationalVideos() } returns flowOf(Result.Success(emptyList()))

        // When
        viewModel.removeBookmark(testArticle)

        // Then
        val state = viewModel.bookmarksState.value
        assertTrue(state is BookmarksViewModel.BookmarksState.Error)
        assertEquals(errorMessage, state.message)
    }

    @Test
    fun `clearAllBookmarks removes all bookmarks`() = runTest {
        // Given
        val bookmarkedContent = listOf(testArticle, testNews, testVideo)
        coEvery { mockRepository.getBookmarkedContent() } returns flowOf(Result.Success(bookmarkedContent))
        coEvery { mockRepository.removeBookmark(any()) } returns Result.Success(Unit)
        coEvery { mockRepository.getHealthArticles() } returns flowOf(Result.Success(emptyList()))
        coEvery { mockRepository.getEducationalVideos() } returns flowOf(Result.Success(emptyList()))

        // When
        viewModel.clearAllBookmarks()

        // Then
        coVerify { mockRepository.removeBookmark(testArticle.id) }
        coVerify { mockRepository.removeBookmark(testNews.id) }
        coVerify { mockRepository.removeBookmark(testVideo.id) }
    }

    @Test
    fun `refreshBookmarks triggers sync from remote`() = runTest {
        // Given
        coEvery { mockRepository.syncContentFromRemote() } returns Result.Success(Unit)

        // When
        viewModel.refreshBookmarks()

        // Then
        coVerify { mockRepository.syncContentFromRemote() }
    }

    @Test
    fun `reading history state updates correctly`() = runTest {
        // Given
        val articleWithProgress = testArticle.copy(readProgress = 0.75f)
        val videoWithProgress = testVideo.copy(watchProgress = 0.6f)
        
        coEvery { mockRepository.getBookmarkedContent() } returns flowOf(Result.Success(emptyList()))
        coEvery { mockRepository.getHealthArticles() } returns flowOf(Result.Success(listOf(articleWithProgress)))
        coEvery { mockRepository.getEducationalVideos() } returns flowOf(Result.Success(listOf(videoWithProgress)))

        // When
        viewModel.loadBookmarks()

        // Then
        val historyState = viewModel.readingHistoryState.value
        assertTrue(historyState is BookmarksViewModel.ReadingHistoryState.Success)
        assertEquals(2, historyState.history.size)
        assertTrue(historyState.history.containsKey(testArticle.id))
        assertTrue(historyState.history.containsKey(testVideo.id))
        assertEquals(0.75f, historyState.history[testArticle.id]?.progress)
        assertEquals(0.6f, historyState.history[testVideo.id]?.progress)
    }
}