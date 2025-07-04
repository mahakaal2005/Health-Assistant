package com.example.health_assistant.features.journal.presentation

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.health_assistant.R
import com.example.health_assistant.features.journal.domain.JournalEntry
import com.example.health_assistant.databinding.ItemJournalEntryBinding
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adapter for journal entries in the RecyclerView.
 * Handles the rendering of different entry types.
 */
class JournalAdapter(
    private val onEdit: (JournalEntry) -> Unit,
    private val onDelete: (JournalEntry) -> Unit
) : ListAdapter<JournalEntry, JournalAdapter.JournalViewHolder>(DiffCallback) {

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

        fun bind(entry: JournalEntry) {
            val context = binding.root.context

            // Format date with locale
            val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            binding.entryDate.text = dateFormat.format(Date(entry.timestamp))

            // CRITICAL FIX: Ensure all views are visible and properly bound
            binding.entryTitle.visibility = View.VISIBLE
            binding.entryDescription.visibility = View.VISIBLE
            binding.entryType.visibility = View.VISIBLE
            binding.entryIcon.visibility = View.VISIBLE

            when (entry) {
                is JournalEntry.Workout -> {
                    // Use built-in Android icons as fallback
                    binding.entryIcon.setImageResource(android.R.drawable.ic_menu_mylocation)
                    binding.entryIcon.setColorFilter(context.getColor(android.R.color.holo_orange_dark))
                    binding.entryTitle.text = "Workout: ${entry.activityType}"
                    binding.entryDescription.text = entry.summary
                    binding.entryType.text = "ACTIVITY"
                    binding.entrySummary.visibility = View.GONE
                }
                is JournalEntry.Generic -> {
                    // CRITICAL FIX: Use built-in Android icon for notes
                    binding.entryIcon.setImageResource(android.R.drawable.ic_menu_edit)
                    binding.entryIcon.setColorFilter(context.getColor(android.R.color.holo_blue_dark))
                    binding.entryTitle.text = "Personal Note"
                    binding.entryDescription.text = entry.content
                    binding.entryType.text = "NOTE"
                    binding.entrySummary.visibility = View.GONE
                }
                is JournalEntry.Weight -> {
                    binding.entryIcon.setImageResource(android.R.drawable.ic_menu_compass)
                    binding.entryIcon.setColorFilter(context.getColor(android.R.color.holo_green_dark))
                    binding.entryTitle.text = "Weight Measurement"
                    binding.entryDescription.text = "${entry.weight} ${entry.unit}"
                    binding.entryType.text = "HEALTH"
                    if (entry.note.isNotEmpty()) {
                        binding.entrySummary.text = entry.note
                        binding.entrySummary.visibility = View.VISIBLE
                    } else {
                        binding.entrySummary.visibility = View.GONE
                    }
                }
                is JournalEntry.Mood -> {
                    binding.entryIcon.setImageResource(android.R.drawable.ic_menu_preferences)
                    binding.entryIcon.setColorFilter(context.getColor(android.R.color.holo_blue_bright))
                    binding.entryTitle.text = "Mood: ${entry.emoji}"
                    binding.entryDescription.text = entry.description
                    binding.entryType.text = "MOOD"
                    if (entry.note.isNotEmpty()) {
                        binding.entrySummary.text = entry.note
                        binding.entrySummary.visibility = View.VISIBLE
                    } else {
                        binding.entrySummary.visibility = View.GONE
                    }
                }
                is JournalEntry.HeartRate -> {
                    binding.entryIcon.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
                    binding.entryIcon.setColorFilter(context.getColor(android.R.color.holo_red_dark))
                    binding.entryTitle.text = "Heart Rate"
                    binding.entryDescription.text = "${entry.bpm} BPM (${entry.state})"
                    binding.entryType.text = "HEALTH"
                    if (entry.note.isNotEmpty()) {
                        binding.entrySummary.text = entry.note
                        binding.entrySummary.visibility = View.VISIBLE
                    } else {
                        binding.entrySummary.visibility = View.GONE
                    }
                }
                is JournalEntry.BloodPressure -> {
                    binding.entryIcon.setImageResource(android.R.drawable.ic_menu_info_details)
                    binding.entryIcon.setColorFilter(context.getColor(android.R.color.holo_red_light))
                    binding.entryTitle.text = "Blood Pressure"
                    binding.entryDescription.text = "${entry.systolic}/${entry.diastolic} mmHg"
                    binding.entryType.text = "HEALTH"
                    if (entry.note.isNotEmpty()) {
                        binding.entrySummary.text = entry.note
                        binding.entrySummary.visibility = View.VISIBLE
                    } else {
                        binding.entrySummary.visibility = View.GONE
                    }
                }
                else -> {
                    // CRITICAL FIX: Add fallback case for unknown entry types
                    binding.entryIcon.setImageResource(android.R.drawable.ic_menu_agenda)
                    binding.entryIcon.setColorFilter(context.getColor(android.R.color.darker_gray))
                    binding.entryTitle.text = "Journal Entry"
                    binding.entryDescription.text = "Unknown entry type"
                    binding.entryType.text = "OTHER"
                    binding.entrySummary.visibility = View.GONE
                }
            }

            // Debug logging to verify data binding
            android.util.Log.d("JournalAdapter", "Binding entry: ${entry.javaClass.simpleName}")
            android.util.Log.d("JournalAdapter", "Title: ${binding.entryTitle.text}")
            android.util.Log.d("JournalAdapter", "Description: ${binding.entryDescription.text}")
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

    companion object DiffCallback : DiffUtil.ItemCallback<JournalEntry>() {
        override fun areItemsTheSame(oldItem: JournalEntry, newItem: JournalEntry): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: JournalEntry, newItem: JournalEntry): Boolean =
            oldItem == newItem
    }
}