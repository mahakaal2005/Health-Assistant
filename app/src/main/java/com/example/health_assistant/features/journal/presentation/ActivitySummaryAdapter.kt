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
 * Adapter for displaying activity summary entries with circular progress indicators
 */
class ActivitySummaryAdapter(
    private val onItemClick: (JournalEntry) -> Unit = {}
) : ListAdapter<JournalEntry, ActivitySummaryAdapter.ActivityViewHolder>(ActivityDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_activity_summary_entry, parent, false)
        return ActivityViewHolder(view)
    }

    override fun onBindViewHolder(holder: ActivityViewHolder, position: Int) {
        val entry = getItem(position)
        holder.bind(entry)
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
                is JournalEntry.ActivitySummary -> bindActivitySummary(entry)
                is JournalEntry.Workout -> bindWorkout(entry)
                else -> bindGenericActivity(entry)
            }

            itemView.setOnClickListener { onItemClick(entry) }
        }

        private fun bindActivitySummary(entry: JournalEntry.ActivitySummary) {
            activityIcon.setImageResource(R.drawable.ic_activity)
            activityTitle.text = "Daily Activity"
            activityDate.text = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(entry.timestamp))

            // Set up steps progress
            val stepsPercentage = if ((entry.stepGoal ?: 0) > 0)
                ((entry.steps ?: 0) * 100 / (entry.stepGoal ?: 1)).coerceIn(0, 100) else 0
            stepsProgress.progress = stepsPercentage
            stepsCount.text = (entry.steps ?: 0).toString()

            // Set up calories progress
            val caloriesPercentage = if ((entry.caloriesGoal ?: 0) > 0)
                ((entry.caloriesBurned ?: 0) * 100 / (entry.caloriesGoal ?: 1)).coerceIn(0, 100) else 0
            caloriesProgress.progress = caloriesPercentage
            caloriesCount.text = (entry.caloriesBurned ?: 0).toString()

            // Set up active minutes progress
            val minutesPercentage = if ((entry.activeMinutesGoal ?: 0) > 0)
                ((entry.activeMinutes ?: 0) * 100 / (entry.activeMinutesGoal ?: 1)).coerceIn(0, 100) else 0
            activeMinutesProgress.progress = minutesPercentage
            activeMinutesCount.text = (entry.activeMinutes ?: 0).toString()

            // Set summary text
            val stepsRemaining = (entry.stepGoal ?: 0) - (entry.steps ?: 0)
            activitySummary.text = if (stepsRemaining > 0) {
                "You're $stepsRemaining steps away from your daily goal!"
            } else {
                "Congratulations! You've reached your step goal for today!"
            }
        }

        private fun bindWorkout(entry: JournalEntry.Workout) {
            activityIcon.setImageResource(R.drawable.ic_workout)
            activityTitle.text = "${entry.activityType} Workout"
            activityDate.text = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(entry.timestamp))

            // Extract calories from summary if available
            val caloriesPattern = "Burned (\\d+) calories".toRegex()
            val caloriesMatch = entry.summary?.let { caloriesPattern.find(it) }
            val calories = caloriesMatch?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0

            // For workouts, we don't have goals, so show fixed values
            stepsProgress.progress = 0
            stepsCount.text = "-"

            caloriesProgress.progress = 100
            caloriesCount.text = calories.toString()

            activeMinutesProgress.progress = 100
            activeMinutesCount.text = entry.duration.toString()

            activitySummary.text = entry.summary ?: "${entry.activityType} for ${entry.duration} minutes"
        }

        private fun bindGenericActivity(entry: JournalEntry) {
            activityIcon.setImageResource(R.drawable.ic_activity)
            activityTitle.text = "Activity"
            activityDate.text = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(entry.timestamp))

            // Hide or reset progress indicators
            stepsProgress.progress = 0
            stepsCount.text = "-"

            caloriesProgress.progress = 0
            caloriesCount.text = "-"

            activeMinutesProgress.progress = 0
            activeMinutesCount.text = "-"

            activitySummary.text = "Activity details not available"
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
