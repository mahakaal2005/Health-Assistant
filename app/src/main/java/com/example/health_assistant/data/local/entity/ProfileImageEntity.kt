package com.example.health_assistant.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for storing profile images locally
 * Ensures profile photos persist until app is uninstalled
 */
@Entity(tableName = "profile_images")
data class ProfileImageEntity(
    @PrimaryKey
    val userId: String,
    val originalUri: String,        // Original selected URI
    val localFilePath: String,      // Copied file path in app storage
    val fileName: String,           // Generated filename
    val mimeType: String?,          // Image MIME type
    val fileSize: Long,             // File size in bytes
    val width: Int?,                // Image width
    val height: Int?,               // Image height
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)