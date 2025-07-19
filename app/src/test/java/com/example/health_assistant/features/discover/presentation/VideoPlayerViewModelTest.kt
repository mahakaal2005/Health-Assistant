
package com.example.health_assistant.features.discover.presentation

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.health_assistant.core.util.Result
import com.example.health_assistant.features.discover.domain.model.DiscoverContent
import com.example.health_assistant.features.discover.domain.repository.DiscoverRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class VideoPlayerViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private val mockRepository = mockk<DiscoverRepository>()
    private lateinit var viewModel: VideoPlayerViewModel

    private val testVideo = DiscoverContent.Video(
        id = "test-video-1",
        title = "Test Video",
        publishedDate = System.currentTimeMillis(),
        category = "fitness",
        imageUrl = "https://example.com/image.jpg",
        userId = "test-user",
        description = "Test video description",
        thumbnailUrl = "https://example.com/thumbnail.jpg",
        videoUrl = "https://example.com/video.mp4",
        durationSeconds = 300,
        difficultyLevel = "beginner",
        expertName = "Dr. Test",
        expertCredentials = "Test Credentials",
        watchProgress = 0.5f,
        isDownloadedOffline = false,
        transcriptAvailable = true
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = VideoPlayerViewModel(mockRepository)
    }

    @Test
    fun `loadVideo should update currentVideo when successful`() = runTest {
        // Given
        coEvery { mockRepository.getVideoById("test-video-1") } returns Result.Success(testVideo)
        coEvery { mockRepository.isContentBookmarked("test-video-1") } returns Result.Success(false)

        // When
        viewModel.loadVideo("test-video-1")
        advanceUntilIdle()

        // Then
        assertEquals(testVideo, viewModel.currentVideo.value)
        assertEquals(false, viewModel.loading.value)
        assertEquals(false, viewModel.isBookmarked.value)
        assertEquals(VideoPlayerViewModel.DownloadState.NOT_DOWNLOADED, viewModel.downloadState.value)
    }

    @Test
    fun `loadVideo should set error when repository fails`() = runTest {
        // Given
        val errorMessage = "Video not found"
        coEvery { mockRepository.getVideoById("test-video-1") } returns Result.Error(
            Exception(errorMessage), 
            errorMessage
        )

        // When
        viewModel.loadVideo("test-video-1")
        advanceUntilIdle()

        // Then
        assertEquals("Failed to load video: $errorMessage", viewModel.error.value)
        assertEquals(false, viewModel.loading.value)
    }

    @Test
    fun `loadVideo should set playback position from watch progress`() = runTest {
        // Given
        val videoWithProgress = testVideo.copy(watchProgress = 0.3f)
        coEvery { mockRepository.getVideoById("test-video-1") } returns Result.Success(videoWithProgress)
        coEvery { mockRepository.isContentBookmarked("test-video-1") } returns Result.Success(false)

        // When
        viewModel.loadVideo("test-video-1")
        advanceUntilIdle()

        // Then
        val expectedPosition = (videoWithProgress.durationSeconds * videoWithProgress.watchProgress * 1000).toLong()
        assertEquals(expectedPosition, viewModel.playbackPosition.value)
    }

    @Test
    fun `loadVideo should set download state to DOWNLOADED when video is offline`() = runTest {
        // Given
        val offlineVideo = testVideo.copy(isDownloadedOffline = true)
        coEvery { mockRepository.getVideoById("test-video-1") } returns Result.Success(offlineVideo)
        coEvery { mockRepository.isContentBookmarked("test-video-1") } returns Result.Success(false)

        // When
        viewModel.loadVideo("test-video-1")
        advanceUntilIdle()

        // Then
        assertEquals(VideoPlayerViewModel.DownloadState.DOWNLOADED, viewModel.downloadState.value)
    }

    @Test
    fun `updateWatchProgress should call repository and update video`() = runTest {
        // Given
        coEvery { mockRepository.getVideoById("test-video-1") } returns Result.Success(testVideo)
        coEvery { mockRepository.isContentBookmarked("test-video-1") } returns Result.Success(false)
        coEvery { mockRepository.updateWatchProgress("test-video-1", 0.8f) } returns Result.Success(Unit)

        viewModel.loadVideo("test-video-1")
        advanceUntilIdle()

        // When
        viewModel.updateWatchProgress(0.8f)
        advanceUntilIdle()

        // Then
        coVerify { mockRepository.updateWatchProgress("test-video-1", 0.8f) }
        assertEquals(0.8f, viewModel.currentVideo.value?.watchProgress)
    }

    @Test
    fun `updateWatchProgress should set error when repository fails`() = runTest {
        // Given
        coEvery { mockRepository.getVideoById("test-video-1") } returns Result.Success(testVideo)
        coEvery { mockRepository.isContentBookmarked("test-video-1") } returns Result.Success(false)
        coEvery { mockRepository.updateWatchProgress("test-video-1", 0.8f) } returns Result.Error(
            Exception("Update failed"), 
            "Update failed"
        )

        viewModel.loadVideo("test-video-1")
        advanceUntilIdle()

        // When
        viewModel.updateWatchProgress(0.8f)
        advanceUntilIdle()

        // Then
        assertEquals("Failed to save progress: Update failed", viewModel.error.value)
    }

    @Test
    fun `toggleBookmark should add bookmark when not bookmarked`() = runTest {
        // Given
        coEvery { mockRepository.getVideoById("test-video-1") } returns Result.Success(testVideo)
        coEvery { mockRepository.isContentBookmarked("test-video-1") } returns Result.Success(false)
        coEvery { mockRepository.addBookmark("test-video-1", "video") } returns Result.Success(Unit)

        viewModel.loadVideo("test-video-1")
        advanceUntilIdle()

        // When
        viewModel.toggleBookmark()
        advanceUntilIdle()

        // Then
        coVerify { mockRepository.addBookmark("test-video-1", "video") }
        assertEquals(true, viewModel.isBookmarked.value)
    }

    @Test
    fun `toggleBookmark should remove bookmark when bookmarked`() = runTest {
        // Given
        coEvery { mockRepository.getVideoById("test-video-1") } returns Result.Success(testVideo)
        coEvery { mockRepository.isContentBookmarked("test-video-1") } returns Result.Success(true)
        coEvery { mockRepository.removeBookmark("test-video-1") } returns Result.Success(Unit)

        viewModel.loadVideo("test-video-1")
        advanceUntilIdle()

        // When
        viewModel.toggleBookmark()
        advanceUntilIdle()

        // Then
        coVerify { mockRepository.removeBookmark("test-video-1") }
        assertEquals(false, viewModel.isBookmarked.value)
    }

    @Test
    fun `toggleOfflineDownload should start download when not downloaded`() = runTest {
        // Given
        coEvery { mockRepository.getVideoById("test-video-1") } returns Result.Success(testVideo)
        coEvery { mockRepository.isContentBookmarked("test-video-1") } returns Result.Success(false)
        coEvery { mockRepository.downloadVideoForOffline("test-video-1") } returns Result.Success(Unit)

        viewModel.loadVideo("test-video-1")
        advanceUntilIdle()

        // When
        viewModel.toggleOfflineDownload()
        advanceUntilIdle()

        // Then
        coVerify { mockRepository.downloadVideoForOffline("test-video-1") }
        assertEquals(VideoPlayerViewModel.DownloadState.DOWNLOADED, viewModel.downloadState.value)
        assertTrue(viewModel.currentVideo.value?.isDownloadedOffline == true)
    }

    @Test
    fun `toggleOfflineDownload should remove download when downloaded`() = runTest {
        // Given
        val downloadedVideo = testVideo.copy(isDownloadedOffline = true)
        coEvery { mockRepository.getVideoById("test-video-1") } returns Result.Success(downloadedVideo)
        coEvery { mockRepository.isContentBookmarked("test-video-1") } returns Result.Success(false)
        coEvery { mockRepository.removeOfflineVideo("test-video-1") } returns Result.Success(Unit)

        viewModel.loadVideo("test-video-1")
        advanceUntilIdle()

        // When
        viewModel.toggleOfflineDownload()
        advanceUntilIdle()

        // Then
        coVerify { mockRepository.removeOfflineVideo("test-video-1") }
        assertEquals(VideoPlayerViewModel.DownloadState.NOT_DOWNLOADED, viewModel.downloadState.value)
        assertFalse(viewModel.currentVideo.value?.isDownloadedOffline == true)
    }

    @Test
    fun `toggleOfflineDownload should set error state when download fails`() = runTest {
        // Given
        coEvery { mockRepository.getVideoById("test-video-1") } returns Result.Success(testVideo)
        coEvery { mockRepository.isContentBookmarked("test-video-1") } returns Result.Success(false)
        coEvery { mockRepository.downloadVideoForOffline("test-video-1") } returns Result.Error(
            Exception("Download failed"), 
            "Download failed"
        )

        viewModel.loadVideo("test-video-1")
        advanceUntilIdle()

        // When
        viewModel.toggleOfflineDownload()
        advanceUntilIdle()

        // Then
        assertEquals(VideoPlayerViewModel.DownloadState.FAILED, viewModel.downloadState.value)
        assertEquals("Download failed: Download failed", viewModel.error.value)
    }

    @Test
    fun `setPlaybackPosition should update playback position`() = runTest {
        // Given
        val position = 15000L

        // When
        viewModel.setPlaybackPosition(position)

        // Then
        assertEquals(position, viewModel.playbackPosition.value)
    }

    @Test
    fun `getAvailableQualities should return quality options`() = runTest {
        // When
        val qualities = viewModel.getAvailableQualities()

        // Then
        assertEquals(4, qualities.size)
        assertTrue(qualities.contains(VideoPlayerViewModel.VideoQuality.AUTO))
        assertTrue(qualities.contains(VideoPlayerViewModel.VideoQuality.HIGH))
        assertTrue(qualities.contains(VideoPlayerViewModel.VideoQuality.MEDIUM))
        assertTrue(qualities.contains(VideoPlayerViewModel.VideoQuality.LOW))
    }

    @Test
    fun `clearError should clear error message`() = runTest {
        // Given
        coEvery { mockRepository.getVideoById("test-video-1") } returns Result.Error(
            Exception("Test error"), 
            "Test error"
        )

        viewModel.loadVideo("test-video-1")
        advanceUntilIdle()

        // Verify error is set
        assertNotNull(viewModel.error.value)

        // When
        viewModel.clearError()

        // Then
        assertEquals(null, viewModel.error.value)
    }
}