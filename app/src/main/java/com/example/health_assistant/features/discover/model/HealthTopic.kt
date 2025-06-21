package com.example.health_assistant.features.discover.model

/**
 * Data class representing a health topic in the Discover section
 */
data class HealthTopic(
    val id: String,
    val title: String,
    val description: String,
    val imageUrl: String? = null,
    val category: String? = null
)