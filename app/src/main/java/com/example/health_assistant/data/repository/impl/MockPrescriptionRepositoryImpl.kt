package com.example.health_assistant.data.repository.impl

import com.example.health_assistant.core.util.Result
import com.example.health_assistant.data.model.DiseaseCategory
import com.example.health_assistant.data.model.Prescription
import com.example.health_assistant.data.repository.interfaces.PrescriptionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
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
                id = "1",
                userId = "current_user",
                imageUri = "sample_prescription_1",
                localImagePath = "/sample/path/1.jpg",
                doctorName = "Dr. John Smith",
                categoryId = "cardiology", // Use categoryId instead of diseaseCategory
                notes = "Take with food, monitor blood pressure",
                fileName = "prescription_1.jpg",
                mimeType = "image/jpeg",
                fileSize = 1024L,
                dateAdded = currentTime - (5 * 24 * 60 * 60 * 1000L), // 5 days ago
                dateModified = currentTime - (5 * 24 * 60 * 60 * 1000L)
            ),
            Prescription(
                id = "2",
                userId = "current_user",
                imageUri = "sample_prescription_2",
                localImagePath = "/sample/path/2.jpg",
                doctorName = "Dr. Sarah Johnson",
                categoryId = "endocrinology", // Use valid category ID
                notes = "Check blood sugar levels twice daily",
                fileName = "prescription_2.jpg",
                mimeType = "image/jpeg",
                fileSize = 2048L,
                dateAdded = currentTime - (2 * 24 * 60 * 60 * 1000L), // 2 days ago
                dateModified = currentTime - (2 * 24 * 60 * 60 * 1000L)
            ),
            Prescription(
                id = "3",
                userId = "current_user",
                imageUri = "sample_prescription_3",
                localImagePath = "/sample/path/3.jpg",
                doctorName = "Dr. Michael Chen",
                categoryId = "general", // Use valid category ID
                fileName = "prescription_3.jpg",
                mimeType = "image/jpeg",
                fileSize = 1536L,
                dateAdded = currentTime - (1 * 24 * 60 * 60 * 1000L), // 1 day ago
                dateModified = currentTime - (1 * 24 * 60 * 60 * 1000L)
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

    override suspend fun getAllPrescriptions(userId: String): Flow<Result<List<Prescription>>> {
        return flowOf(Result.Success(mockPrescriptions.filter { it.userId == userId }))
    }

    override suspend fun getPrescriptionById(id: String): Result<Prescription?> {
        val prescription = mockPrescriptions.find { it.id == id }
        return Result.Success(prescription)
    }

    override suspend fun updatePrescription(prescription: Prescription): Result<Unit> {
        return try {
            val index = mockPrescriptions.indexOfFirst { it.id == prescription.id }
            if (index >= 0) {
                // Update with current timestamp for dateModified
                val updatedPrescription = prescription.copy(dateModified = System.currentTimeMillis())
                mockPrescriptions[index] = updatedPrescription
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, "Failed to update prescription")
        }
    }

    override suspend fun deletePrescription(id: String): Result<Unit> {
        return try {
            mockPrescriptions.removeAll { it.id == id }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, "Failed to delete prescription")
        }
    }


    override suspend fun categoryExists(categoryId: String): Boolean {
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