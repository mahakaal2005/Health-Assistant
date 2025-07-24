package com.example.health_assistant.features.journal.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.example.health_assistant.R
import com.example.health_assistant.core.design.components.HealthCardComponent
import com.example.health_assistant.core.design.tokens.HealthColors
import com.example.health_assistant.core.design.tokens.HealthTypography
import com.example.health_assistant.features.journal.domain.JournalEntry
import java.text.SimpleDateFormat
import java.util.*

/**
 * Journal Entry Card Component
 * 
 * Standardized card component for displaying journal entries using HealthCardComponent
 * with consistent styling across Activity, Note, and Diary entry types.
 * 
 * Features:
 * - Uses HealthCardComponent.Primary for Activity entries
 * - Uses HealthCardComponent.Secondary for Note and Diary entries  
 * - Consistent typography hierarchy using HealthTypography tokens
 * - Proper accessibility support with content descriptions
 */
class JournalEntryCardComponent @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val healthCard: HealthCardComponent
    private val contentLayout: ConstraintLayout
    private val entryTitle: TextView
    private val entryDescription: TextView
    private val entrySummary: TextView
    private val entryDate: TextView
    private val entryType: TextView

    init {
        // Create the health card component
        healthCard = HealthCardComponent(context)
        healthCard.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        addView(healthCard)

        // Inflate the content layout inside the card
        val inflater = LayoutInflater.from(context)
        contentLayout = inflater.inflate(R.layout.journal_entry_card_content, healthCard, false) as ConstraintLayout
        healthCard.addView(contentLayout)

        // Get references to child views
        entryTitle = contentLayout.findViewById(R.id.entryTitle)
        entryDescription = contentLayout.findViewById(R.id.entryDescription)
        entrySummary = contentLayout.findViewById(R.id.entrySummary)
        entryDate = contentLayout.findViewById(R.id.entryDate)
        entryType = contentLayout.findViewById(R.id.entryType)

        // Apply consistent typography
        setupTypography()
    }

    /**
     * Delegate click listeners to the health card
     */
    override fun setOnClickListener(l: OnClickListener?) {
        healthCard.setOnClickListener(l)
    }

    override fun setOnLongClickListener(l: OnLongClickListener?) {
        healthCard.setOnLongClickListener(l)
    }

    /**
     * Apply consistent typography hierarchy using HealthTypography tokens
     */
    private fun setupTypography() {
        entryTitle.setTextAppearance(HealthTypography.Title.medium)
        entryDescription.setTextAppearance(HealthTypography.Body.large)
        entrySummary.setTextAppearance(HealthTypography.Body.small)
        entryDate.setTextAppearance(HealthTypography.Caption.default)
        entryType.setTextAppearance(HealthTypography.Caption.default)
    }

    /**
     * Bind journal entry data to the card with appropriate styling
     */
    fun bindEntry(entry: JournalEntry) {
        // Format date with locale
        val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        entryDate.text = dateFormat.format(Date(entry.timestamp))

        when (entry) {
            is JournalEntry.Generic -> {
                when (entry.type.lowercase()) {
                    "activity_card", "activity_summary" -> {
                        bindActivityEntry(entry)
                    }
                    "note" -> {
                        bindNoteEntry(entry)
                    }
                    "diary" -> {
                        bindDiaryEntry(entry)
                    }
                    else -> {
                        bindGenericEntry(entry)
                    }
                }
            }
            is JournalEntry.Workout -> {
                bindWorkoutEntry(entry)
            }
            else -> {
                bindOtherEntry(entry)
            }
        }
    }

    /**
     * Bind Activity entry with Primary card styling
     */
    private fun bindActivityEntry(entry: JournalEntry.Generic) {
        healthCard.setCardType(HealthCardComponent.HealthCardType.PRIMARY)
        
        entryTitle.text = "Daily Activity Summary"
        entryTitle.setTextColor(ContextCompat.getColor(context, HealthColors.Text.primary))
        
        // Parse activity data
        val content = entry.content ?: ""
        val activityData = parseActivityData(content)
        
        val activityDescription = buildString {
            append("🚶‍♂️ ${String.format("%,d", activityData.steps)} steps")
            append("\n🔥 ${activityData.calories} calories burned")
            append("\n❤️ ${activityData.heartPoints} heart points earned")
        }
        
        entryDescription.text = activityDescription
        entryDescription.setTextColor(ContextCompat.getColor(context, HealthColors.Text.secondary))
        
        entryType.text = "Activity"
        entryType.setTextColor(ContextCompat.getColor(context, HealthColors.Primary.default))
        entryType.setBackgroundColor(ContextCompat.getColor(context, HealthColors.Primary.container))
        
        entryDate.setTextColor(ContextCompat.getColor(context, HealthColors.Text.tertiary))
        entrySummary.visibility = GONE
        
        contentDescription = "Activity entry card showing daily activity summary"
    }

    /**
     * Bind Note entry with Secondary card styling
     */
    private fun bindNoteEntry(entry: JournalEntry.Generic) {
        healthCard.setCardType(HealthCardComponent.HealthCardType.SECONDARY)
        
        entryTitle.text = "Personal Note"
        entryTitle.setTextColor(ContextCompat.getColor(context, HealthColors.Text.primary))
        
        entryDescription.text = entry.content
        entryDescription.setTextColor(ContextCompat.getColor(context, HealthColors.Text.secondary))
        
        entryType.text = "Note"
        entryType.setTextColor(ContextCompat.getColor(context, HealthColors.Secondary.default))
        entryType.setBackgroundColor(ContextCompat.getColor(context, HealthColors.Secondary.container))
        
        entryDate.setTextColor(ContextCompat.getColor(context, HealthColors.Text.tertiary))
        entrySummary.visibility = GONE
        
        contentDescription = "Note entry card with personal note content"
    }

    /**
     * Bind Diary entry with Secondary card styling
     */
    private fun bindDiaryEntry(entry: JournalEntry.Generic) {
        healthCard.setCardType(HealthCardComponent.HealthCardType.SECONDARY)
        
        entryTitle.text = "Diary Entry"
        entryTitle.setTextColor(ContextCompat.getColor(context, HealthColors.Text.primary))
        
        entryDescription.text = entry.content
        entryDescription.setTextColor(ContextCompat.getColor(context, HealthColors.Text.secondary))
        
        entryType.text = "Diary"
        entryType.setTextColor(ContextCompat.getColor(context, HealthColors.Secondary.default))
        entryType.setBackgroundColor(ContextCompat.getColor(context, HealthColors.Secondary.container))
        
        entryDate.setTextColor(ContextCompat.getColor(context, HealthColors.Text.tertiary))
        entrySummary.visibility = GONE
        
        contentDescription = "Diary entry card with personal diary content"
    }

    /**
     * Bind Workout entry with Primary card styling
     */
    private fun bindWorkoutEntry(entry: JournalEntry.Workout) {
        healthCard.setCardType(HealthCardComponent.HealthCardType.PRIMARY)
        
        entryTitle.text = "Workout: ${entry.activityType}"
        entryTitle.setTextColor(ContextCompat.getColor(context, HealthColors.Text.primary))
        
        entryDescription.text = entry.summary
        entryDescription.setTextColor(ContextCompat.getColor(context, HealthColors.Text.secondary))
        
        entryType.text = "Activity"
        entryType.setTextColor(ContextCompat.getColor(context, HealthColors.Primary.default))
        entryType.setBackgroundColor(ContextCompat.getColor(context, HealthColors.Primary.container))
        
        entryDate.setTextColor(ContextCompat.getColor(context, HealthColors.Text.tertiary))
        entrySummary.visibility = GONE
        
        contentDescription = "Workout entry card showing ${entry.activityType} activity"
    }

    /**
     * Bind generic entry with default styling
     */
    private fun bindGenericEntry(entry: JournalEntry.Generic) {
        healthCard.setCardType(HealthCardComponent.HealthCardType.SECONDARY)
        
        entryTitle.text = "Journal Entry"
        entryTitle.setTextColor(ContextCompat.getColor(context, HealthColors.Text.primary))
        
        entryDescription.text = entry.content
        entryDescription.setTextColor(ContextCompat.getColor(context, HealthColors.Text.secondary))
        
        entryType.text = entry.type.uppercase()
        entryType.setTextColor(ContextCompat.getColor(context, HealthColors.Text.tertiary))
        entryType.setBackgroundColor(ContextCompat.getColor(context, HealthColors.Surface.variant))
        
        entryDate.setTextColor(ContextCompat.getColor(context, HealthColors.Text.tertiary))
        entrySummary.visibility = GONE
        
        contentDescription = "Journal entry card with ${entry.type} content"
    }

    /**
     * Bind other entry types with default styling
     */
    private fun bindOtherEntry(entry: JournalEntry) {
        healthCard.setCardType(HealthCardComponent.HealthCardType.SECONDARY)
        
        when (entry) {
            is JournalEntry.Weight -> {
                entryTitle.text = "Weight Measurement"
                entryDescription.text = "${entry.weight} ${entry.unit}"
                if (entry.note.isNotEmpty()) {
                    entrySummary.text = entry.note
                    entrySummary.visibility = VISIBLE
                } else {
                    entrySummary.visibility = GONE
                }
                entryType.text = "Health"
            }
            is JournalEntry.Mood -> {
                entryTitle.text = "Mood: ${entry.emoji}"
                entryDescription.text = entry.description
                if (entry.note.isNotEmpty()) {
                    entrySummary.text = entry.note
                    entrySummary.visibility = VISIBLE
                } else {
                    entrySummary.visibility = GONE
                }
                entryType.text = "Mood"
            }
            is JournalEntry.HeartRate -> {
                entryTitle.text = "Heart Rate"
                entryDescription.text = "${entry.bpm} BPM (${entry.state})"
                if (entry.note.isNotEmpty()) {
                    entrySummary.text = entry.note
                    entrySummary.visibility = VISIBLE
                } else {
                    entrySummary.visibility = GONE
                }
                entryType.text = "Health"
            }
            is JournalEntry.BloodPressure -> {
                entryTitle.text = "Blood Pressure"
                entryDescription.text = "${entry.systolic}/${entry.diastolic} mmHg"
                if (entry.note.isNotEmpty()) {
                    entrySummary.text = entry.note
                    entrySummary.visibility = VISIBLE
                } else {
                    entrySummary.visibility = GONE
                }
                entryType.text = "Health"
            }
            else -> {
                entryTitle.text = "Journal Entry"
                entryDescription.text = "Unknown entry type"
                entryType.text = "OTHER"
                entrySummary.visibility = GONE
            }
        }
        
        // Apply consistent colors for other entry types
        entryTitle.setTextColor(ContextCompat.getColor(context, HealthColors.Text.primary))
        entryDescription.setTextColor(ContextCompat.getColor(context, HealthColors.Text.secondary))
        entryType.setTextColor(ContextCompat.getColor(context, HealthColors.Primary.default))
        entryType.setBackgroundColor(ContextCompat.getColor(context, HealthColors.Primary.container))
        entryDate.setTextColor(ContextCompat.getColor(context, HealthColors.Text.tertiary))
        
        contentDescription = "Journal entry card with health data"
    }

    /**
     * Parse activity data from JSON or legacy format
     * TODO: Extract to ActivityDataParser utility class for better separation of concerns
     */
    private fun parseActivityData(content: String): ActivityData {
        return try {
            // Try JSON format first using Gson
            val gson = com.google.gson.Gson()
            val activityData = gson.fromJson(content, Map::class.java)
            ActivityData(
                steps = (activityData["stepCount"] as? Double)?.toInt() ?: 0,
                calories = (activityData["caloriesBurned"] as? Double)?.toInt() ?: 0,
                heartPoints = (activityData["heartPoints"] as? Double)?.toInt() ?: 0
            )
        } catch (jsonException: Exception) {
            // Fallback to legacy parsing with improved error handling
            parseLegacyActivityData(content)
        }
    }

    /**
     * Parse legacy activity data format
     */
    private fun parseLegacyActivityData(content: String): ActivityData {
        return try {
            if (content.contains("stepCount") && content.contains("\"")) {
                // Handle malformed JSON-like strings
                ActivityData(
                    steps = extractJsonValue(content, "stepCount"),
                    calories = extractJsonValue(content, "caloriesBurned"),
                    heartPoints = extractJsonValue(content, "heartPoints")
                )
            } else {
                // Original colon-separated format
                ActivityData(
                    steps = extractColonValue(content, "steps"),
                    calories = extractColonValue(content, "calories"),
                    heartPoints = extractColonValue(content, "heartPoints")
                )
            }
        } catch (e: Exception) {
            // Return safe defaults if all parsing fails
            ActivityData(0, 0, 0)
        }
    }

    /**
     * Extract value from JSON-like format
     */
    private fun extractJsonValue(content: String, key: String): Int {
        return content.substringAfter("\"$key\":")
            .substringBefore(",")
            .replace("\"", "")
            .replace("}", "")
            .trim()
            .toIntOrNull() ?: 0
    }

    /**
     * Extract value from colon-separated format
     */
    private fun extractColonValue(content: String, key: String): Int {
        return content.substringAfter("$key:")
            .substringBefore(",")
            .trim()
            .toIntOrNull() ?: 0
    }

    /**
     * Data class for parsed activity data
     */
    private data class ActivityData(
        val steps: Int,
        val calories: Int,
        val heartPoints: Int
    )
}