package com.example.health_assistant.features.journal.db

import com.example.health_assistant.features.journal.data.JournalEntryEntity
import com.example.health_assistant.features.journal.domain.JournalEntry

/**
 * Extension functions to convert between Entity and Domain objects
 * These converters handle the mapping between the flexible entity structure
 * and the type-safe domain models
 */

fun JournalEntryEntity.toDomain(): JournalEntry {
    return when (type) {
        "mood" -> JournalEntry.Mood(
            id = id,
            timestamp = timestamp,
            moodLevel = moodLevel ?: 3,
            emoji = emoji ?: "😐",
            description = title ?: "Mood Entry",
            note = content ?: ""
        )
        "heart_rate" -> JournalEntry.HeartRate(
            id = id,
            timestamp = timestamp,
            bpm = numericValue1?.toInt() ?: 75,
            state = state ?: "resting",
            note = content ?: ""
        )
        "blood_pressure" -> JournalEntry.BloodPressure(
            id = id,
            timestamp = timestamp,
            systolic = numericValue1?.toInt() ?: 120,
            diastolic = numericValue2?.toInt() ?: 80,
            note = content ?: ""
        )
        "workout" -> JournalEntry.Workout(
            id = id,
            timestamp = timestamp,
            activityType = title ?: "Exercise",
            duration = numericValue1?.toInt() ?: 30,
            summary = content ?: ""
        )
        "weight" -> JournalEntry.Weight(
            id = id,
            timestamp = timestamp,
            weight = numericValue1 ?: 70.0,
            unit = unit ?: "kg",
            note = content ?: ""
        )
        "sleep" -> JournalEntry.Sleep(
            id = id,
            timestamp = timestamp,
            duration = numericValue1?.toInt() ?: 480, // 8 hours in minutes
            quality = numericValue2?.toInt() ?: 3,
            note = content ?: ""
        )
        "note", "generic" -> JournalEntry.Generic(
            id = id,
            timestamp = timestamp,
            type = type,
            content = content ?: title ?: "Journal Entry"
        )
        else -> JournalEntry.Generic(
            id = id,
            timestamp = timestamp,
            type = type,
            content = content ?: "Unknown entry type: $type"
        )
    }
}

fun JournalEntry.toEntity(): JournalEntryEntity {
    return when (this) {
        is JournalEntry.Mood -> JournalEntryEntity(
            id = if (id == 0L) 0 else id, // Let Room auto-generate if 0
            timestamp = timestamp,
            type = "mood",
            title = description,
            content = note,
            moodLevel = moodLevel,
            emoji = emoji
        )
        is JournalEntry.HeartRate -> JournalEntryEntity(
            id = if (id == 0L) 0 else id,
            timestamp = timestamp,
            type = "heart_rate",
            content = note,
            numericValue1 = bpm.toDouble(),
            state = state
        )
        is JournalEntry.BloodPressure -> JournalEntryEntity(
            id = if (id == 0L) 0 else id,
            timestamp = timestamp,
            type = "blood_pressure",
            content = note,
            numericValue1 = systolic.toDouble(),
            numericValue2 = diastolic.toDouble()
        )
        is JournalEntry.Workout -> JournalEntryEntity(
            id = if (id == 0L) 0 else id,
            timestamp = timestamp,
            type = "workout",
            title = activityType,
            content = summary,
            numericValue1 = duration.toDouble()
        )
        is JournalEntry.Weight -> JournalEntryEntity(
            id = if (id == 0L) 0 else id,
            timestamp = timestamp,
            type = "weight",
            content = note,
            numericValue1 = weight,
            unit = unit
        )
        is JournalEntry.Sleep -> JournalEntryEntity(
            id = if (id == 0L) 0 else id,
            timestamp = timestamp,
            type = "sleep",
            content = note,
            numericValue1 = duration.toDouble(),
            numericValue2 = quality.toDouble()
        )
        is JournalEntry.Generic -> JournalEntryEntity(
            id = if (id == 0L) 0 else id,
            timestamp = timestamp,
            type = type,
            content = content
        )
    }
}

/**
 * Utility function to convert a list of entities to domain objects
 */
fun List<JournalEntryEntity>.toDomainList(): List<JournalEntry> {
    return map { it.toDomain() }
}

/**
 * Utility function to convert a list of domain objects to entities
 */
fun List<JournalEntry>.toEntityList(): List<JournalEntryEntity> {
    return map { it.toEntity() }
}