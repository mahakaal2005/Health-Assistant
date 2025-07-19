package com.example.health_assistant.features.discover.data.mapper

import com.example.health_assistant.features.discover.data.entity.*
import com.example.health_assistant.features.discover.domain.model.DiscoverContent

/**
 * Extension functions for mapping between data layer entities and domain models
 */

/**
 * Convert HealthArticleEntity to DiscoverContent.Article
 */
fun HealthArticleEntity.toDomainModel(): DiscoverContent.Article {
    return DiscoverContent.Article(
        id = id,
        title = title,
        publishedDate = publishedDate,
        category = category,
        imageUrl = imageUrl,
        userId = userId,
        summary = summary,
        content = content,
        authorName = authorName,
        authorCredentials = authorCredentials,
        sourceUrl = sourceUrl,
        lastUpdated = lastUpdated,
        readingTimeMinutes = readingTimeMinutes,
        tags = tags,
        isBookmarked = isBookmarked,
        readProgress = readProgress,
        credibilityScore = credibilityScore
    )
}

/**
 * Convert DiscoverContent.Article to HealthArticleEntity
 */
fun DiscoverContent.Article.toEntityModel(): HealthArticleEntity {
    return HealthArticleEntity(
        id = id,
        title = title,
        summary = summary,
        content = content,
        category = category,
        authorName = authorName,
        authorCredentials = authorCredentials,
        sourceUrl = sourceUrl,
        publishedDate = publishedDate,
        lastUpdated = lastUpdated,
        readingTimeMinutes = readingTimeMinutes,
        imageUrl = imageUrl,
        tags = tags,
        isBookmarked = isBookmarked,
        readProgress = readProgress,
        credibilityScore = credibilityScore,
        userId = userId
    )
}

/**
 * Convert HealthNewsEntity to DiscoverContent.News
 */
fun HealthNewsEntity.toDomainModel(): DiscoverContent.News {
    return DiscoverContent.News(
        id = id,
        title = headline,
        publishedDate = publishedDate,
        category = category,
        imageUrl = imageUrl,
        userId = userId,
        summary = summary,
        fullContent = fullContent,
        sourcePublication = sourcePublication,
        sourceCredibility = sourceCredibility,
        externalUrl = externalUrl,
        isBreakingNews = isBreakingNews,
        relevanceScore = relevanceScore
    )
}

/**
 * Convert DiscoverContent.News to HealthNewsEntity
 */
fun DiscoverContent.News.toEntityModel(): HealthNewsEntity {
    return HealthNewsEntity(
        id = id,
        headline = title,
        summary = summary,
        fullContent = fullContent,
        category = category,
        sourcePublication = sourcePublication,
        sourceCredibility = sourceCredibility,
        publishedDate = publishedDate,
        imageUrl = imageUrl,
        externalUrl = externalUrl,
        isBreakingNews = isBreakingNews,
        relevanceScore = relevanceScore,
        userId = userId
    )
}

/**
 * Convert EducationalVideoEntity to DiscoverContent.Video
 */
fun EducationalVideoEntity.toDomainModel(): DiscoverContent.Video {
    return DiscoverContent.Video(
        id = id,
        title = title,
        publishedDate = publishedDate,
        category = category,
        imageUrl = thumbnailUrl,
        userId = userId,
        description = description,
        thumbnailUrl = thumbnailUrl,
        videoUrl = videoUrl,
        durationSeconds = durationSeconds,
        difficultyLevel = difficultyLevel,
        expertName = expertName,
        expertCredentials = expertCredentials,
        watchProgress = watchProgress,
        isDownloadedOffline = isDownloadedOffline,
        transcriptAvailable = transcriptAvailable
    )
}

/**
 * Convert DiscoverContent.Video to EducationalVideoEntity
 */
fun DiscoverContent.Video.toEntityModel(): EducationalVideoEntity {
    return EducationalVideoEntity(
        id = id,
        title = title,
        description = description,
        category = category,
        thumbnailUrl = thumbnailUrl,
        videoUrl = videoUrl,
        durationSeconds = durationSeconds,
        difficultyLevel = difficultyLevel,
        expertName = expertName,
        expertCredentials = expertCredentials,
        publishedDate = publishedDate,
        watchProgress = watchProgress,
        isDownloadedOffline = isDownloadedOffline,
        transcriptAvailable = transcriptAvailable,
        userId = userId
    )
}

/**
 * Convert ContentBookmarkEntity to domain bookmark representation
 */
fun ContentBookmarkEntity.toDomainModel(): ContentBookmark {
    return ContentBookmark(
        id = id,
        contentId = contentId,
        contentType = contentType,
        bookmarkedDate = bookmarkedDate,
        userId = userId
    )
}

/**
 * Domain model for content bookmark
 */
data class ContentBookmark(
    val id: String,
    val contentId: String,
    val contentType: String,
    val bookmarkedDate: Long,
    val userId: String
)

/**
 * Convert list of mixed entities to domain models
 */
fun List<Any>.toDomainContentList(): List<DiscoverContent> {
    return mapNotNull { entity ->
        when (entity) {
            is HealthArticleEntity -> entity.toDomainModel()
            is HealthNewsEntity -> entity.toDomainModel()
            is EducationalVideoEntity -> entity.toDomainModel()
            else -> null
        }
    }
}



/**
 * Helper function to sort mixed content by publication date
 */
fun List<DiscoverContent>.sortedByPublicationDate(): List<DiscoverContent> {
    return sortedByDescending { it.publishedDate }
}

/**
 * Helper function to filter content by category
 */
fun List<DiscoverContent>.filterByCategory(category: String?): List<DiscoverContent> {
    return if (category.isNullOrBlank()) {
        this
    } else {
        filter { it.category.equals(category, ignoreCase = true) }
    }
}

/**
 * Helper function to get content type string
 */
fun DiscoverContent.getContentTypeString(): String {
    return when (this) {
        is DiscoverContent.Article -> "article"
        is DiscoverContent.News -> "news"
        is DiscoverContent.Video -> "video"
    }
}

/**
 * Helper function to get content summary
 */
fun DiscoverContent.getContentSummary(): String {
    return when (this) {
        is DiscoverContent.Article -> summary
        is DiscoverContent.News -> summary
        is DiscoverContent.Video -> description
    }
}

/**
 * Helper function to check if content has image
 */
fun DiscoverContent.hasImage(): Boolean {
    return !imageUrl.isNullOrBlank()
}

/**
 * Helper function to get reading/watching time
 */
fun DiscoverContent.getEstimatedTime(): String {
    return when (this) {
        is DiscoverContent.Article -> "${readingTimeMinutes} min read"
        is DiscoverContent.News -> "Quick read"
        is DiscoverContent.Video -> {
            val minutes = durationSeconds / 60
            val seconds = durationSeconds % 60
            if (minutes > 0) "${minutes}:${seconds.toString().padStart(2, '0')} min"
            else "${seconds}s"
        }
    }
}

/**
 * Helper function to get content credibility indicator
 */
fun DiscoverContent.getCredibilityIndicator(): String {
    return when (this) {
        is DiscoverContent.Article -> when (credibilityScore) {
            5 -> "Peer-reviewed"
            4 -> "Medical expert"
            3 -> "Verified source"
            2 -> "General health"
            else -> "Unverified"
        }
        is DiscoverContent.News -> when (sourceCredibility) {
            "peer-reviewed" -> "Peer-reviewed"
            "medical-journal" -> "Medical journal"
            "health-organization" -> "Health organization"
            else -> "News source"
        }
        is DiscoverContent.Video -> "Expert content"
    }
}

/**
 * Helper function to check if content is recent (within last 7 days)
 */
fun DiscoverContent.isRecent(): Boolean {
    val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)
    return publishedDate > sevenDaysAgo
}

/**
 * Helper function to format publication date
 */
fun DiscoverContent.getFormattedPublishDate(): String {
    val now = System.currentTimeMillis()
    val diff = now - publishedDate
    
    return when {
        diff < 60 * 1000 -> "Just now"
        diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)} minutes ago"
        diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)} hours ago"
        diff < 7 * 24 * 60 * 60 * 1000 -> "${diff / (24 * 60 * 60 * 1000)} days ago"
        else -> {
            val date = java.util.Date(publishedDate)
            java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(date)
        }
    }
}