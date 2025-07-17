package com.example.health_assistant.features.prescriptions.data

import com.example.health_assistant.data.local.entity.PrescriptionEntity
import com.example.health_assistant.features.prescriptions.domain.Prescription
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Extension functions to convert between PrescriptionEntity and Prescription domain model
 */

fun PrescriptionEntity.toDomain(): Prescription {
    val gson = Gson()
    val reminderTimesList = if (reminderTimes.isNotEmpty()) {
        try {
            val listType = object : TypeToken<List<String>>() {}.type
            gson.fromJson<List<String>>(reminderTimes, listType) ?: emptyList()
        } catch (e: Exception) {
            // Fallback for simple comma-separated format
            reminderTimes.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        }
    } else {
        emptyList()
    }

    return Prescription(
        id = id,
        name = name,
        doctorName = doctorName,
        dosage = dosage,
        frequency = frequency,
        duration = duration,
        instructions = instructions,
        imagePath = imagePath,
        dateCreated = dateCreated,
        dateModified = dateModified,
        isActive = isActive,
        reminderEnabled = reminderEnabled,
        reminderTimes = reminderTimesList,
        notes = notes,
        diseaseCategory = diseaseCategory,
        startDate = startDate,
        endDate = endDate,
        pillCount = pillCount,
        refillReminder = refillReminder,
        userId = userId
    )
}

fun Prescription.toEntity(): PrescriptionEntity {
    val gson = Gson()
    val reminderTimesJson = if (reminderTimes.isNotEmpty()) {
        gson.toJson(reminderTimes)
    } else {
        ""
    }

    return PrescriptionEntity(
        id = if (id == 0L) 0 else id, // Let Room auto-generate if 0
        name = name,
        doctorName = doctorName,
        dosage = dosage,
        frequency = frequency,
        duration = duration,
        instructions = instructions,
        imagePath = imagePath,
        dateCreated = dateCreated,
        dateModified = dateModified,
        isActive = isActive,
        reminderEnabled = reminderEnabled,
        reminderTimes = reminderTimesJson,
        notes = notes,
        diseaseCategory = diseaseCategory,
        startDate = startDate,
        endDate = endDate,
        pillCount = pillCount,
        refillReminder = refillReminder,
        userId = userId
    )
}

/**
 * Utility functions for list conversions
 */
fun List<PrescriptionEntity>.toDomainList(): List<Prescription> {
    return map { it.toDomain() }
}

fun List<Prescription>.toEntityList(): List<PrescriptionEntity> {
    return map { it.toEntity() }
}