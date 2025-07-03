package com.example.health_assistant.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "prescriptions")
data class PrescriptionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val medicationName: String,
    val dosage: String,
    val frequency: String,
    val startDate: Date,
    val endDate: Date?,
    val instructions: String?,
    val doctorName: String?,
    val isActive: Boolean = true,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)

// Domain model
data class Prescription(
    val id: Long = 0,
    val medicationName: String,
    val dosage: String,
    val frequency: String,
    val startDate: Date,
    val endDate: Date?,
    val instructions: String?,
    val doctorName: String?,
    val isActive: Boolean = true,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)

// Extension functions for mapping
fun PrescriptionEntity.toPrescription(): Prescription {
    return Prescription(
        id = id,
        medicationName = medicationName,
        dosage = dosage,
        frequency = frequency,
        startDate = startDate,
        endDate = endDate,
        instructions = instructions,
        doctorName = doctorName,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun Prescription.toPrescriptionEntity(): PrescriptionEntity {
    return PrescriptionEntity(
        id = id,
        medicationName = medicationName,
        dosage = dosage,
        frequency = frequency,
        startDate = startDate,
        endDate = endDate,
        instructions = instructions,
        doctorName = doctorName,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}