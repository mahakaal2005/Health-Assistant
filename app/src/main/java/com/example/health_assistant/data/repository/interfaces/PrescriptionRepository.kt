package com.example.health_assistant.data.repository.interfaces

import com.example.health_assistant.core.util.Result
import com.example.health_assistant.data.model.Prescription
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for prescription operations
 */
interface PrescriptionRepository {

    /**
     * Insert a new prescription
     */
    suspend fun insertPrescription(prescription: Prescription): Result<Unit>

    /**
     * Get all prescriptions for a user
     */
    suspend fun getAllPrescriptions(userId: String): Flow<Result<List<Prescription>>>

    /**
     * Get prescription by ID
     */
    suspend fun getPrescriptionById(id: String): Result<Prescription?>

    /**
     * Update an existing prescription
     */
    suspend fun updatePrescription(prescription: Prescription): Result<Unit>

    /**
     * Delete a prescription by ID
     */
    suspend fun deletePrescription(id: String): Result<Unit>

    /**
     * Check if a category exists in the database
     * This is crucial for foreign key constraint validation
     */
    suspend fun categoryExists(categoryId: String): Boolean

    /**
     * Get all available disease categories
     */
    suspend fun getAllCategories(): Result<List<com.example.health_assistant.data.model.DiseaseCategory>>

    /**
     * Initialize default categories if they don't exist
     */
    suspend fun initializeDefaultCategories(): Result<Unit>
}