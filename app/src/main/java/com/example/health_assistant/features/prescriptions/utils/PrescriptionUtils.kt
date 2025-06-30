package com.example.health_assistant.features.prescriptions.utils

import com.example.health_assistant.data.model.DiseaseCategory
import com.example.health_assistant.data.model.Prescription
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Utility functions for prescription management
 */
object PrescriptionUtils {

    /**
     * Format LocalDateTime for display in UI
     */
    fun formatDateTime(dateTime: LocalDateTime): String {
        val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
        return dateTime.format(formatter)
    }

    /**
     * Format LocalDateTime for display in UI (alias for formatDateTime)
     */
    fun formatDate(dateTime: LocalDateTime): String {
        return formatDateTime(dateTime)
    }

    /**
     * Format LocalDateTime for display with time
     */
    fun formatDateTimeWithTime(dateTime: LocalDateTime): String {
        val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' HH:mm")
        return dateTime.format(formatter)
    }

    /**
     * Generate a unique prescription ID
     */
    fun generatePrescriptionId(): String {
        return "prescription_${UUID.randomUUID()}"
    }

    /**
     * Validate prescription data before saving
     */
    fun validatePrescription(
        doctorName: String,
        diseaseCategory: DiseaseCategory?,
        imageUri: String?
    ): PrescriptionValidationResult {
        val errors = mutableListOf<String>()

        if (doctorName.isBlank()) {
            errors.add("Doctor name is required")
        }

        if (diseaseCategory == null) {
            errors.add("Disease category is required")
        }

        if (imageUri.isNullOrBlank()) {
            errors.add("Prescription image is required")
        }

        return if (errors.isEmpty()) {
            PrescriptionValidationResult.Valid
        } else {
            PrescriptionValidationResult.Invalid(errors)
        }
    }

    /**
     * Sort prescriptions by date (newest first)
     */
    fun sortPrescriptionsByDate(prescriptions: List<Prescription>): List<Prescription> {
        return prescriptions.sortedByDescending { it.dateAdded }
    }

    /**
     * Group prescriptions by category and sort within each group
     */
    fun groupAndSortPrescriptions(prescriptions: List<Prescription>): Map<DiseaseCategory, List<Prescription>> {
        return prescriptions
            .groupBy { it.diseaseCategory }
            .mapValues { (_, prescriptions) -> sortPrescriptionsByDate(prescriptions) }
            .toSortedMap(compareBy { it.displayName })
    }

    /**
     * Search prescriptions by doctor name (case-insensitive)
     */
    fun searchByDoctorName(prescriptions: List<Prescription>, query: String): List<Prescription> {
        if (query.isBlank()) return prescriptions

        return prescriptions.filter { prescription ->
            prescription.doctorName.contains(query, ignoreCase = true)
        }
    }

    /**
     * Get prescription count by category
     */
    fun getPrescriptionCountByCategory(
        prescriptions: List<Prescription>,
        category: DiseaseCategory
    ): Int {
        return prescriptions.count { it.diseaseCategory.id == category.id }
    }

    /**
     * Create a new prescription with current timestamp
     */
    fun createPrescription(
        imageUri: String,
        localImagePath: String,
        doctorName: String,
        diseaseCategory: DiseaseCategory,
        notes: String?,
        userId: String
    ): Prescription {
        val now = LocalDateTime.now()
        return Prescription(
            id = generatePrescriptionId(),
            imageUri = imageUri,
            localImagePath = localImagePath,
            doctorName = doctorName.trim(),
            diseaseCategory = diseaseCategory,
            dateAdded = now,
            dateModified = now,
            notes = notes?.takeIf { it.isNotBlank() },
            userId = userId
        )
    }

    /**
     * Validate doctor name format
     */
    fun isValidDoctorName(doctorName: String): Boolean {
        if (doctorName.isBlank()) return false

        // Doctor name should be at least 2 characters
        if (doctorName.trim().length < 2) return false

        // Should contain only letters, spaces, periods, and common name characters
        val namePattern = Regex("^[a-zA-Z\\s.'-]+$")
        if (!namePattern.matches(doctorName.trim())) return false

        // Should not be all spaces or special characters
        if (doctorName.trim().all { it.isWhitespace() || it in ".'- " }) return false

        return true
    }
}

/**
 * Result of prescription validation
 */
sealed class PrescriptionValidationResult {
    object Valid : PrescriptionValidationResult()
    data class Invalid(val errors: List<String>) : PrescriptionValidationResult()
}