package com.example.health_assistant.data.model

import java.time.LocalDateTime
import java.util.UUID

/**
 * Core data model for prescription information
 * Used throughout the app for prescription management
 */
data class Prescription(
    val id: String = UUID.randomUUID().toString(),
    val imageUri: String,
    val localImagePath: String,
    val doctorName: String,
    val diseaseCategory: DiseaseCategory,
    val dateAdded: LocalDateTime,
    val dateModified: LocalDateTime,
    val notes: String? = null,
    val userId: String
) {
    /**
     * Format date for display in UI
     */
    fun getFormattedDateAdded(): String {
        return "${dateAdded.monthValue}/${dateAdded.dayOfMonth}/${dateAdded.year}"
    }

    /**
     * Format date for display in UI
     */
    fun getFormattedDateModified(): String {
        return "${dateModified.monthValue}/${dateModified.dayOfMonth}/${dateModified.year}"
    }

    /**
     * Check if prescription has notes
     */
    fun hasNotes(): Boolean = !notes.isNullOrBlank()

    /**
     * Get display text for date added
     */
    fun getDateAddedDisplay(): String = "Added: ${getFormattedDateAdded()}"

    /**
     * Create a copy with updated modification date
     */
    fun copyWithUpdatedDate(): Prescription {
        return this.copy(dateModified = LocalDateTime.now())
    }
}