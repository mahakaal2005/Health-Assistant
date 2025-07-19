package com.example.health_assistant.features.discover.domain.model

/**
 * Data class representing a content report submitted by users
 */
data class ContentReport(
    val id: String = "",
    val contentId: String,
    val contentType: String, // "article", "news", "video"
    val reportType: ContentReportType,
    val description: String,
    val reporterUserId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: ContentReportStatus = ContentReportStatus.PENDING,
    val moderatorNotes: String = ""
)

/**
 * Types of content reports users can submit
 */
enum class ContentReportType(val displayName: String, val description: String) {
    INACCURATE_INFORMATION(
        "Inaccurate Information",
        "The content contains medical or health information that appears to be incorrect"
    ),
    OUTDATED_CONTENT(
        "Outdated Content",
        "The information is no longer current or has been superseded by newer research"
    ),
    MISLEADING_CLAIMS(
        "Misleading Claims",
        "The content makes unsubstantiated or exaggerated health claims"
    ),
    INAPPROPRIATE_CONTENT(
        "Inappropriate Content",
        "The content is not suitable for a health education platform"
    ),
    SPAM_OR_PROMOTIONAL(
        "Spam or Promotional",
        "The content appears to be spam or primarily promotional in nature"
    ),
    COPYRIGHT_VIOLATION(
        "Copyright Violation",
        "The content appears to violate copyright or intellectual property rights"
    ),
    BROKEN_LINK_OR_MEDIA(
        "Broken Link or Media",
        "Links, videos, or images in the content are not working properly"
    ),
    OTHER(
        "Other",
        "Other issues not covered by the above categories"
    )
}

/**
 * Status of a content report in the moderation process
 */
enum class ContentReportStatus(val displayName: String) {
    PENDING("Under Review"),
    INVESTIGATING("Investigating"),
    RESOLVED("Resolved"),
    DISMISSED("Dismissed"),
    CONTENT_REMOVED("Content Removed"),
    CONTENT_UPDATED("Content Updated")
}