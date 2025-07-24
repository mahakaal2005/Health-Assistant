package com.example.health_assistant.features.journal.presentation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.health_assistant.features.journal.domain.JournalEntry
import com.example.health_assistant.features.journal.components.JournalEntryCardComponent

/**
 * Adapter for journal entries in the RecyclerView.
 * Uses JournalEntryCardComponent for consistent styling across all entry types.
 */
class JournalAdapter(
    private val onEdit: (JournalEntry) -> Unit,
    private val onDelete: (JournalEntry) -> Unit
) : ListAdapter<JournalEntry, JournalAdapter.JournalViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JournalViewHolder {
        val cardComponent = JournalEntryCardComponent(parent.context)
        cardComponent.layoutParams = ViewGroup.MarginLayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(
                parent.context.resources.getDimensionPixelSize(com.example.health_assistant.R.dimen.ds_margin_small),
                parent.context.resources.getDimensionPixelSize(com.example.health_assistant.R.dimen.ds_margin_small),
                parent.context.resources.getDimensionPixelSize(com.example.health_assistant.R.dimen.ds_margin_small),
                parent.context.resources.getDimensionPixelSize(com.example.health_assistant.R.dimen.ds_margin_medium)
            )
        }
        return JournalViewHolder(cardComponent)
    }

    override fun onBindViewHolder(holder: JournalViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class JournalViewHolder(private val cardComponent: JournalEntryCardComponent) : RecyclerView.ViewHolder(cardComponent) {
        init {
            // Setup click listeners
            cardComponent.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onEdit(getItem(position))
                }
            }

            // Long click for delete
            cardComponent.setOnLongClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onDelete(getItem(position))
                    true
                } else false
            }
        }

        fun bind(entry: JournalEntry) {
            // Use the standardized card component to bind entry data
            cardComponent.bindEntry(entry)
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<JournalEntry>() {
        override fun areItemsTheSame(oldItem: JournalEntry, newItem: JournalEntry): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: JournalEntry, newItem: JournalEntry): Boolean =
            oldItem == newItem
    }
}