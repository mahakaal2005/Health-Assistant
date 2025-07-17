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
            note = content ?: "",
            userId = userId
        )
        "heart_rate" -> JournalEntry.HeartRate(
            id = id,
            timestamp = timestamp,
            bpm = numericValue1?.toInt() ?: 75,
            state = state ?: "resting",
            note = content ?: "",
            userId = userId
        )
        "blood_pressure" -> JournalEntry.BloodPressure(
            id = id,
            timestamp = timestamp,
            systolic = numericValue1?.toInt() ?: 120,
            diastolic = numericValue2?.toInt() ?: 80,
            note = content ?: "",
            userId = userId
        )
        "workout" -> JournalEntry.Workout(
            id = id,
            timestamp = timestamp,
            activityType = title ?: "Exercise",
            duration = numericValue1?.toInt() ?: 30,
            summary = content ?: "",
            userId = userId
        )
        "weight" -> JournalEntry.Weight(
            id = id,
            timestamp = timestamp,
            weight = numericValue1 ?: 70.0,
            unit = unit ?: "kg",
            note = content ?: "",
            userId = userId
        )
        "sleep" -> JournalEntry.Sleep(
            id = id,
            timestamp = timestamp,
            duration = numericValue1?.toInt() ?: 480, // 8 hours in minutes
            quality = numericValue2?.toInt() ?: 3,
            note = content ?: "",
            userId = userId
        )
        "note", "generic" -> JournalEntry.Generic(
            id = id,
            timestamp = timestamp,
            type = type,
            content = content ?: title ?: "Journal Entry",
            userId = userId
        )
        else -> JournalEntry.Generic(
            id = id,
            timestamp = timestamp,
            type = type,
            content = content ?: "Unknown entry type: $type",
            userId = userId
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
            emoji = emoji,
            userId = userId
        )
        is JournalEntry.HeartRate -> JournalEntryEntity(
            id = if (id == 0L) 0 else id,
            timestamp = timestamp,
            type = "heart_rate",
            content = note,
            numericValue1 = bpm.toDouble(),
            state = state,
            userId = userId
        )
        is JournalEntry.BloodPressure -> JournalEntryEntity(
            id = if (id == 0L) 0 else id,
            timestamp = timestamp,
            type = "blood_pressure",
            content = note,
            numericValue1 = systolic.toDouble(),
            numericValue2 = diastolic.toDouble(),
            userId = userId
        )
        is JournalEntry.Workout -> JournalEntryEntity(
            id = if (id == 0L) 0 else id,
            timestamp = timestamp,
            type = "workout",
            title = activityType,
            content = summary,
            numericValue1 = duration.toDouble(),
            userId = userId
        )
        is JournalEntry.Weight -> JournalEntryEntity(
            id = if (id == 0L) 0 else id,
            timestamp = timestamp,
            type = "weight",
            content = note,
            numericValue1 = weight,
            unit = unit,
            userId = userId
        )
        is JournalEntry.Sleep -> JournalEntryEntity(
            id = if (id == 0L) 0 else id,
            timestamp = timestamp,
            type = "sleep",
            content = note,
            numericValue1 = duration.toDouble(),
            numericValue2 = quality.toDouble(),
            userId = userId
        )
        is JournalEntry.Generic -> JournalEntryEntity(
            id = if (id == 0L) 0 else id,
            timestamp = timestamp,
            type = type,
            content = content,
            userId = userId
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