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
            id = UUID.randomUUID().toString(),
            userId = userId,
            imageUri = imageUri,
            localImagePath = localImagePath,
            doctorName = doctorName,
            categoryId = diseaseCategory.id, // Use the category ID for foreign key
            notes = notes,
            fileName = fileName,
            mimeType = mimeType,
            fileSize = fileSize,
            dateAdded = System.currentTimeMillis(),
            dateModified = System.currentTimeMillis()
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
    fun getCategoryById(categoryId: String): DiseaseCategory? {
        return DiseaseCategory.getDefaultCategories().find { it.id == categoryId }
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