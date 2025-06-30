package com.example.health_assistant.data.repository.interfaces

import com.example.health_assistant.data.model.Prescription
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for prescription data operations
 * Defines contract for both mock and Room implementations
 */
interface PrescriptionRepository {

    /**
     * Get all prescriptions as a Flow for reactive UI updates
     */
    fun getAllPrescriptions(): Flow<List<Prescription>>

    /**
     * Get prescriptions filtered by disease category
     */
    fun getPrescriptionsByCategory(categoryId: String): Flow<List<Prescription>>

    /**
     * Search prescriptions by doctor name with real-time filtering
     */
    fun searchPrescriptionsByDoctor(doctorName: String): Flow<List<Prescription>>

    /**
     * Get prescriptions grouped by category for section headers
     */
    fun getPrescriptionsGroupedByCategory(): Flow<Map<String, List<Prescription>>>

    /**
     * Insert a new prescription
     */
    suspend fun insertPrescription(prescription: Prescription): Result<Unit>

    /**
     * Update an existing prescription
     */
    suspend fun updatePrescription(prescription: Prescription): Result<Unit>

    /**
     * Delete a prescription by ID
     */
    suspend fun deletePrescription(prescriptionId: String): Result<Unit>

    /**
     * Get a single prescription by ID
     */
    suspend fun getPrescriptionById(prescriptionId: String): Prescription?

    /**
     * Get prescription count for a specific category
     */
    suspend fun getPrescriptionCountByCategory(categoryId: String): Int

    /**
     * Delete all prescriptions (for testing/cleanup)
     */
    suspend fun deleteAllPrescriptions(): Result<Unit>
}