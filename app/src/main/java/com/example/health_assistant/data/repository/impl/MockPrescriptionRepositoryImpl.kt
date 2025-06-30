package com.example.health_assistant.data.repository.impl

import com.example.health_assistant.data.model.DiseaseCategory
import com.example.health_assistant.data.model.Prescription
import com.example.health_assistant.data.repository.interfaces.PrescriptionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mock implementation of PrescriptionRepository for UI development
 * Uses in-memory storage with StateFlow for reactive updates
 */
@Singleton
class MockPrescriptionRepositoryImpl @Inject constructor() : PrescriptionRepository {

    private val mockPrescriptions = mutableListOf<Prescription>()
    private val _prescriptionsFlow = MutableStateFlow<List<Prescription>>(emptyList())

    init {
        // Add some sample data for UI testing
        initializeSampleData()
    }

    private fun initializeSampleData() {
        val samplePrescriptions = listOf(
            Prescription(
                id = "1",
                imageUri = "sample_prescription_1",
                localImagePath = "/sample/path/1.jpg",
                doctorName = "Dr. John Smith",
                diseaseCategory = DiseaseCategory.findById("cardiology")!!,
                dateAdded = LocalDateTime.now().minusDays(5),
                dateModified = LocalDateTime.now().minusDays(5),
                notes = "Take with food, monitor blood pressure",
                userId = "current_user"
            ),
            Prescription(
                id = "2",
                imageUri = "sample_prescription_2",
                localImagePath = "/sample/path/2.jpg",
                doctorName = "Dr. Sarah Johnson",
                diseaseCategory = DiseaseCategory.findById("diabetes")!!,
                dateAdded = LocalDateTime.now().minusDays(2),
                dateModified = LocalDateTime.now().minusDays(2),
                notes = "Check blood sugar levels twice daily",
                userId = "current_user"
            ),
            Prescription(
                id = "3",
                imageUri = "sample_prescription_3",
                localImagePath = "/sample/path/3.jpg",
                doctorName = "Dr. Michael Chen",
                diseaseCategory = DiseaseCategory.findById("respiratory")!!,
                dateAdded = LocalDateTime.now().minusDays(1),
                dateModified = LocalDateTime.now().minusDays(1),
                userId = "current_user"
            )
        )

        mockPrescriptions.addAll(samplePrescriptions)
        _prescriptionsFlow.value = mockPrescriptions.toList()
    }

    override fun getAllPrescriptions(): Flow<List<Prescription>> {
        return _prescriptionsFlow.asStateFlow()
    }

    override fun getPrescriptionsByCategory(categoryId: String): Flow<List<Prescription>> {
        return _prescriptionsFlow.map { prescriptions ->
            prescriptions.filter { it.diseaseCategory.id == categoryId }
        }
    }

    override fun searchPrescriptionsByDoctor(doctorName: String): Flow<List<Prescription>> {
        return _prescriptionsFlow.map { prescriptions ->
            if (doctorName.isBlank()) {
                prescriptions
            } else {
                prescriptions.filter {
                    it.doctorName.contains(doctorName, ignoreCase = true)
                }
            }
        }
    }

    override fun getPrescriptionsGroupedByCategory(): Flow<Map<String, List<Prescription>>> {
        return _prescriptionsFlow.map { prescriptions ->
            prescriptions.groupBy { it.diseaseCategory.id }
        }
    }

    override suspend fun insertPrescription(prescription: Prescription): Result<Unit> {
        return try {
            mockPrescriptions.add(prescription)
            _prescriptionsFlow.value = mockPrescriptions.toList()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updatePrescription(prescription: Prescription): Result<Unit> {
        return try {
            val index = mockPrescriptions.indexOfFirst { it.id == prescription.id }
            if (index != -1) {
                mockPrescriptions[index] = prescription.copyWithUpdatedDate()
                _prescriptionsFlow.value = mockPrescriptions.toList()
                Result.success(Unit)
            } else {
                Result.failure(Exception("Prescription not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deletePrescription(prescriptionId: String): Result<Unit> {
        return try {
            val removed = mockPrescriptions.removeIf { it.id == prescriptionId }
            if (removed) {
                _prescriptionsFlow.value = mockPrescriptions.toList()
                Result.success(Unit)
            } else {
                Result.failure(Exception("Prescription not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getPrescriptionById(prescriptionId: String): Prescription? {
        return mockPrescriptions.find { it.id == prescriptionId }
    }

    override suspend fun getPrescriptionCountByCategory(categoryId: String): Int {
        return mockPrescriptions.count { it.diseaseCategory.id == categoryId }
    }

    override suspend fun deleteAllPrescriptions(): Result<Unit> {
        return try {
            mockPrescriptions.clear()
            _prescriptionsFlow.value = emptyList()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}