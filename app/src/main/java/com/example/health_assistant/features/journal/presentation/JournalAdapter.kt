package com.example.health_assistant.features.journal.presentation

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.health_assistant.R
import com.example.health_assistant.features.journal.data.JournalEntryEntity
import com.example.health_assistant.databinding.ItemJournalEntryBinding
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adapter for journal entries in the RecyclerView.
 * Handles the rendering of different entry types.
 */
class JournalAdapter(
    private val onEdit: (JournalEntryEntity) -> Unit,
    private val onDelete: (JournalEntryEntity) -> Unit
) : ListAdapter<JournalEntryEntity, JournalAdapter.JournalViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JournalViewHolder {
        val binding = ItemJournalEntryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return JournalViewHolder(binding)
    }

    override fun onBindViewHolder(holder: JournalViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class JournalViewHolder(private val binding: ItemJournalEntryBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            // Setup click listeners
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onEdit(getItem(position))
                }
            }

            // Long click for delete instead of using a non-existent deleteButton
            binding.root.setOnLongClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onDelete(getItem(position))
                    true
                } else false
            }
        }

        fun bind(entry: JournalEntryEntity) {
            val context = binding.root.context

            // Format date with locale
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            binding.entryDate.text = dateFormat.format(Date(entry.timestamp))

            when (entry.type) {
                "workout" -> {
                    binding.entryIcon.setImageResource(R.drawable.ic_workout)
                    binding.entryIcon.setColorFilter(context.getColor(R.color.colorWorkout))
                    binding.entryTitle.text = context.getString(R.string.journal_entry_workout_title, entry.activityType)
                    binding.entrySummary.text = entry.summary
                    binding.entrySummary.visibility = View.VISIBLE
                }
                "note" -> {
                    binding.entryIcon.setImageResource(R.drawable.ic_note)
                    binding.entryIcon.setColorFilter(context.getColor(R.color.colorNote))
                    binding.entryTitle.text = context.getString(R.string.journal_entry_note_title)
                    binding.entrySummary.text = entry.content
                    binding.entrySummary.visibility = View.VISIBLE
                }
                "goal" -> {
                    binding.entryIcon.setImageResource(R.drawable.ic_goal)
                    binding.entryIcon.setColorFilter(context.getColor(R.color.colorGoal))
                    binding.entryTitle.text = context.getString(R.string.journal_entry_goal_title, entry.goalTitle)
                    binding.entrySummary.text = context.getString(R.string.journal_entry_goal_progress, entry.progress)
                    binding.entrySummary.visibility = View.VISIBLE
                }
                "measurement" -> {
                    binding.entryIcon.setImageResource(R.drawable.ic_measurement)
                    binding.entryIcon.setColorFilter(context.getColor(R.color.colorMeasurement))
                    binding.entryTitle.text = entry.measurementType
                    binding.entrySummary.text = context.getString(R.string.journal_entry_measurement_summary, entry.value, entry.unit)
                    binding.entrySummary.visibility = View.VISIBLE
                }
                "weight" -> {
                    binding.entryIcon.setImageResource(R.drawable.ic_weight)
                    binding.entryIcon.setColorFilter(context.getColor(R.color.progress_green))
                    binding.entryTitle.text = "Weight"
                    val changeText = if (entry.previousValue != null) {
                        val change = entry.value?.minus(entry.previousValue!!) ?: 0f
                        val changeSymbol = if (change >= 0) "+" else ""
                        " ($changeSymbol${String.format("%.1f", change)} ${entry.unit})"
                    } else ""
                    binding.entrySummary.text = "${entry.value} ${entry.unit}$changeText"
                    binding.entrySummary.visibility = View.VISIBLE
                }
                "heart_rate" -> {
                    binding.entryIcon.setImageResource(R.drawable.ic_heart_rate)
                    binding.entryIcon.setColorFilter(context.getColor(R.color.colorError))
                    binding.entryTitle.text = "Heart Rate"
                    val stateText = entry.state?.let { " ($it)" } ?: ""
                    binding.entrySummary.text = "${entry.value?.toInt()} BPM$stateText"
                    binding.entrySummary.visibility = View.VISIBLE
                }
                "blood_pressure" -> {
                    binding.entryIcon.setImageResource(R.drawable.ic_blood_pressure)
                    binding.entryIcon.setColorFilter(context.getColor(R.color.colorError))
                    binding.entryTitle.text = "Blood Pressure"
                    binding.entrySummary.text = "${entry.systolic}/${entry.diastolic} mmHg"
                    binding.entrySummary.visibility = View.VISIBLE
                }
                "activity_summary" -> {
                    binding.entryIcon.setImageResource(R.drawable.ic_activity)
                    binding.entryIcon.setColorFilter(context.getColor(R.color.progress_blue))
                    binding.entryTitle.text = "Activity Summary"
                    binding.entrySummary.text = "${entry.steps} steps, ${entry.activeMinutes} active min, ${entry.calories} cal"
                    binding.entrySummary.visibility = View.VISIBLE
                }
                "mood" -> {
                    binding.entryIcon.setImageResource(R.drawable.ic_mood)
                    binding.entryIcon.setColorFilter(context.getColor(R.color.progress_orange))
                    binding.entryTitle.text = "Mood: ${entry.emoji}"
                    binding.entrySummary.text = entry.description ?: getMoodDescription(entry.moodLevel ?: 3)
                    binding.entrySummary.visibility = View.VISIBLE
                }
                else -> {
                    binding.entryIcon.setImageResource(R.drawable.ic_note)
                    binding.entryTitle.text = entry.type.replaceFirstChar { it.uppercase() }
                    binding.entrySummary.text = entry.content ?: ""
                    binding.entrySummary.visibility = if (entry.content.isNullOrBlank()) View.GONE else View.VISIBLE
                }
            }

            // Set type label for accessibility
            binding.entryType.text = entry.type.replaceFirstChar { it.uppercase() }

            // Accessibility
            binding.root.contentDescription = "${binding.entryTitle.text}, ${entry.type}, ${binding.entryDate.text}"
        }

        /**
         * Returns a descriptive text based on mood level
         */
        private fun getMoodDescription(moodLevel: Int): String {
            return when (moodLevel) {
                5 -> "Feeling great today!"
                4 -> "Feeling good"
                3 -> "Feeling okay"
                2 -> "Not feeling well"
                1 -> "Feeling terrible"
                else -> "Neutral mood"
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<JournalEntryEntity>() {
        override fun areItemsTheSame(oldItem: JournalEntryEntity, newItem: JournalEntryEntity): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: JournalEntryEntity, newItem: JournalEntryEntity): Boolean =
            oldItem == newItem
    }
}
