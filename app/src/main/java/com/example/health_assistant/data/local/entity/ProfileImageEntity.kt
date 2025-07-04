package com.example.health_assistant.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity for profile images in the database
 * Stores user profile photo information and metadata
 */
@Entity(tableName = "profile_images")
data class ProfileImageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val imagePath: String, // Path to the profile image file
    val fileName: String, // Original file name
    val fileSize: Long, // File size in bytes
    val mimeType: String = "image/jpeg", // MIME type of the image
    val dateCreated: Long = System.currentTimeMillis(),
    val dateModified: Long = System.currentTimeMillis(),
    val isActive: Boolean = true, // Whether this is the current profile image
    val width: Int? = null, // Image width in pixels
    val height: Int? = null, // Image height in pixels
    val description: String = "", // Optional description
    val compressionQuality: Int = 85 // Compression quality used (0-100)
)