package com.example.health_assistant.data.local.dao

import androidx.room.*
import com.example.health_assistant.data.local.entity.PrescriptionEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for prescription operations
 * Provides database operations for prescription management
 */
@Dao
interface PrescriptionDao {

    @Query("SELECT * FROM prescriptions ORDER BY dateCreated DESC")
    fun getAllPrescriptions(): Flow<List<PrescriptionEntity>>

    @Query("SELECT * FROM prescriptions WHERE userId = :userId ORDER BY dateCreated DESC")
    fun getAllPrescriptionsByUserId(userId: String): Flow<List<PrescriptionEntity>>

    @Query("SELECT * FROM prescriptions WHERE isActive = 1 ORDER BY dateCreated DESC")
    fun getActivePrescriptions(): Flow<List<PrescriptionEntity>>

    @Query("SELECT * FROM prescriptions WHERE isActive = 1 AND userId = :userId ORDER BY dateCreated DESC")
    fun getActivePrescriptionsByUserId(userId: String): Flow<List<PrescriptionEntity>>

    @Query("SELECT * FROM prescriptions WHERE id = :id")
    suspend fun getPrescriptionById(id: Long): PrescriptionEntity?

    @Query("SELECT * FROM prescriptions WHERE diseaseCategory = :category ORDER BY dateCreated DESC")
    fun getPrescriptionsByCategory(category: String): Flow<List<PrescriptionEntity>>

    @Query("SELECT * FROM prescriptions WHERE diseaseCategory = :category AND userId = :userId ORDER BY dateCreated DESC")
    fun getPrescriptionsByCategoryAndUserId(category: String, userId: String): Flow<List<PrescriptionEntity>>

    @Query("SELECT * FROM prescriptions WHERE reminderEnabled = 1 AND isActive = 1")
    fun getPrescriptionsWithReminders(): Flow<List<PrescriptionEntity>>

    @Query("SELECT * FROM prescriptions WHERE reminderEnabled = 1 AND isActive = 1 AND userId = :userId")
    fun getPrescriptionsWithRemindersByUserId(userId: String): Flow<List<PrescriptionEntity>>

    @Query("SELECT * FROM prescriptions WHERE name LIKE '%' || :searchQuery || '%' OR doctorName LIKE '%' || :searchQuery || '%'")
    fun searchPrescriptions(searchQuery: String): Flow<List<PrescriptionEntity>>

    @Query("SELECT * FROM prescriptions WHERE (name LIKE '%' || :searchQuery || '%' OR doctorName LIKE '%' || :searchQuery || '%') AND userId = :userId")
    fun searchPrescriptionsByUserId(searchQuery: String, userId: String): Flow<List<PrescriptionEntity>>

    @Query("SELECT * FROM prescriptions WHERE endDate IS NOT NULL AND endDate < :currentTime AND isActive = 1")
    suspend fun getExpiredPrescriptions(currentTime: Long): List<PrescriptionEntity>

    @Query("SELECT * FROM prescriptions WHERE endDate IS NOT NULL AND endDate < :currentTime AND isActive = 1 AND userId = :userId")
    suspend fun getExpiredPrescriptionsByUserId(currentTime: Long, userId: String): List<PrescriptionEntity>

    @Query("SELECT * FROM prescriptions WHERE pillCount IS NOT NULL AND pillCount <= :threshold AND isActive = 1")
    suspend fun getLowStockPrescriptions(threshold: Int = 5): List<PrescriptionEntity>

    @Query("SELECT * FROM prescriptions WHERE pillCount IS NOT NULL AND pillCount <= :threshold AND isActive = 1 AND userId = :userId")
    suspend fun getLowStockPrescriptionsByUserId(threshold: Int = 5, userId: String): List<PrescriptionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrescription(prescription: PrescriptionEntity): Long

    @Update
    suspend fun updatePrescription(prescription: PrescriptionEntity)

    @Delete
    suspend fun deletePrescription(prescription: PrescriptionEntity)

    @Query("UPDATE prescriptions SET isActive = 0 WHERE id = :id")
    suspend fun deactivatePrescription(id: Long)

    @Query("UPDATE prescriptions SET pillCount = :newCount WHERE id = :id")
    suspend fun updatePillCount(id: Long, newCount: Int)

    @Query("SELECT COUNT(*) FROM prescriptions WHERE isActive = 1")
    suspend fun getActivePrescriptionCount(): Int

    @Query("SELECT COUNT(*) FROM prescriptions WHERE isActive = 1 AND userId = :userId")
    suspend fun getActivePrescriptionCountByUserId(userId: String): Int
}