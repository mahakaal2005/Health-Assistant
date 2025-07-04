package com.example.health_assistant.features.journal.domain

/**
 * Domain models for journal entries
 * Sealed class to represent different types of journal entries
 */
sealed class JournalEntry {
    abstract val id: Long
    abstract val timestamp: Long
    abstract val type: String

    data class Generic(
        override val id: Long,
        override val timestamp: Long,
        override val type: String = "note",
        val content: String
    ) : JournalEntry()

    data class Mood(
        override val id: Long,
        override val timestamp: Long,
        override val type: String = "mood",
        val moodLevel: Int, // 1-5 scale
        val emoji: String,
        val description: String,
        val note: String = ""
    ) : JournalEntry()

    data class HeartRate(
        override val id: Long,
        override val timestamp: Long,
        override val type: String = "heart_rate",
        val bpm: Int,
        val state: String, // "resting", "active", "exercise"
        val note: String = ""
    ) : JournalEntry()

    data class BloodPressure(
        override val id: Long,
        override val timestamp: Long,
        override val type: String = "blood_pressure",
        val systolic: Int,
        val diastolic: Int,
        val note: String = ""
    ) : JournalEntry()

    data class Workout(
        override val id: Long,
        override val timestamp: Long,
        override val type: String = "workout",
        val activityType: String,
        val duration: Int, // in minutes
        val summary: String = ""
    ) : JournalEntry()

    data class Weight(
        override val id: Long,
        override val timestamp: Long,
        override val type: String = "weight",
        val weight: Double, // in kg
        val unit: String = "kg",
        val note: String = ""
    ) : JournalEntry()

    data class Sleep(
        override val id: Long,
        override val timestamp: Long,
        override val type: String = "sleep",
        val duration: Int, // in minutes
        val quality: Int, // 1-5 scale
        val note: String = ""
    ) : JournalEntry()
}