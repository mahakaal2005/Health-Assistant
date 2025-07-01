package com.example.health_assistant.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for storing disease categories
 */
@Entity(tableName = "disease_categories")
data class DiseaseCategoryEntity(
    @PrimaryKey
    val id: String,
    val displayName: String,
    val description: String? = null,
    val iconRes: Int? = null
)