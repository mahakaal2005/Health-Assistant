package com.example.health_assistant.features.discover.domain.usecase

import com.example.health_assistant.core.util.Result
import com.example.health_assistant.features.discover.domain.model.ContentReport
import com.example.health_assistant.features.discover.domain.model.ContentReportType
import com.example.health_assistant.features.discover.domain.repository.DiscoverRepository
import javax.inject.Inject

/**
 * Use case for reporting content issues
 */
class ReportContentUseCase @Inject constructor(
    private val discoverRepository: DiscoverRepository
) {
    
    suspend operator fun invoke(
        contentId: String,
        contentType: String,
        reportType: ContentReportType,
        description: String,
        userId: String
    ): Result<Unit> {
        return try {
            // Validate input
            if (contentId.isBlank()) {
                return Result.Error(null, "Content ID cannot be empty")
            }
            
            if (description.isBlank() && reportType == ContentReportType.OTHER) {
                return Result.Error(null, "Description is required for 'Other' report type")
            }
            
            // Create content report
            val report = ContentReport(
                contentId = contentId,
                contentType = contentType,
                reportType = reportType,
                description = description.trim(),
                reporterUserId = userId
            )
            
            // Submit report through repository
            discoverRepository.reportContentIssue(report)
            
        } catch (e: Exception) {
            Result.Error(e, "Failed to submit report: ${e.message}")
        }
    }
    
    suspend fun getUserReports(userId: String): Result<List<ContentReport>> {
        return try {
            discoverRepository.getUserContentReports(userId)
        } catch (e: Exception) {
            Result.Error(e, "Failed to load reports: ${e.message}")
        }
    }
}