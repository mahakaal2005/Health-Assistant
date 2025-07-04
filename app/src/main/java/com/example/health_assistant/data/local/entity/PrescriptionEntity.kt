package com.example.health_assistant.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity for prescription data in the database
 * Stores prescription information including images and metadata
 */
@Entity(tableName = "prescriptions")
data class PrescriptionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val doctorName: String = "",
    val dosage: String = "",
    val frequency: String = "", // e.g., "2 times daily"
    val duration: String = "", // e.g., "7 days"
    val instructions: String = "",
    val imagePath: String? = null, // Path to prescription image
    val dateCreated: Long = System.currentTimeMillis(),
    val dateModified: Long = System.currentTimeMillis(),
    val isActive: Boolean = true, // Whether prescription is currently active
    val reminderEnabled: Boolean = false,
    val reminderTimes: String = "", // JSON string of reminder times
    val notes: String = "",
    val diseaseCategory: String = "", // Related disease category
    val startDate: Long? = null, // When to start taking medication
    val endDate: Long? = null, // When to stop taking medication
    val pillCount: Int? = null, // Number of pills remaining
    val refillReminder: Boolean = false
)