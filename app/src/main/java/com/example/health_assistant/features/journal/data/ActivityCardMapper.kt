package com.example.health_assistant.features.journal.data

import com.example.health_assistant.features.journal.domain.ActivityCard
import com.google.gson.Gson
import java.time.LocalDate
import java.time.ZoneId
import android.util.Log

/**
 * Mapper for converting between ActivityCard domain models and JournalEntryEntity
 * Handles user ID preservation and proper data serialization
 */
object ActivityCardMapper {
    private const val TAG = "ActivityCardMapper"
    private val gson = Gson()
    const val ACTIVITY_CARD_TYPE = "activity_summary"

    /**
     * Convert ActivityCard domain model to JournalEntryEntity
     */
    fun toJournalEntry(activityCard: ActivityCard): JournalEntryEntity {
        val activityData = mapOf(
            "stepCount" to activityCard.stepCount,
            "caloriesBurned" to activityCard.caloriesBurned,
            "heartPoints" to activityCard.heartPoints
        )

        val midnightTimestamp = activityCard.date
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        return JournalEntryEntity(
            id = activityCard.id,
            timestamp = midnightTimestamp,
            type = ACTIVITY_CARD_TYPE,
            title = "Daily Activity Summary",
            content = gson.toJson(activityData),
            description = "Steps: ${activityCard.stepCount}, Calories: ${activityCard.caloriesBurned}, Heart Points: ${activityCard.heartPoints}",
            userId = activityCard.userId // Preserve user ID
        )
    }

    /**
     * Convert JournalEntryEntity to ActivityCard domain model
     */
    fun toActivityCard(journalEntry: JournalEntryEntity): ActivityCard? {
        if (journalEntry.type != ACTIVITY_CARD_TYPE) return null

        return try {
            val activityData = gson.fromJson(journalEntry.content, Map::class.java)
            val date = LocalDate.ofEpochDay(journalEntry.timestamp / (24 * 60 * 60 * 1000))

            ActivityCard(
                id = journalEntry.id,
                date = date,
                stepCount = (activityData["stepCount"] as? Double)?.toInt() ?: 0,
                caloriesBurned = (activityData["caloriesBurned"] as? Double)?.toInt() ?: 0,
                heartPoints = (activityData["heartPoints"] as? Double)?.toInt() ?: 0,
                userId = journalEntry.userId // Preserve user ID
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error converting journal entry to activity card", e)
            null
        }
    }

    /**
     * Create a new ActivityCard with today's date for a specific user
     */
    fun createTodayActivityCard(
        userId: String,
        stepCount: Int = 0,
        caloriesBurned: Int = 0,
        heartPoints: Int = 0
    ): ActivityCard {
        return ActivityCard(
            id = 0, // Will be assigned by Room
            date = LocalDate.now(),
            stepCount = stepCount,
            caloriesBurned = caloriesBurned,
            heartPoints = heartPoints,
            userId = userId
        )
    }
}