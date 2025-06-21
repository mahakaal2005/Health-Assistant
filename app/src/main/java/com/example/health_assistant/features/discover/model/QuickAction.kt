package com.example.health_assistant.features.discover.model

/**
 * Data class representing quick action buttons on the Discover screen
 * These are interactive elements that provide shortcuts to common health-related actions
 */
data class QuickAction(
    val id: String,
    val title: String,
    val iconResId: Int
)