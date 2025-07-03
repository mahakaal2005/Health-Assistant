package com.example.health_assistant.features.journal.db

import com.example.health_assistant.features.journal.data.JournalEntryEntity
import com.example.health_assistant.features.journal.domain.JournalEntry

/**
 * Extension function to convert a JournalEntry domain model to JournalEntryEntity database model
 */
fun JournalEntry.toEntity(): JournalEntryEntity {
    return JournalEntryEntity(
        id = id,
        type = type,
        timestamp = timestamp,
        content = content,
        moodLevel = moodLevel,
        emoji = emoji,
        description = description,
        activityType = activityType,
        duration = duration,
        summary = summary,
        goalTitle = goalTitle,
        progress = progress,
        measurementType = measurementType,
        value = value,
        unit = unit,
        previousValue = previousValue,
        state = state,
        systolic = systolic,
        diastolic = diastolic,
        steps = steps,
        stepGoal = stepGoal,
        activeMinutes = activeMinutes,
        activeMinutesGoal = activeMinutesGoal,
        calories = calories,
        caloriesGoal = caloriesGoal,
        distance = distance
    )
}

/**
 * Extension function to convert a JournalEntryEntity database model to JournalEntry domain model
 */
fun JournalEntryEntity.toDomain(): JournalEntry {
    return JournalEntry(
        id = id,
        type = type,
        timestamp = timestamp,
        content = content,
        moodLevel = moodLevel,
        emoji = emoji,
        description = description,
        activityType = activityType,
        duration = duration,
        summary = summary,
        goalTitle = goalTitle,
        progress = progress,
        measurementType = measurementType,
        value = value,
        unit = unit,
        previousValue = previousValue,
        state = state,
        systolic = systolic,
        diastolic = diastolic,
        steps = steps,
        stepGoal = stepGoal,
        activeMinutes = activeMinutes,
        activeMinutesGoal = activeMinutesGoal,
        calories = calories,
        caloriesGoal = caloriesGoal,
        distance = distance
    )
}
