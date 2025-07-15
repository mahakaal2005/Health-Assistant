package com.example.health_assistant.features.journal.data

import com.example.health_assistant.features.journal.domain.ActivityCard
import com.google.gson.Gson
import java.time.LocalDate
import java.time.ZoneId

/**
 * Simplified mapper for converting between ActivityCard domain models and JournalEntryEntity
 * Only handles the 3 essential health metrics: steps, calories, heart rate
 */
object ActivityCardMapper {

    private val gson = Gson()

    const val ACTIVITY_CARD_TYPE = "activity_summary"

    /**
     * Convert simplified ActivityCard domain model to JournalEntryEntity
     */
    fun toJournalEntry(activityCard: ActivityCard): JournalEntryEntity {
        val activityData = ActivityCardData(
            stepCount = activityCard.stepCount,
            caloriesBurned = activityCard.caloriesBurned,
            heartPoints = activityCard.heartPoints
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
            description = "Steps, Calories, Heart Rate overview"
        )
    }

    /**
     * Convert JournalEntryEntity to simplified ActivityCard domain model
     */
    fun toActivityCard(journalEntry: JournalEntryEntity): ActivityCard? {
        if (journalEntry.type != ACTIVITY_CARD_TYPE) return null

        val activityData = try {
            gson.fromJson(journalEntry.content, ActivityCardData::class.java)
        } catch (e: Exception) {
            return null
        }

        val date = LocalDate.ofEpochDay(journalEntry.timestamp / (24 * 60 * 60 * 1000))

        return ActivityCard(
            id = journalEntry.id,
            date = date,
            stepCount = activityData.stepCount,
            caloriesBurned = activityData.caloriesBurned,
            heartPoints = activityData.heartPoints
        )
    }

    /**
     * Create a new ActivityCard with today's date
     */
    fun createTodayActivityCard(
        stepCount: Int = 0,
        caloriesBurned: Int = 0,
        heartPoints: Int = 0
    ): ActivityCard {
        return ActivityCard(
            id = 0, // Will be assigned by Room
            date = LocalDate.now(),
            stepCount = stepCount,
            caloriesBurned = caloriesBurned,
            heartPoints = heartPoints
        )
    }
}