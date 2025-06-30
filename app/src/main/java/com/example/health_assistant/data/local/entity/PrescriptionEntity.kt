package com.example.health_assistant.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Room entity for storing prescriptions locally
 * Ensures prescriptions persist across app sessions with full metadata
 */
@Entity(
    tableName = "prescriptions",
    foreignKeys = [
        ForeignKey(
            entity = DiseaseCategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_DEFAULT
        )
    ],
    indices = [
        Index(value = ["categoryId"]),
        Index(value = ["userId"]),
        Index(value = ["doctorName"]),
        Index(value = ["dateAdded"])
    ]
)
data class PrescriptionEntity(
    @PrimaryKey
    val id: String,
    val userId: String,                    // User who owns this prescription
    val imageUri: String,                  // Original image URI from camera/gallery
    val localImagePath: String,            // Local file path in app storage
    val doctorName: String,                // Prescribing doctor's name
    val categoryId: String,                // Foreign key to disease category
    val notes: String? = null,             // Optional user notes
    val fileName: String,                  // Generated image filename
    val mimeType: String?,                 // Image MIME type
    val fileSize: Long,                    // Image file size in bytes
    val imageWidth: Int? = null,           // Image width in pixels
    val imageHeight: Int? = null,          // Image height in pixels
    val dateAdded: Long,                   // When prescription was added (timestamp)
    val dateModified: Long,                // When prescription was last modified (timestamp)
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)