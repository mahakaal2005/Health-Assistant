package com.example.health_assistant.features.discover.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.health_assistant.databinding.ItemQuickActionBinding
import com.example.health_assistant.features.discover.model.QuickAction

/**
 * Adapter for displaying quick action grid items in the Discover screen
 */
class QuickActionsAdapter(
    private val onActionClick: (QuickAction) -> Unit
) : ListAdapter<QuickAction, QuickActionsAdapter.QuickActionViewHolder>(QuickActionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuickActionViewHolder {
        val binding = ItemQuickActionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return QuickActionViewHolder(binding, onActionClick)
    }

    override fun onBindViewHolder(holder: QuickActionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class QuickActionViewHolder(
        private val binding: ItemQuickActionBinding,
        private val onActionClick: (QuickAction) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(action: QuickAction) {
            binding.apply {
                quickActionTitle.text = action.title

                // Safely set icon resource with error handling
                try {
                    quickActionIcon.setImageResource(action.iconResId)
                } catch (e: Exception) {
                    // If the resource ID is invalid, set a default icon
                    quickActionIcon.setImageResource(android.R.drawable.ic_menu_help)
                }

                // Set click listener on the item
                root.setOnClickListener {
                    onActionClick(action)
                }
            }
        }
    }
}

/**
 * DiffUtil callback for efficient RecyclerView updates
 */
private class QuickActionDiffCallback : DiffUtil.ItemCallback<QuickAction>() {
    override fun areItemsTheSame(oldItem: QuickAction, newItem: QuickAction): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: QuickAction, newItem: QuickAction): Boolean {
        return oldItem == newItem
    }
}