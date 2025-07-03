package com.example.health_assistant.data.local.dao

import androidx.room.*
import com.example.health_assistant.data.model.PrescriptionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PrescriptionDao {

    @Query("SELECT * FROM prescriptions WHERE isActive = 1 ORDER BY createdAt DESC")
    fun getAllActivePrescriptions(): Flow<List<PrescriptionEntity>>

    @Query("SELECT * FROM prescriptions ORDER BY createdAt DESC")
    fun getAllPrescriptions(): Flow<List<PrescriptionEntity>>

    @Query("SELECT * FROM prescriptions WHERE id = :id")
    suspend fun getPrescriptionById(id: Long): PrescriptionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrescription(prescription: PrescriptionEntity): Long

    @Update
    suspend fun updatePrescription(prescription: PrescriptionEntity)

    @Delete
    suspend fun deletePrescription(prescription: PrescriptionEntity)

    @Query("UPDATE prescriptions SET isActive = 0 WHERE id = :id")
    suspend fun deactivatePrescription(id: Long)

    @Query("SELECT * FROM prescriptions WHERE medicationName LIKE '%' || :query || '%'")
    fun searchPrescriptions(query: String): Flow<List<PrescriptionEntity>>
}