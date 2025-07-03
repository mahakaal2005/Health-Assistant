package com.example.health_assistant.features.journal.presentation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.health_assistant.databinding.ItemJournalMoodBinding
import com.example.health_assistant.features.journal.domain.JournalEntry

class MoodAdapter(
    private val onItemClick: (JournalEntry.Mood) -> Unit = {}
) : ListAdapter<JournalEntry.Mood, MoodAdapter.MoodViewHolder>(MoodDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MoodViewHolder {
        val binding = ItemJournalMoodBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MoodViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MoodViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    inner class MoodViewHolder(
        private val binding: ItemJournalMoodBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }
        }

        fun bind(item: JournalEntry.Mood) {
            binding.apply {
                moodEmoji.text = item.emoji
                moodDescription.text = item.description ?: ""
                moodDate.text = android.text.format.DateUtils.getRelativeTimeSpanString(
                    item.timestamp,
                    System.currentTimeMillis(),
                    android.text.format.DateUtils.MINUTE_IN_MILLIS
                )
            }
        }
    }

    private class MoodDiffCallback : DiffUtil.ItemCallback<JournalEntry.Mood>() {
        override fun areItemsTheSame(oldItem: JournalEntry.Mood, newItem: JournalEntry.Mood): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: JournalEntry.Mood, newItem: JournalEntry.Mood): Boolean {
            return oldItem == newItem
        }
    }
}
