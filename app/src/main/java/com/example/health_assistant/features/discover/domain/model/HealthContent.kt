package com.example.health_assistant.features.discover.domain.model

/**
 * Simple data class for UI display of health content
 * Replaces complex domain models with basic fields needed for display
 */
data class HealthContent(
    val id: String,
    val title: String,
    val description: String,
    val imageUrl: String?,
    val sourceUrl: String,
    val publishedDate: String,
    val contentType: ContentType,
    val sourceName: String? = null
)

/**
 * Content type enumeration
 */
enum class ContentType {
    ARTICLE, NEWS, VIDEO
}

/**
 * Container for sectioned discover content
 */
data class DiscoverSections(
    val articles: List<HealthContent> = emptyList(),
    val news: List<HealthContent> = emptyList(),
    val videos: List<HealthContent> = emptyList()
) {
    fun isEmpty(): Boolean = articles.isEmpty() && news.isEmpty() && videos.isEmpty()
    
    fun hasContent(): Boolean = !isEmpty()
    
    fun getTotalCount(): Int = articles.size + news.size + videos.size
}