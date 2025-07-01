package com.example.health_assistant.data.model

import com.example.health_assistant.data.local.entity.PrescriptionEntity
import java.util.UUID

/**
 * Domain model for prescriptions
 */
data class Prescription(
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val imageUri: String,
    val localImagePath: String,
    val doctorName: String,
    val categoryId: String,
    val notes: String? = null,
    val fileName: String,
    val mimeType: String?,
    val fileSize: Long,
    val imageWidth: Int? = null,
    val imageHeight: Int? = null,
    val dateAdded: Long = System.currentTimeMillis(),
    val dateModified: Long = System.currentTimeMillis()
) {
    /**
     * Convert to database entity
     */
    fun toEntity(): PrescriptionEntity {
        return PrescriptionEntity(
            id = id,
            userId = userId,
            imageUri = imageUri,
            localImagePath = localImagePath,
            doctorName = doctorName,
            categoryId = categoryId,
            notes = notes,
            fileName = fileName,
            mimeType = mimeType,
            fileSize = fileSize,
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            dateAdded = dateAdded,
            dateModified = dateModified,
            createdAt = dateAdded,
            updatedAt = dateModified
        )
    }
}

/**
 * Extension function to convert entity back to domain model
 */
fun PrescriptionEntity.toPrescription(): Prescription {
    return Prescription(
        id = id,
        userId = userId,
        imageUri = imageUri,
        localImagePath = localImagePath,
        doctorName = doctorName,
        categoryId = categoryId,
        notes = notes,
        fileName = fileName,
        mimeType = mimeType,
        fileSize = fileSize,
        imageWidth = imageWidth,
        imageHeight = imageHeight,
        dateAdded = dateAdded,
        dateModified = dateModified
    )
}