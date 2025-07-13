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

            // Ensure all remaining views are visible and properly bound
            binding.entryTitle.visibility = View.VISIBLE
            binding.entryDescription.visibility = View.VISIBLE
            binding.entryType.visibility = View.VISIBLE

            when (entry) {
                is JournalEntry.Workout -> {
                    // Activity/Workout styling
                    binding.cardView.setCardBackgroundColor(context.getColor(android.R.color.white))
                    binding.entryTitle.text = "Workout: ${entry.activityType}"
                    binding.entryTitle.setTextColor(context.getColor(android.R.color.black))
                    binding.entryDescription.text = entry.summary
                    binding.entryDescription.setTextColor(context.getColor(android.R.color.darker_gray))
                    binding.entryType.text = "Activity"
                    binding.entryType.setTextColor(context.getColor(android.R.color.holo_orange_dark))
                    binding.entryType.setBackgroundColor(context.getColor(android.R.color.holo_orange_light))
                    binding.entryDate.setTextColor(context.getColor(android.R.color.holo_orange_dark))
                    binding.entrySummary.visibility = View.GONE
                }
                is JournalEntry.Generic -> {
                    when (entry.type.lowercase()) {
                        "diary" -> {
                            // Diary Entry styling - Beige background (#F5EFE6)
                            binding.cardView.setCardBackgroundColor(android.graphics.Color.parseColor("#F5EFE6"))
                            binding.entryTitle.text = "Diary Entry"
                            binding.entryTitle.setTextColor(android.graphics.Color.parseColor("#3E2723"))
                            binding.entryDescription.text = entry.content
                            binding.entryDescription.setTextColor(android.graphics.Color.parseColor("#5D4037"))
                            binding.entryType.text = "Diary"
                            binding.entryType.setTextColor(android.graphics.Color.parseColor("#8D6E63"))
                            binding.entryType.setBackgroundColor(android.graphics.Color.parseColor("#FFDDAA"))
                            binding.entryDate.setTextColor(android.graphics.Color.parseColor("#8D6E63"))
                        }
                        "note" -> {
                            // Personal Note styling - Light purple background (#E6E0F8)
                            binding.cardView.setCardBackgroundColor(android.graphics.Color.parseColor("#E6E0F8"))
                            binding.entryTitle.text = "Personal Note"
                            binding.entryTitle.setTextColor(android.graphics.Color.parseColor("#4A148C"))
                            binding.entryDescription.text = entry.content
                            binding.entryDescription.setTextColor(android.graphics.Color.parseColor("#6A1B9A"))
                            binding.entryType.text = "Notes"
                            binding.entryType.setTextColor(android.graphics.Color.parseColor("#7B1FA2"))
                            binding.entryType.setBackgroundColor(android.graphics.Color.parseColor("#D1C4E9"))
                            binding.entryDate.setTextColor(android.graphics.Color.parseColor("#7B1FA2"))
                        }
                        else -> {
                            // Fallback for other generic types - default white background
                            binding.cardView.setCardBackgroundColor(context.getColor(android.R.color.white))
                            binding.entryTitle.text = "Journal Entry"
                            binding.entryTitle.setTextColor(context.getColor(android.R.color.black))
                            binding.entryDescription.text = entry.content
                            binding.entryDescription.setTextColor(context.getColor(android.R.color.darker_gray))
                            binding.entryType.text = entry.type.uppercase()
                            binding.entryType.setTextColor(context.getColor(android.R.color.holo_blue_dark))
                            binding.entryType.setBackgroundColor(context.getColor(android.R.color.holo_blue_light))
                            binding.entryDate.setTextColor(context.getColor(android.R.color.holo_blue_dark))
                        }
                    }
                    binding.entrySummary.visibility = View.GONE
                }
                is JournalEntry.Weight -> {
                    // Health measurement styling
                    binding.cardView.setCardBackgroundColor(context.getColor(android.R.color.white))
                    binding.entryTitle.text = "Weight Measurement"
                    binding.entryTitle.setTextColor(context.getColor(android.R.color.black))
                    binding.entryDescription.text = "${entry.weight} ${entry.unit}"
                    binding.entryDescription.setTextColor(context.getColor(android.R.color.darker_gray))
                    binding.entryType.text = "Health"
                    binding.entryType.setTextColor(context.getColor(android.R.color.holo_green_dark))
                    binding.entryType.setBackgroundColor(context.getColor(android.R.color.holo_green_light))
                    binding.entryDate.setTextColor(context.getColor(android.R.color.holo_green_dark))
                    if (entry.note.isNotEmpty()) {
                        binding.entrySummary.text = entry.note
                        binding.entrySummary.setTextColor(context.getColor(android.R.color.darker_gray))
                        binding.entrySummary.visibility = View.VISIBLE
                    } else {
                        binding.entrySummary.visibility = View.GONE
                    }
                }
                is JournalEntry.Mood -> {
                    // Mood entry styling
                    binding.cardView.setCardBackgroundColor(context.getColor(android.R.color.white))
                    binding.entryTitle.text = "Mood: ${entry.emoji}"
                    binding.entryTitle.setTextColor(context.getColor(android.R.color.black))
                    binding.entryDescription.text = entry.description
                    binding.entryDescription.setTextColor(context.getColor(android.R.color.darker_gray))
                    binding.entryType.text = "Mood"
                    binding.entryType.setTextColor(context.getColor(android.R.color.holo_blue_bright))
                    binding.entryType.setBackgroundColor(context.getColor(android.R.color.holo_blue_light))
                    binding.entryDate.setTextColor(context.getColor(android.R.color.holo_blue_bright))
                    if (entry.note.isNotEmpty()) {
                        binding.entrySummary.text = entry.note
                        binding.entrySummary.setTextColor(context.getColor(android.R.color.darker_gray))
                        binding.entrySummary.visibility = View.VISIBLE
                    } else {
                        binding.entrySummary.visibility = View.GONE
                    }
                }
                is JournalEntry.HeartRate -> {
                    // Heart rate styling
                    binding.cardView.setCardBackgroundColor(context.getColor(android.R.color.white))
                    binding.entryTitle.text = "Heart Rate"
                    binding.entryTitle.setTextColor(context.getColor(android.R.color.black))
                    binding.entryDescription.text = "${entry.bpm} BPM (${entry.state})"
                    binding.entryDescription.setTextColor(context.getColor(android.R.color.darker_gray))
                    binding.entryType.text = "Health"
                    binding.entryType.setTextColor(context.getColor(android.R.color.holo_red_dark))
                    binding.entryType.setBackgroundColor(context.getColor(android.R.color.holo_red_light))
                    binding.entryDate.setTextColor(context.getColor(android.R.color.holo_red_light))
                    if (entry.note.isNotEmpty()) {
                        binding.entrySummary.text = entry.note
                        binding.entrySummary.setTextColor(context.getColor(android.R.color.darker_gray))
                        binding.entrySummary.visibility = View.VISIBLE
                    } else {
                        binding.entrySummary.visibility = View.GONE
                    }
                }
                is JournalEntry.BloodPressure -> {
                    // Blood pressure styling
                    binding.cardView.setCardBackgroundColor(context.getColor(android.R.color.white))
                    binding.entryTitle.text = "Blood Pressure"
                    binding.entryTitle.setTextColor(context.getColor(android.R.color.black))
                    binding.entryDescription.text = "${entry.systolic}/${entry.diastolic} mmHg"
                    binding.entryDescription.setTextColor(context.getColor(android.R.color.darker_gray))
                    binding.entryType.text = "Health"
                    binding.entryType.setTextColor(context.getColor(android.R.color.holo_red_light))
                    binding.entryType.setBackgroundColor(context.getColor(android.R.color.holo_red_light))
                    binding.entryDate.setTextColor(context.getColor(android.R.color.holo_red_light))
                    if (entry.note.isNotEmpty()) {
                        binding.entrySummary.text = entry.note
                        binding.entrySummary.setTextColor(context.getColor(android.R.color.darker_gray))
                        binding.entrySummary.visibility = View.VISIBLE
                    } else {
                        binding.entrySummary.visibility = View.GONE
                    }
                }
                else -> {
                    // Fallback case for unknown entry types
                    binding.cardView.setCardBackgroundColor(context.getColor(android.R.color.white))
                    binding.entryTitle.text = "Journal Entry"
                    binding.entryTitle.setTextColor(context.getColor(android.R.color.black))
                    binding.entryDescription.text = "Unknown entry type"
                    binding.entryDescription.setTextColor(context.getColor(android.R.color.darker_gray))
                    binding.entryType.text = "OTHER"
                    binding.entryType.setTextColor(context.getColor(android.R.color.darker_gray))
                    binding.entryType.setBackgroundColor(context.getColor(android.R.color.background_light))
                    binding.entryDate.setTextColor(context.getColor(android.R.color.darker_gray))
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