package com.example.health_assistant.features.prescriptions.data

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
 */
@Singleton
class PrescriptionRepositoryImpl @Inject constructor(
    private val dao: PrescriptionDao
) : PrescriptionRepository {

    override fun getAllPrescriptions(): Flow<List<Prescription>> {
        return dao.getAllPrescriptions().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getActivePrescriptions(): Flow<List<Prescription>> {
        return dao.getActivePrescriptions().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getPrescriptionsByCategory(category: String): Flow<List<Prescription>> {
        return dao.getPrescriptionsByCategory(category).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getPrescriptionsWithReminders(): Flow<List<Prescription>> {
        return dao.getPrescriptionsWithReminders().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun searchPrescriptions(query: String): Flow<List<Prescription>> {
        return dao.searchPrescriptions(query).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getPrescriptionById(id: Long): Prescription? {
        return dao.getPrescriptionById(id)?.toDomain()
    }

    override suspend fun getExpiredPrescriptions(): List<Prescription> {
        val currentTime = System.currentTimeMillis()
        return dao.getExpiredPrescriptions(currentTime).map { it.toDomain() }
    }

    override suspend fun getLowStockPrescriptions(threshold: Int): List<Prescription> {
        return dao.getLowStockPrescriptions(threshold).map { it.toDomain() }
    }

    override suspend fun insertPrescription(prescription: Prescription): Long {
        return dao.insertPrescription(prescription.toEntity())
    }

    override suspend fun updatePrescription(prescription: Prescription) {
        dao.updatePrescription(prescription.toEntity())
    }

    override suspend fun deletePrescription(prescription: Prescription) {
        dao.deletePrescription(prescription.toEntity())
    }

    override suspend fun deactivatePrescription(id: Long) {
        dao.deactivatePrescription(id)
    }

    override suspend fun updatePillCount(id: Long, newCount: Int) {
        dao.updatePillCount(id, newCount)
    }

    override suspend fun getActivePrescriptionCount(): Int {
        return dao.getActivePrescriptionCount()
    }
}