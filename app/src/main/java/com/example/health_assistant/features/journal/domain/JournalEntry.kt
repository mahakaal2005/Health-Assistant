package com.example.health_assistant.features.journal.domain

import java.util.Date

// Domain model representing a journal entry
data class JournalEntry(
    val id: Long = 0,
    val type: String = "journal",
    val timestamp: Long = System.currentTimeMillis(),
    val content: String = "",
    val moodLevel: Int = 0,
    val emoji: String = "",
    val description: String = "",
    val activityType: String? = null,
    val duration: Int? = null,
    val summary: String? = null,
    val goalTitle: String? = null,
    val progress: Float? = null,
    val measurementType: String? = null,
    val value: Float? = null,
    val unit: String? = null,
    val previousValue: Float? = null,
    val state: String? = null,
    val systolic: Int? = null,
    val diastolic: Int? = null,
    val steps: Int? = null,
    val stepGoal: Int? = null,
    val activeMinutes: Int? = null,
    val activeMinutesGoal: Int? = null,
    val calories: Int? = null,
    val caloriesGoal: Int? = null,
    val distance: Float? = null
)

// Extension functions for mapping between domain and data models
fun com.example.health_assistant.features.journal.data.JournalEntryEntity.toJournalEntry(): JournalEntry {
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

fun JournalEntry.toJournalEntryEntity(): com.example.health_assistant.features.journal.data.JournalEntryEntity {
    return com.example.health_assistant.features.journal.data.JournalEntryEntity(
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
