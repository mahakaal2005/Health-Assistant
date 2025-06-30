package com.example.health_assistant.data.local.dao

import androidx.room.*
import com.example.health_assistant.data.local.entity.PrescriptionEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for prescriptions
 * Provides comprehensive CRUD operations with reactive Flow support and advanced querying
 */
@Dao
interface PrescriptionDao {

    /**
     * Get all prescriptions for a user ordered by date (newest first)
     */
    @Query("SELECT * FROM prescriptions WHERE userId = :userId ORDER BY dateAdded DESC")
    fun getAllPrescriptionsFlow(userId: String): Flow<List<PrescriptionEntity>>

    /**
     * Get all prescriptions for a user as suspend function
     */
    @Query("SELECT * FROM prescriptions WHERE userId = :userId ORDER BY dateAdded DESC")
    suspend fun getAllPrescriptions(userId: String): List<PrescriptionEntity>

    /**
     * Get prescription by ID
     */
    @Query("SELECT * FROM prescriptions WHERE id = :prescriptionId")
    suspend fun getPrescriptionById(prescriptionId: String): PrescriptionEntity?

    /**
     * Get prescription by ID as Flow for reactive updates
     */
    @Query("SELECT * FROM prescriptions WHERE id = :prescriptionId")
    fun getPrescriptionByIdFlow(prescriptionId: String): Flow<PrescriptionEntity?>

    /**
     * Get prescriptions by category for a user
     */
    @Query("SELECT * FROM prescriptions WHERE userId = :userId AND categoryId = :categoryId ORDER BY dateAdded DESC")
    fun getPrescriptionsByCategoryFlow(userId: String, categoryId: String): Flow<List<PrescriptionEntity>>

    /**
     * Search prescriptions by doctor name for a user
     */
    @Query("SELECT * FROM prescriptions WHERE userId = :userId AND doctorName LIKE '%' || :doctorName || '%' ORDER BY dateAdded DESC")
    fun searchPrescriptionsByDoctorFlow(userId: String, doctorName: String): Flow<List<PrescriptionEntity>>

    /**
     * Search prescriptions by doctor name as suspend function
     */
    @Query("SELECT * FROM prescriptions WHERE userId = :userId AND doctorName LIKE '%' || :doctorName || '%' ORDER BY dateAdded DESC")
    suspend fun searchPrescriptionsByDoctor(userId: String, doctorName: String): List<PrescriptionEntity>

    /**
     * Get prescriptions grouped by category (for organized display)
     */
    @Query("SELECT * FROM prescriptions WHERE userId = :userId ORDER BY categoryId ASC, dateAdded DESC")
    fun getPrescriptionsGroupedFlow(userId: String): Flow<List<PrescriptionEntity>>

    /**
     * Insert single prescription
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrescription(prescription: PrescriptionEntity)

    /**
     * Insert multiple prescriptions (for data migration/seeding)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrescriptions(prescriptions: List<PrescriptionEntity>)

    /**
     * Update prescription
     */
    @Update
    suspend fun updatePrescription(prescription: PrescriptionEntity)

    /**
     * Delete prescription by ID
     */
    @Query("DELETE FROM prescriptions WHERE id = :prescriptionId")
    suspend fun deletePrescription(prescriptionId: String)

    /**
     * Delete all prescriptions for a user
     */
    @Query("DELETE FROM prescriptions WHERE userId = :userId")
    suspend fun deleteAllPrescriptionsForUser(userId: String)

    /**
     * Delete all prescriptions (for app cleanup)
     */
    @Query("DELETE FROM prescriptions")
    suspend fun deleteAllPrescriptions()

    /**
     * Get prescription count for a user
     */
    @Query("SELECT COUNT(*) FROM prescriptions WHERE userId = :userId")
    suspend fun getPrescriptionCount(userId: String): Int

    /**
     * Get prescription count by category for a user
     */
    @Query("SELECT COUNT(*) FROM prescriptions WHERE userId = :userId AND categoryId = :categoryId")
    suspend fun getPrescriptionCountByCategory(userId: String, categoryId: String): Int

    /**
     * Get total storage used by prescription images for a user
     */
    @Query("SELECT SUM(fileSize) FROM prescriptions WHERE userId = :userId")
    suspend fun getTotalStorageUsed(userId: String): Long?

    /**
     * Get prescriptions older than specified timestamp (for cleanup)
     */
    @Query("SELECT * FROM prescriptions WHERE userId = :userId AND dateAdded < :cutoffTime ORDER BY dateAdded ASC")
    suspend fun getOldPrescriptions(userId: String, cutoffTime: Long): List<PrescriptionEntity>

    /**
     * Delete prescriptions older than specified timestamp
     */
    @Query("DELETE FROM prescriptions WHERE userId = :userId AND dateAdded < :cutoffTime")
    suspend fun deleteOldPrescriptions(userId: String, cutoffTime: Long)

    /**
     * Get recently added prescriptions (last 30 days)
     */
    @Query("SELECT * FROM prescriptions WHERE userId = :userId AND dateAdded > :cutoffTime ORDER BY dateAdded DESC")
    suspend fun getRecentPrescriptions(userId: String, cutoffTime: Long): List<PrescriptionEntity>

    /**
     * Check if prescription exists
     */
    @Query("SELECT COUNT(*) FROM prescriptions WHERE id = :prescriptionId")
    suspend fun prescriptionExists(prescriptionId: String): Int

    /**
     * Get distinct doctor names for autocomplete
     */
    @Query("SELECT DISTINCT doctorName FROM prescriptions WHERE userId = :userId ORDER BY doctorName ASC")
    suspend fun getDistinctDoctorNames(userId: String): List<String>

    /**
     * Update prescription modified timestamp
     */
    @Query("UPDATE prescriptions SET dateModified = :timestamp, updatedAt = :timestamp WHERE id = :prescriptionId")
    suspend fun updateModifiedTimestamp(prescriptionId: String, timestamp: Long = System.currentTimeMillis())
}