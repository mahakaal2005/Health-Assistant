package com.example.health_assistant.features.prescriptions.utils

import com.example.health_assistant.data.model.DiseaseCategory
import com.example.health_assistant.data.model.Prescription
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Utility class for prescription operations
 */
object PrescriptionUtils {

    /**
     * Create a prescription object with proper validation
     */
    fun createPrescription(
        imageUri: String,
        localImagePath: String,
        doctorName: String,
        diseaseCategory: DiseaseCategory,
        notes: String? = null,
        userId: String
    ): Prescription {
        val file = File(localImagePath)
        val fileName = file.name
        val fileSize = if (file.exists()) file.length() else 0L
        val mimeType = getMimeTypeFromPath(localImagePath)

        return Prescription(
            id = 0L, // Use 0 for auto-generated ID
            medicationName = "Unknown Medication", // Required field - should be extracted from OCR or user input
            dosage = "As prescribed", // Required field - default value
            frequency = "As needed", // Required field - default value
            startDate = java.util.Date(), // Required field - default to current date
            endDate = null, // Optional field
            instructions = notes, // Map notes to instructions
            doctorName = doctorName,
            isActive = true,
            createdAt = java.util.Date(),
            updatedAt = java.util.Date(),
            userId = userId,
            categoryId = diseaseCategory.id,
            displayName = doctorName, // Use doctor name as display name
            notes = notes,
            imageUri = imageUri,
            localImagePath = localImagePath,
            fileName = fileName,
            mimeType = mimeType,
            fileSize = fileSize,
            dateAdded = java.util.Date(),
            dateModified = java.util.Date()
        )
    }

    /**
     * Validate prescription data before saving
     */
    fun validatePrescription(
        doctorName: String,
        diseaseCategory: DiseaseCategory?,
        imageUri: String?
    ): PrescriptionValidationResult {
        return when {
            doctorName.isBlank() -> PrescriptionValidationResult.InvalidDoctorName
            diseaseCategory == null -> PrescriptionValidationResult.InvalidCategory
            imageUri.isNullOrBlank() -> PrescriptionValidationResult.InvalidImage
            else -> PrescriptionValidationResult.Valid
        }
    }

    /**
     * Format timestamp to readable date string
     */
    fun formatDate(timestamp: Long): String {
        val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        return formatter.format(Date(timestamp))
    }

    /**
     * Format timestamp to readable date and time string
     */
    fun formatDateTime(timestamp: Long): String {
        val formatter = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
        return formatter.format(Date(timestamp))
    }

    /**
     * Validate doctor name
     */
    fun isValidDoctorName(doctorName: String): Boolean {
        return doctorName.isNotBlank() &&
               doctorName.length >= 2 &&
               doctorName.length <= 100 &&
               doctorName.trim() == doctorName && // No leading/trailing spaces
               doctorName.matches(Regex("^[a-zA-Z\\s.'-]+$")) // Only letters, spaces, periods, apostrophes, hyphens
    }

    /**
     * Get category by ID from default categories
     */
    fun getCategoryById(categoryId: Long?): DiseaseCategory? {
        return if (categoryId != null) {
            DiseaseCategory.getDefaultCategories().find { it.id == categoryId }
        } else {
            null
        }
    }

    /**
     * Get category by String ID (for backward compatibility)
     */
    fun getCategoryById(categoryId: String?): DiseaseCategory? {
        val longId = categoryId?.toLongOrNull()
        return getCategoryById(longId)
    }

    /**
     * Get MIME type from file path
     */
    private fun getMimeTypeFromPath(path: String): String {
        return when {
            path.endsWith(".jpg", ignoreCase = true) ||
            path.endsWith(".jpeg", ignoreCase = true) -> "image/jpeg"
            path.endsWith(".png", ignoreCase = true) -> "image/png"
            path.endsWith(".webp", ignoreCase = true) -> "image/webp"
            else -> "image/jpeg" // Default to JPEG
        }
    }
}

/**
 * Validation result for prescription data
 */
sealed class PrescriptionValidationResult {
    object Valid : PrescriptionValidationResult()
    object InvalidDoctorName : PrescriptionValidationResult()
    object InvalidCategory : PrescriptionValidationResult()
    object InvalidImage : PrescriptionValidationResult()
}