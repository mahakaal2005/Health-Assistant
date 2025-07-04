package com.example.health_assistant.features.journal.presentation

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.health_assistant.R
import com.example.health_assistant.features.journal.domain.JournalEntry
import com.google.android.material.progressindicator.CircularProgressIndicator
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adapter for displaying activity and workout entries in a specialized format
 * Handles workout entries and displays them as activity summaries
 */
class ActivitySummaryAdapter(
    private val onItemClick: (JournalEntry) -> Unit
) : ListAdapter<JournalEntry, ActivitySummaryAdapter.ActivityViewHolder>(ActivityDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_activity_summary_entry, parent, false)
        return ActivityViewHolder(view)
    }

    override fun onBindViewHolder(holder: ActivityViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ActivityViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val activityIcon: ImageView = itemView.findViewById(R.id.activityIcon)
        private val activityTitle: TextView = itemView.findViewById(R.id.activityTitle)
        private val activityDate: TextView = itemView.findViewById(R.id.activityDate)

        private val stepsProgress: CircularProgressIndicator = itemView.findViewById(R.id.stepsProgress)
        private val stepsCount: TextView = itemView.findViewById(R.id.stepsCount)

        private val caloriesProgress: CircularProgressIndicator = itemView.findViewById(R.id.caloriesProgress)
        private val caloriesCount: TextView = itemView.findViewById(R.id.caloriesCount)

        private val activeMinutesProgress: CircularProgressIndicator = itemView.findViewById(R.id.activeMinutesProgress)
        private val activeMinutesCount: TextView = itemView.findViewById(R.id.activeMinutesCount)

        private val activitySummary: TextView = itemView.findViewById(R.id.activitySummary)

        fun bind(entry: JournalEntry) {
            when (entry) {
                is JournalEntry.Workout -> bindWorkout(entry)
                is JournalEntry.Sleep -> bindSleep(entry)
                is JournalEntry.HeartRate -> bindHeartRate(entry)
                else -> bindGenericActivity(entry)
            }

            itemView.setOnClickListener { onItemClick(entry) }
        }

        private fun bindWorkout(entry: JournalEntry.Workout) {
            activityIcon.setImageResource(R.drawable.ic_workout)
            activityTitle.text = entry.activityType
            activityDate.text = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(entry.timestamp))

            // Calculate estimated metrics based on workout duration and type
            val durationMinutes = entry.duration
            val estimatedCalories = calculateCalories(entry.activityType, durationMinutes)
            val estimatedSteps = calculateSteps(entry.activityType, durationMinutes)
            val heartPoints = calculateHeartPoints(entry.activityType, durationMinutes)

            // Set up steps progress (goal: 10,000 steps)
            val stepGoal = 10000
            val stepPercentage = ((estimatedSteps.toFloat() / stepGoal) * 100).coerceAtMost(100f)
            stepsProgress.progress = stepPercentage.toInt()
            stepsCount.text = "$estimatedSteps steps"
            stepsProgress.visibility = View.VISIBLE
            stepsCount.visibility = View.VISIBLE

            // Set up calories progress (goal: 300 calories)
            val calorieGoal = 300
            val caloriePercentage = ((estimatedCalories.toFloat() / calorieGoal) * 100).coerceAtMost(100f)
            caloriesProgress.progress = caloriePercentage.toInt()
            caloriesCount.text = "$estimatedCalories cal"

            // Set up heart points (active minutes) progress (goal: 60 minutes)
            val heartPointGoal = 60
            val heartPointPercentage = ((heartPoints.toFloat() / heartPointGoal) * 100).coerceAtMost(100f)
            activeMinutesProgress.progress = heartPointPercentage.toInt()
            activeMinutesCount.text = "$heartPoints pts"

            activitySummary.text = entry.summary
        }

        // Helper function to calculate calories based on activity type and duration
        private fun calculateCalories(activityType: String, durationMinutes: Int): Int {
            val caloriesPerMinute = when (activityType.lowercase()) {
                "running", "jogging" -> 12 // High intensity
                "cycling", "biking" -> 8
                "walking", "hiking" -> 4
                "swimming" -> 10
                "strength training", "weightlifting" -> 6
                "yoga", "stretching" -> 3
                "dancing" -> 7
                else -> 5 // Default moderate activity
            }
            return durationMinutes * caloriesPerMinute
        }

        // Helper function to calculate steps based on activity type and duration
        private fun calculateSteps(activityType: String, durationMinutes: Int): Int {
            val stepsPerMinute = when (activityType.lowercase()) {
                "running", "jogging" -> 150 // High pace
                "walking" -> 100 // Normal walking pace
                "hiking" -> 120 // Varies with terrain
                "dancing" -> 130
                "cycling", "biking" -> 0 // No steps for cycling
                "swimming" -> 0 // No steps for swimming
                "strength training", "weightlifting" -> 20 // Minimal movement
                "yoga", "stretching" -> 10 // Minimal movement
                else -> 50 // Conservative estimate for other activities
            }
            return durationMinutes * stepsPerMinute
        }

        // Helper function to calculate heart points (active minutes equivalent)
        private fun calculateHeartPoints(activityType: String, durationMinutes: Int): Int {
            val intensityMultiplier = when (activityType.lowercase()) {
                "running", "jogging" -> 2.0 // High intensity = 2x heart points
                "cycling", "biking" -> 1.5
                "swimming" -> 2.0
                "strength training", "weightlifting" -> 1.5
                "dancing" -> 1.5
                "walking" -> 1.0 // Moderate intensity = 1x heart points
                "hiking" -> 1.2
                "yoga", "stretching" -> 0.5 // Low intensity = 0.5x heart points
                else -> 1.0 // Default moderate intensity
            }
            return (durationMinutes * intensityMultiplier).toInt()
        }

        private fun bindSleep(entry: JournalEntry.Sleep) {
            activityIcon.setImageResource(R.drawable.ic_mood) // Using existing icon instead of ic_sleep
            activityTitle.text = "Sleep"
            activityDate.text = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(entry.timestamp))

            val sleepHours = entry.duration / 60.0
            val sleepGoal = 8.0 // 8 hours
            val sleepPercentage = ((sleepHours / sleepGoal) * 100).coerceAtMost(100.0)

            // Use active minutes progress for sleep duration
            activeMinutesProgress.progress = sleepPercentage.toInt()
            activeMinutesCount.text = String.format("%.1fh", sleepHours)

            // Use calories progress for sleep quality
            val qualityPercentage = ((entry.quality.toFloat() / 5) * 100)
            caloriesProgress.progress = qualityPercentage.toInt()
            caloriesCount.text = "${entry.quality}/5"

            // Hide steps for sleep
            stepsProgress.visibility = View.GONE
            stepsCount.visibility = View.GONE

            activitySummary.text = "Sleep quality: ${entry.quality}/5"
        }

        private fun bindHeartRate(entry: JournalEntry.HeartRate) {
            activityIcon.setImageResource(R.drawable.ic_measurement) // Using existing icon instead of ic_heart_rate
            activityTitle.text = "Heart Rate"
            activityDate.text = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(entry.timestamp))

            // Use calories progress for heart rate (normal range 60-100)
            val hrPercentage = when {
                entry.bpm < 60 -> 30f
                entry.bpm > 100 -> 100f
                else -> ((entry.bpm - 60).toFloat() / 40) * 70 + 30 // Scale 60-100 to 30-100%
            }
            caloriesProgress.progress = hrPercentage.toInt()
            caloriesCount.text = "${entry.bpm} BPM"

            // Hide other progress indicators
            stepsProgress.visibility = View.GONE
            stepsCount.visibility = View.GONE
            activeMinutesProgress.visibility = View.GONE
            activeMinutesCount.visibility = View.GONE

            activitySummary.text = "State: ${entry.state ?: "Unknown"}"
        }

        private fun bindGenericActivity(entry: JournalEntry) {
            activityIcon.setImageResource(R.drawable.ic_note)
            activityTitle.text = entry.type.replaceFirstChar { it.uppercase() }
            activityDate.text = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(entry.timestamp))

            // Hide all progress indicators for generic entries
            stepsProgress.visibility = View.GONE
            stepsCount.visibility = View.GONE
            caloriesProgress.visibility = View.GONE
            caloriesCount.visibility = View.GONE
            activeMinutesProgress.visibility = View.GONE
            activeMinutesCount.visibility = View.GONE

            activitySummary.text = when (entry) {
                is JournalEntry.Generic -> entry.content
                is JournalEntry.Weight -> "${entry.weight} ${entry.unit}"
                is JournalEntry.BloodPressure -> "${entry.systolic}/${entry.diastolic} mmHg"
                is JournalEntry.Mood -> entry.description
                else -> "No details available"
            }
        }

        private fun isWalkingActivity(activityType: String): Boolean {
            val walkingActivities = listOf("walking", "jogging", "running", "hiking", "treadmill")
            return walkingActivities.any { activityType.lowercase().contains(it) }
        }
    }

    class ActivityDiffCallback : DiffUtil.ItemCallback<JournalEntry>() {
        override fun areItemsTheSame(oldItem: JournalEntry, newItem: JournalEntry): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: JournalEntry, newItem: JournalEntry): Boolean {
            return oldItem == newItem
        }
    }
}