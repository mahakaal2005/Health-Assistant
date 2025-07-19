package com.example.health_assistant.features.discover.domain.usecase

import com.example.health_assistant.core.util.Result
import com.example.health_assistant.features.discover.domain.model.ContentReport
import com.example.health_assistant.features.discover.domain.model.ContentReportType
import com.example.health_assistant.features.discover.domain.repository.DiscoverRepository
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*

class ReportContentUseCaseTest {
    
    @Mock
    private lateinit var discoverRepository: DiscoverRepository
    
    private lateinit var reportContentUseCase: ReportContentUseCase
    
    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        reportContentUseCase = ReportContentUseCase(discoverRepository)
    }
    
    @Test
    fun `invoke with valid data submits report successfully`() = runTest {
        // Given
        val contentId = "article_123"
        val contentType = "article"
        val reportType = ContentReportType.INACCURATE_INFORMATION
        val description = "This article contains outdated medical information"
        val userId = "user_456"
        
        whenever(discoverRepository.reportContentIssue(any<ContentReport>()))
            .thenReturn(Result.Success(Unit))
        
        // When
        val result = reportContentUseCase(contentId, contentType, reportType, description, userId)
        
        // Then
        assertTrue(result is Result.Success)
        verify(discoverRepository).reportContentIssue(any<ContentReport>())
    }
    
    @Test
    fun `invoke with empty contentId returns error`() = runTest {
        // Given
        val contentId = ""
        val contentType = "article"
        val reportType = ContentReportType.INACCURATE_INFORMATION
        val description = "Test description"
        val userId = "user_456"
        
        // When
        val result = reportContentUseCase(contentId, contentType, reportType, description, userId)
        
        // Then
        assertTrue(result is Result.Error)
        assertEquals("Content ID cannot be empty", (result as Result.Error).message)
    }
    
    @Test
    fun `invoke with OTHER report type and empty description returns error`() = runTest {
        // Given
        val contentId = "article_123"
        val contentType = "article"
        val reportType = ContentReportType.OTHER
        val description = ""
        val userId = "user_456"
        
        // When
        val result = reportContentUseCase(contentId, contentType, reportType, description, userId)
        
        // Then
        assertTrue(result is Result.Error)
        assertEquals("Description is required for 'Other' report type", (result as Result.Error).message)
    }
    
    @Test
    fun `invoke with OTHER report type and valid description succeeds`() = runTest {
        // Given
        val contentId = "article_123"
        val contentType = "article"
        val reportType = ContentReportType.OTHER
        val description = "Custom issue description"
        val userId = "user_456"
        
        whenever(discoverRepository.reportContentIssue(any<ContentReport>()))
            .thenReturn(Result.Success(Unit))
        
        // When
        val result = reportContentUseCase(contentId, contentType, reportType, description, userId)
        
        // Then
        assertTrue(result is Result.Success)
        verify(discoverRepository).reportContentIssue(any<ContentReport>())
    }
    
    @Test
    fun `invoke handles repository error`() = runTest {
        // Given
        val contentId = "article_123"
        val contentType = "article"
        val reportType = ContentReportType.INACCURATE_INFORMATION
        val description = "Test description"
        val userId = "user_456"
        
        whenever(discoverRepository.reportContentIssue(any<ContentReport>()))
            .thenReturn(Result.Error(null, "Network error"))
        
        // When
        val result = reportContentUseCase(contentId, contentType, reportType, description, userId)
        
        // Then
        assertTrue(result is Result.Error)
        assertEquals("Network error", (result as Result.Error).message)
    }
    
    @Test
    fun `invoke handles repository exception`() = runTest {
        // Given
        val contentId = "article_123"
        val contentType = "article"
        val reportType = ContentReportType.INACCURATE_INFORMATION
        val description = "Test description"
        val userId = "user_456"
        
        whenever(discoverRepository.reportContentIssue(any<ContentReport>()))
            .thenThrow(RuntimeException("Database error"))
        
        // When
        val result = reportContentUseCase(contentId, contentType, reportType, description, userId)
        
        // Then
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).message.contains("Failed to submit report"))
    }
    
    @Test
    fun `getUserReports returns user reports successfully`() = runTest {
        // Given
        val userId = "user_456"
        val mockReports = listOf(
            ContentReport(
                id = "report_1",
                contentId = "article_123",
                contentType = "article",
                reportType = ContentReportType.INACCURATE_INFORMATION,
                description = "Test report",
                reporterUserId = userId
            )
        )
        
        whenever(discoverRepository.getUserContentReports(userId))
            .thenReturn(Result.Success(mockReports))
        
        // When
        val result = reportContentUseCase.getUserReports(userId)
        
        // Then
        assertTrue(result is Result.Success)
        assertEquals(mockReports, (result as Result.Success).data)
    }
    
    @Test
    fun `getUserReports handles repository error`() = runTest {
        // Given
        val userId = "user_456"
        
        whenever(discoverRepository.getUserContentReports(userId))
            .thenReturn(Result.Error(null, "Network error"))
        
        // When
        val result = reportContentUseCase.getUserReports(userId)
        
        // Then
        assertTrue(result is Result.Error)
        assertEquals("Network error", (result as Result.Error).message)
    }
    
    @Test
    fun `getUserReports handles repository exception`() = runTest {
        // Given
        val userId = "user_456"
        
        whenever(discoverRepository.getUserContentReports(userId))
            .thenThrow(RuntimeException("Database error"))
        
        // When
        val result = reportContentUseCase.getUserReports(userId)
        
        // Then
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).message.contains("Failed to load reports"))
    }
}