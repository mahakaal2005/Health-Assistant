package com.example.health_assistant.data.repository.impl

import com.example.health_assistant.core.util.Result
import com.example.health_assistant.data.local.dao.PrescriptionDao
import com.example.health_assistant.data.local.dao.DiseaseCategoryDao
import com.example.health_assistant.data.model.Prescription
import com.example.health_assistant.data.model.DiseaseCategory
import com.example.health_assistant.data.model.toDiseaseCategory
import com.example.health_assistant.data.model.toDiseaseCategoryEntity
import com.example.health_assistant.data.model.toPrescription
import com.example.health_assistant.data.model.toPrescriptionEntity
import com.example.health_assistant.data.repository.interfaces.PrescriptionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Room-based implementation of PrescriptionRepository
 */
@Singleton
class RoomPrescriptionRepositoryImpl @Inject constructor(
    private val prescriptionDao: PrescriptionDao,
    private val diseaseCategoryDao: DiseaseCategoryDao
) : PrescriptionRepository {

    override suspend fun insertPrescription(prescription: Prescription): Result<Unit> {
        return try {
            val entity = prescription.toPrescriptionEntity()
            prescriptionDao.insertPrescription(entity)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, e.message ?: "Failed to insert prescription")
        }
    }

    override suspend fun getAllPrescriptions(): Flow<Result<List<Prescription>>> = flow {
        try {
            prescriptionDao.getAllPrescriptions().collect { entities ->
                val prescriptions = entities.map { it.toPrescription() }
                emit(Result.Success(prescriptions))
            }
        } catch (e: Exception) {
            emit(Result.Error(e, e.message ?: "Failed to get prescriptions"))
        }
    }

    override suspend fun getPrescriptionById(id: Long): Result<Prescription?> {
        return try {
            val entity = prescriptionDao.getPrescriptionById(id)
            val prescription = entity?.toPrescription()
            Result.Success(prescription)
        } catch (e: Exception) {
            Result.Error(e, e.message ?: "Failed to get prescription")
        }
    }

    override suspend fun updatePrescription(prescription: Prescription): Result<Unit> {
        return try {
            val entity = prescription.toPrescriptionEntity()
            prescriptionDao.updatePrescription(entity)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, e.message ?: "Failed to update prescription")
        }
    }

    override suspend fun deletePrescription(id: Long): Result<Unit> {
        return try {
            prescriptionDao.deletePrescriptionById(id)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, e.message ?: "Failed to delete prescription")
        }
    }

    override suspend fun categoryExists(categoryId: Long): Boolean {
        return try {
            diseaseCategoryDao.getCategoryById(categoryId) != null
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun getAllCategories(): Result<List<DiseaseCategory>> {
        return try {
            val entityList = diseaseCategoryDao.getAllCategories().first()
            val categories = entityList.map { it.toDiseaseCategory() }
            Result.Success(categories)
        } catch (e: Exception) {
            Result.Error(e, e.message ?: "Failed to get categories")
        }
    }

    override suspend fun initializeDefaultCategories(): Result<Unit> {
        return try {
            val defaultCategories = DiseaseCategory.getDefaultCategories()
            val entities = defaultCategories.map { it.toDiseaseCategoryEntity() }
            diseaseCategoryDao.insertCategories(entities)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, e.message ?: "Failed to initialize categories")
        }
    }
}