package com.example.health_assistant.features.prescriptions.domain

import kotlinx.coroutines.flow.Flow

/**
 * Domain model for prescription data
 * Clean architecture domain representation
 */
data class Prescription(
    val id: Long = 0,
    val name: String,
    val doctorName: String = "",
    val dosage: String = "",
    val frequency: String = "",
    val duration: String = "",
    val instructions: String = "",
    val imagePath: String? = null,
    val dateCreated: Long = System.currentTimeMillis(),
    val dateModified: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    val reminderEnabled: Boolean = false,
    val reminderTimes: List<String> = emptyList(),
    val notes: String = "",
    val diseaseCategory: String = "",
    val startDate: Long? = null,
    val endDate: Long? = null,
    val pillCount: Int? = null,
    val refillReminder: Boolean = false,
    val userId: String = "" // User ID to associate prescriptions with specific users
)

/**
 * Repository interface for prescription operations
 */
interface PrescriptionRepository {
    fun getAllPrescriptions(): Flow<List<Prescription>>
    fun getActivePrescriptions(): Flow<List<Prescription>>
    fun getPrescriptionsByCategory(category: String): Flow<List<Prescription>>
    fun getPrescriptionsWithReminders(): Flow<List<Prescription>>
    fun searchPrescriptions(query: String): Flow<List<Prescription>>

    suspend fun getPrescriptionById(id: Long): Prescription?
    suspend fun getExpiredPrescriptions(): List<Prescription>
    suspend fun getLowStockPrescriptions(threshold: Int = 5): List<Prescription>
    suspend fun insertPrescription(prescription: Prescription): Long
    suspend fun updatePrescription(prescription: Prescription)
    suspend fun deletePrescription(prescription: Prescription)
    suspend fun deactivatePrescription(id: Long)
    suspend fun updatePillCount(id: Long, newCount: Int)
    suspend fun getActivePrescriptionCount(): Int
}