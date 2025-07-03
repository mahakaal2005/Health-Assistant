package com.example.health_assistant.data.repository.impl

import com.example.health_assistant.core.util.Result
import com.example.health_assistant.data.model.DiseaseCategory
import com.example.health_assistant.data.model.Prescription
import com.example.health_assistant.data.repository.interfaces.PrescriptionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mock implementation of PrescriptionRepository for UI development
 * Provides sample data without requiring database setup
 */
@Singleton
class MockPrescriptionRepositoryImpl @Inject constructor() : PrescriptionRepository {

    private val mockPrescriptions = mutableListOf<Prescription>()

    init {
        // Add some sample data for UI testing
        initializeSampleData()
    }

    private fun initializeSampleData() {
        val currentTime = System.currentTimeMillis()
        val samplePrescriptions = listOf(
            Prescription(
                id = 1L,
                medicationName = "Lisinopril 10mg",
                dosage = "10mg",
                frequency = "Once daily",
                startDate = Date(currentTime - (5 * 24 * 60 * 60 * 1000L)), // 5 days ago
                endDate = Date(currentTime + (25 * 24 * 60 * 60 * 1000L)), // 25 days from now
                instructions = "Take with food, monitor blood pressure",
                doctorName = "Dr. John Smith",
                isActive = true,
                createdAt = Date(currentTime - (5 * 24 * 60 * 60 * 1000L)),
                updatedAt = Date(currentTime - (5 * 24 * 60 * 60 * 1000L))
            ),
            Prescription(
                id = 2L,
                medicationName = "Metformin 500mg",
                dosage = "500mg",
                frequency = "Twice daily",
                startDate = Date(currentTime - (2 * 24 * 60 * 60 * 1000L)), // 2 days ago
                endDate = Date(currentTime + (28 * 24 * 60 * 60 * 1000L)), // 28 days from now
                instructions = "Take with meals, check blood sugar levels twice daily",
                doctorName = "Dr. Sarah Johnson",
                isActive = true,
                createdAt = Date(currentTime - (2 * 24 * 60 * 60 * 1000L)),
                updatedAt = Date(currentTime - (2 * 24 * 60 * 60 * 1000L))
            ),
            Prescription(
                id = 3L,
                medicationName = "Ibuprofen 400mg",
                dosage = "400mg",
                frequency = "As needed",
                startDate = Date(currentTime - (1 * 24 * 60 * 60 * 1000L)), // 1 day ago
                endDate = Date(currentTime + (7 * 24 * 60 * 60 * 1000L)), // 7 days from now
                instructions = "Take with food for pain relief",
                doctorName = "Dr. Michael Chen",
                isActive = true,
                createdAt = Date(currentTime - (1 * 24 * 60 * 60 * 1000L)),
                updatedAt = Date(currentTime - (1 * 24 * 60 * 60 * 1000L))
            )
        )

        mockPrescriptions.addAll(samplePrescriptions)
    }

    override suspend fun insertPrescription(prescription: Prescription): Result<Unit> {
        return try {
            mockPrescriptions.add(prescription)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, "Failed to insert prescription")
        }
    }

    override suspend fun getAllPrescriptions(): Flow<Result<List<Prescription>>> {
        return flowOf(Result.Success(mockPrescriptions.toList()))
    }

    override suspend fun getPrescriptionById(id: Long): Result<Prescription?> {
        val prescription = mockPrescriptions.find { it.id == id }
        return Result.Success(prescription)
    }

    override suspend fun updatePrescription(prescription: Prescription): Result<Unit> {
        return try {
            val index = mockPrescriptions.indexOfFirst { it.id == prescription.id }
            if (index >= 0) {
                // Update with current timestamp for updatedAt
                val updatedPrescription = prescription.copy(updatedAt = Date())
                mockPrescriptions[index] = updatedPrescription
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, "Failed to update prescription")
        }
    }

    override suspend fun deletePrescription(id: Long): Result<Unit> {
        return try {
            mockPrescriptions.removeAll { it.id == id }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, "Failed to delete prescription")
        }
    }

    override suspend fun categoryExists(categoryId: Long): Boolean {
        // For mock implementation, assume all default categories exist
        val defaultCategories = DiseaseCategory.getDefaultCategories()
        return defaultCategories.any { it.id == categoryId }
    }

    override suspend fun getAllCategories(): Result<List<DiseaseCategory>> {
        return Result.Success(DiseaseCategory.getDefaultCategories())
    }

    override suspend fun initializeDefaultCategories(): Result<Unit> {
        // Mock implementation - categories are always available
        return Result.Success(Unit)
    }
}