package com.example.health_assistant.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for storing disease categories locally
 * Ensures categories are available offline and can be customized
 */
@Entity(tableName = "disease_categories")
data class DiseaseCategoryEntity(
    @PrimaryKey
    val id: String,
    val name: String,              // Internal name (e.g., "cardiology")
    val displayName: String,       // User-facing name (e.g., "Cardiology")
    val iconResName: String?,      // Resource name for icon (e.g., "ic_heart")
    val isCustom: Boolean = false, // Whether user created this category
    val isActive: Boolean = true,  // Whether category is currently active
    val sortOrder: Int = 0,        // Display order (lower = first)
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)