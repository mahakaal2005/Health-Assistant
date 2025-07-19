package com.example.health_assistant.features.discover.data.firebase

import com.example.health_assistant.features.discover.data.entity.*
import com.google.firebase.Timestamp

/**
 * Firebase data models for Discover content
 * These models match the Firestore document structure
 */

/**
 * Firebase model for health articles
 */
data class FirebaseHealthArticle(
    val id: String = "",
    val title: String = "",
    val summary: String = "",
    val content: String = "",
    val category: String = "",
    val authorName: String = "",
    val authorCredentials: String = "",
    val sourceUrl: String = "",
    val publishedDate: Timestamp = Timestamp.now(),
    val lastUpdated: Timestamp = Timestamp.now(),
    val readingTimeMinutes: Int = 0,
    val imageUrl: String? = null,
    val tags: List<String> = emptyList(),
    val credibilityScore: Int = 1
) {
    /**
     * Convert Firebase model to Room entity
     */
    fun toEntityModel(userId: String): HealthArticleEntity {
        return HealthArticleEntity(
            id = id,
            title = title,
            summary = summary,
            content = content,
            category = category,
            authorName = authorName,
            authorCredentials = authorCredentials,
            sourceUrl = sourceUrl,
            publishedDate = publishedDate.toDate().time,
            lastUpdated = lastUpdated.toDate().time,
            readingTimeMinutes = readingTimeMinutes,
            imageUrl = imageUrl,
            tags = tags,
            isBookmarked = false, // Will be updated based on bookmarks
            readProgress = 0f,
            credibilityScore = credibilityScore,
            userId = userId
        )
    }
}

/**
 * Firebase model for health news
 */
data class FirebaseHealthNews(
    val id: String = "",
    val headline: String = "",
    val summary: String = "",
    val fullContent: String? = null,
    val category: String = "",
    val sourcePublication: String = "",
    val sourceCredibility: String = "",
    val publishedDate: Timestamp = Timestamp.now(),
    val imageUrl: String? = null,
    val externalUrl: String = "",
    val isBreakingNews: Boolean = false,
    val relevanceScore: Int = 0
) {
    /**
     * Convert Firebase model to Room entity
     */
    fun toEntityModel(userId: String): HealthNewsEntity {
        return HealthNewsEntity(
            id = id,
            headline = headline,
            summary = summary,
            fullContent = fullContent,
            category = category,
            sourcePublication = sourcePublication,
            sourceCredibility = sourceCredibility,
            publishedDate = publishedDate.toDate().time,
            imageUrl = imageUrl,
            externalUrl = externalUrl,
            isBreakingNews = isBreakingNews,
            relevanceScore = relevanceScore,
            userId = userId
        )
    }
}

/**
 * Firebase model for educational videos
 */
data class FirebaseEducationalVideo(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val thumbnailUrl: String = "",
    val videoUrl: String = "",
    val durationSeconds: Int = 0,
    val difficultyLevel: String = "",
    val expertName: String = "",
    val expertCredentials: String = "",
    val publishedDate: Timestamp = Timestamp.now(),
    val transcriptAvailable: Boolean = false
) {
    /**
     * Convert Firebase model to Room entity
     */
    fun toEntityModel(userId: String): EducationalVideoEntity {
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
            publishedDate = publishedDate.toDate().time,
            watchProgress = 0f,
            isDownloadedOffline = false,
            transcriptAvailable = transcriptAvailable,
            userId = userId
        )
    }
}

/**
 * Firebase model for content bookmarks
 */
data class FirebaseContentBookmark(
    val id: String = "",
    val contentId: String = "",
    val contentType: String = "",
    val bookmarkedDate: Timestamp = Timestamp.now(),
    val userId: String = ""
) {
    /**
     * Convert Firebase model to Room entity
     */
    fun toEntityModel(): ContentBookmarkEntity {
        return ContentBookmarkEntity(
            id = id,
            contentId = contentId,
            contentType = contentType,
            bookmarkedDate = bookmarkedDate.toDate().time,
            userId = userId
        )
    }
}

/**
 * Firebase model for user preferences
 */
data class FirebaseUserPreferences(
    val userId: String = "",
    val preferredCategories: List<String> = emptyList(),
    val contentLanguage: String = "en",
    val notificationsEnabled: Boolean = true,
    val offlineDownloadEnabled: Boolean = true,
    val lastUpdated: Timestamp = Timestamp.now()
)