package com.example.health_assistant.data.model

import java.util.Date

// Domain model
data class Prescription(
    val id: Long = 0,
    val medicationName: String,
    val dosage: String,
    val frequency: String,
    val startDate: Date,
    val endDate: Date?,
    val instructions: String?,
    val doctorName: String?,
    val isActive: Boolean = true,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
    // Additional fields expected by UI
    val userId: String? = null,
    val categoryId: Long? = null,
    val displayName: String? = null, // We'll use this to store category name
    val notes: String? = null,
    val imageUri: String? = null,
    val localImagePath: String? = null,
    val fileName: String? = null,
    val mimeType: String? = null,
    val fileSize: Long? = null,
    val dateAdded: Date = Date(),
    val dateModified: Date = Date()
)

// Extension functions for mapping
fun com.example.health_assistant.data.local.entity.PrescriptionEntity.toPrescription(): Prescription {
    return Prescription(
        id = id,
        medicationName = name,
        dosage = dosage,
        frequency = frequency,
        startDate = java.util.Date(startDate ?: dateCreated), // Use dateCreated as fallback
        endDate = endDate?.let { java.util.Date(it) },
        instructions = instructions,
        doctorName = doctorName,
        isActive = isActive,
        createdAt = java.util.Date(dateCreated),
        updatedAt = java.util.Date(dateModified),
        userId = null, // Not available in entity
        categoryId = null, // Not available in entity
        displayName = diseaseCategory,
        notes = notes,
        imageUri = imagePath,
        localImagePath = imagePath,
        fileName = null,
        mimeType = null,
        fileSize = null,
        dateAdded = java.util.Date(dateCreated),
        dateModified = java.util.Date(dateModified)
    )
}

fun Prescription.toPrescriptionEntity(): com.example.health_assistant.data.local.entity.PrescriptionEntity {
    return com.example.health_assistant.data.local.entity.PrescriptionEntity(
        id = if (id == 0L) 0 else id, // Let Room auto-generate if 0
        name = medicationName,
        doctorName = doctorName ?: "",
        dosage = dosage,
        frequency = frequency,
        duration = "", // Default value
        instructions = instructions ?: "",
        imagePath = localImagePath ?: imageUri,
        dateCreated = createdAt.time,
        dateModified = updatedAt.time,
        isActive = isActive,
        reminderEnabled = false, // Default value
        reminderTimes = "", // Default value
        notes = notes ?: "",
        diseaseCategory = displayName ?: "",
        startDate = startDate.time,
        endDate = endDate?.time,
        pillCount = null,
        refillReminder = false
    )
}