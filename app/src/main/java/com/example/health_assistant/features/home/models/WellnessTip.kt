package com.example.health_assistant.features.home.models

/**
 * Model class for wellness tips displayed in the home screen carousel
 */
data class WellnessTip(
    val id: String,
    val title: String,
    val shortDescription: String,
    val fullContent: String? = null,
    val imageResId: Int? = null,
    val imageUrl: String? = null
)