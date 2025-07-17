package com.example.health_assistant.features.prescriptions.data

import com.example.health_assistant.auth.session.SessionManager
import com.example.health_assistant.data.local.dao.PrescriptionDao
import com.example.health_assistant.features.prescriptions.domain.Prescription
import com.example.health_assistant.features.prescriptions.domain.PrescriptionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of PrescriptionRepository
 * Handles data operations and converts between entity and domain models
 * Now with proper user isolation for multi-user support
 */
@Singleton
class PrescriptionRepositoryImpl @Inject constructor(
    private val dao: PrescriptionDao,
    private val sessionManager: SessionManager
) : PrescriptionRepository {

    private fun getCurrentUserId(): String {
        return sessionManager.getCurrentUserId() ?: ""
    }

    override fun getAllPrescriptions(): Flow<List<Prescription>> {
        val userId = getCurrentUserId()
        return if (userId.isNotEmpty()) {
            dao.getAllPrescriptionsByUserId(userId).map { entities ->
                entities.map { it.toDomain() }
            }
        } else {
            dao.getAllPrescriptions().map { entities ->
                entities.map { it.toDomain() }
            }
        }
    }

    override fun getActivePrescriptions(): Flow<List<Prescription>> {
        val userId = getCurrentUserId()
        return if (userId.isNotEmpty()) {
            dao.getActivePrescriptionsByUserId(userId).map { entities ->
                entities.map { it.toDomain() }
            }
        } else {
            dao.getActivePrescriptions().map { entities ->
                entities.map { it.toDomain() }
            }
        }
    }

    override fun getPrescriptionsByCategory(category: String): Flow<List<Prescription>> {
        val userId = getCurrentUserId()
        return if (userId.isNotEmpty()) {
            dao.getPrescriptionsByCategoryAndUserId(category, userId).map { entities ->
                entities.map { it.toDomain() }
            }
        } else {
            dao.getPrescriptionsByCategory(category).map { entities ->
                entities.map { it.toDomain() }
            }
        }
    }

    override fun getPrescriptionsWithReminders(): Flow<List<Prescription>> {
        val userId = getCurrentUserId()
        return if (userId.isNotEmpty()) {
            dao.getPrescriptionsWithRemindersByUserId(userId).map { entities ->
                entities.map { it.toDomain() }
            }
        } else {
            dao.getPrescriptionsWithReminders().map { entities ->
                entities.map { it.toDomain() }
            }
        }
    }

    override fun searchPrescriptions(query: String): Flow<List<Prescription>> {
        val userId = getCurrentUserId()
        return if (userId.isNotEmpty()) {
            dao.searchPrescriptionsByUserId(query, userId).map { entities ->
                entities.map { it.toDomain() }
            }
        } else {
            dao.searchPrescriptions(query).map { entities ->
                entities.map { it.toDomain() }
            }
        }
    }

    override suspend fun getPrescriptionById(id: Long): Prescription? {
        val entity = dao.getPrescriptionById(id)
        
        // Only return the prescription if it belongs to the current user or if no user is logged in
        val userId = getCurrentUserId()
        return if (userId.isNotEmpty() && entity != null) {
            if (entity.userId == userId) entity.toDomain() else null
        } else {
            entity?.toDomain()
        }
    }

    override suspend fun getExpiredPrescriptions(): List<Prescription> {
        val currentTime = System.currentTimeMillis()
        val userId = getCurrentUserId()
        return if (userId.isNotEmpty()) {
            dao.getExpiredPrescriptionsByUserId(currentTime, userId).map { it.toDomain() }
        } else {
            dao.getExpiredPrescriptions(currentTime).map { it.toDomain() }
        }
    }

    override suspend fun getLowStockPrescriptions(threshold: Int): List<Prescription> {
        val userId = getCurrentUserId()
        return if (userId.isNotEmpty()) {
            dao.getLowStockPrescriptionsByUserId(threshold, userId).map { it.toDomain() }
        } else {
            dao.getLowStockPrescriptions(threshold).map { it.toDomain() }
        }
    }

    override suspend fun insertPrescription(prescription: Prescription): Long {
        val userId = getCurrentUserId()
        // Convert to entity and set the userId
        val entity = prescription.toEntity().copy(userId = userId)
        return dao.insertPrescription(entity)
    }

    override suspend fun updatePrescription(prescription: Prescription) {
        val userId = getCurrentUserId()
        // Convert to entity and ensure the userId is set
        val entity = prescription.toEntity().copy(userId = userId)
        dao.updatePrescription(entity)
    }

    override suspend fun deletePrescription(prescription: Prescription) {
        val userId = getCurrentUserId()
        // Convert to entity and ensure the userId is set
        val entity = prescription.toEntity().copy(userId = userId)
        dao.deletePrescription(entity)
    }

    override suspend fun deactivatePrescription(id: Long) {
        // First check if the prescription belongs to the current user
        val userId = getCurrentUserId()
        if (userId.isNotEmpty()) {
            val prescription = dao.getPrescriptionById(id)
            if (prescription != null && prescription.userId == userId) {
                dao.deactivatePrescription(id)
            }
        } else {
            dao.deactivatePrescription(id)
        }
    }

    override suspend fun updatePillCount(id: Long, newCount: Int) {
        // First check if the prescription belongs to the current user
        val userId = getCurrentUserId()
        if (userId.isNotEmpty()) {
            val prescription = dao.getPrescriptionById(id)
            if (prescription != null && prescription.userId == userId) {
                dao.updatePillCount(id, newCount)
            }
        } else {
            dao.updatePillCount(id, newCount)
        }
    }

    override suspend fun getActivePrescriptionCount(): Int {
        val userId = getCurrentUserId()
        return if (userId.isNotEmpty()) {
            dao.getActivePrescriptionCountByUserId(userId)
        } else {
            dao.getActivePrescriptionCount()
        }
    }
}