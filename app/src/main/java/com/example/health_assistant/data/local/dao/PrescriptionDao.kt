package com.example.health_assistant.data.local.dao

import androidx.room.*
import com.example.health_assistant.data.local.entity.PrescriptionEntity

@Dao
interface PrescriptionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrescription(prescription: PrescriptionEntity)

    @Query("SELECT * FROM prescriptions WHERE userId = :userId ORDER BY dateAdded DESC")
    suspend fun getAllPrescriptionsForUser(userId: String): List<PrescriptionEntity>

    @Query("SELECT * FROM prescriptions WHERE id = :id")
    suspend fun getPrescriptionById(id: String): PrescriptionEntity?

    @Update
    suspend fun updatePrescription(prescription: PrescriptionEntity)

    @Query("DELETE FROM prescriptions WHERE id = :id")
    suspend fun deletePrescriptionById(id: String)

    @Query("SELECT COUNT(*) FROM prescriptions WHERE userId = :userId")
    suspend fun getPrescriptionCount(userId: String): Int
}