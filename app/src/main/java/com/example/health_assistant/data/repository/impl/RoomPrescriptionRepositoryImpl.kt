package com.example.health_assistant.data.repository.impl

import com.example.health_assistant.data.local.dao.DiseaseCategoryDao
import com.example.health_assistant.data.local.dao.PrescriptionDao
import com.example.health_assistant.data.local.entity.DiseaseCategoryEntity
import com.example.health_assistant.data.local.entity.PrescriptionEntity
import com.example.health_assistant.data.model.DiseaseCategory
import com.example.health_assistant.data.model.Prescription
import com.example.health_assistant.data.repository.interfaces.PrescriptionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Room-based implementation of PrescriptionRepository
 * Provides persistent storage for prescriptions using Room database
 */
@Singleton
class RoomPrescriptionRepositoryImpl @Inject constructor(
    private val prescriptionDao: PrescriptionDao,
    private val diseaseCategoryDao: DiseaseCategoryDao
) : PrescriptionRepository {

    // TODO: Get actual user ID from auth session
    private val currentUserId = "user_1" // Temporary hardcoded user ID

    override fun getAllPrescriptions(): Flow<List<Prescription>> {
        return prescriptionDao.getAllPrescriptionsFlow(currentUserId)
            .map { entities -> entities.map { it.toDomainModel() } }
    }

    override fun getPrescriptionsByCategory(categoryId: String): Flow<List<Prescription>> {
        return prescriptionDao.getPrescriptionsByCategoryFlow(currentUserId, categoryId)
            .map { entities -> entities.map { it.toDomainModel() } }
    }

    override fun searchPrescriptionsByDoctor(doctorName: String): Flow<List<Prescription>> {
        return prescriptionDao.searchPrescriptionsByDoctorFlow(currentUserId, doctorName)
            .map { entities -> entities.map { it.toDomainModel() } }
    }

    override fun getPrescriptionsGroupedByCategory(): Flow<Map<String, List<Prescription>>> {
        return prescriptionDao.getPrescriptionsGroupedFlow(currentUserId)
            .map { entities ->
                entities.groupBy { it.categoryId }
                    .mapValues { (_, prescriptions) ->
                        prescriptions.map { it.toDomainModel() }
                    }
            }
    }

    override suspend fun insertPrescription(prescription: Prescription): Result<Unit> {
        return try {
            val entity = prescription.toEntity(currentUserId)
            prescriptionDao.insertPrescription(entity)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updatePrescription(prescription: Prescription): Result<Unit> {
        return try {
            val entity = prescription.toEntity(currentUserId)
            prescriptionDao.updatePrescription(entity)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deletePrescription(prescriptionId: String): Result<Unit> {
        return try {
            prescriptionDao.deletePrescription(prescriptionId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getPrescriptionById(prescriptionId: String): Prescription? {
        return try {
            prescriptionDao.getPrescriptionById(prescriptionId)?.toDomainModel()
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getPrescriptionCountByCategory(categoryId: String): Int {
        return try {
            prescriptionDao.getPrescriptionCountByCategory(currentUserId, categoryId)
        } catch (e: Exception) {
            0
        }
    }

    override suspend fun deleteAllPrescriptions(): Result<Unit> {
        return try {
            prescriptionDao.deleteAllPrescriptionsForUser(currentUserId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get all disease categories from database
     */
    suspend fun getAllCategories(): List<DiseaseCategory> {
        return try {
            diseaseCategoryDao.getAllCategories().map { it.toDomainModel() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Convert PrescriptionEntity to domain model
     */
    private suspend fun PrescriptionEntity.toDomainModel(): Prescription {
        val category = diseaseCategoryDao.getCategoryById(this.categoryId)?.toDomainModel()
            ?: DiseaseCategory.getDefaultCategory() // Fallback to default category

        return Prescription(
            id = this.id,
            imageUri = this.imageUri,
            localImagePath = this.localImagePath,
            doctorName = this.doctorName,
            diseaseCategory = category,
            dateAdded = Instant.ofEpochMilli(this.dateAdded).atZone(ZoneId.systemDefault()).toLocalDateTime(),
            dateModified = Instant.ofEpochMilli(this.dateModified).atZone(ZoneId.systemDefault()).toLocalDateTime(),
            notes = this.notes,
            userId = this.userId
        )
    }

    /**
     * Convert domain model to PrescriptionEntity
     */
    private fun Prescription.toEntity(userId: String): PrescriptionEntity {
        val currentTime = System.currentTimeMillis()

        return PrescriptionEntity(
            id = this.id,
            userId = userId,
            imageUri = this.imageUri,
            localImagePath = this.localImagePath,
            doctorName = this.doctorName,
            categoryId = this.diseaseCategory.id,
            notes = this.notes,
            fileName = this.localImagePath.substringAfterLast('/'),
            mimeType = "image/jpeg", // Default to JPEG for now
            fileSize = 0L, // TODO: Calculate actual file size
            imageWidth = null,
            imageHeight = null,
            dateAdded = this.dateAdded.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            dateModified = this.dateModified.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            createdAt = currentTime,
            updatedAt = currentTime
        )
    }

    /**
     * Convert DiseaseCategoryEntity to domain model
     */
    private fun DiseaseCategoryEntity.toDomainModel(): DiseaseCategory {
        return DiseaseCategory(
            id = this.id,
            name = this.name,
            displayName = this.displayName,
            iconRes = null, // TODO: Map icon resource names to actual resources
            isCustom = this.isCustom
        )
    }
}