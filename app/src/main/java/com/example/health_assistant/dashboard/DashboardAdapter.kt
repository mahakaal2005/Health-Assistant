package com.example.health_assistant.dashboard

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.health_assistant.databinding.ItemDashboardCardBinding
import com.google.android.material.chip.Chip

/**
 * Adapter for displaying dashboard sections in a RecyclerView
 */
class DashboardAdapter(
    private val sections: List<DashboardSection>,
    private val onActionClicked: (DashboardSection, DashboardAction) -> Unit
) : RecyclerView.Adapter<DashboardAdapter.DashboardViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DashboardViewHolder {
        val binding = ItemDashboardCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DashboardViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DashboardViewHolder, position: Int) {
        holder.bind(sections[position])
    }

    override fun getItemCount(): Int = sections.size

    inner class DashboardViewHolder(
        private val binding: ItemDashboardCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(section: DashboardSection) {
            val context = binding.root.context

            // Set section data
            binding.featureTitle.text = section.title
            binding.featureDescription.text = section.description

            // Set icon and accent color
            binding.featureIcon.setImageResource(section.iconRes)
            val accentColor = ContextCompat.getColor(context, section.accentColorRes)
            binding.featureIcon.imageTintList = ColorStateList.valueOf(accentColor)
            binding.featureAccentIndicator.setBackgroundColor(accentColor)

            // Clear and add action chips
            binding.actionChips.removeAllViews()
            section.actions.forEach { action ->
                val chip = createActionChip(action, section, accentColor)
                binding.actionChips.addView(chip)
            }
        }

        private fun createActionChip(
            action: DashboardAction,
            section: DashboardSection,
            accentColor: Int
        ): Chip {
            return Chip(binding.root.context).apply {
                text = action.label
                chipStrokeWidth = 1f
                chipStrokeColor = ColorStateList.valueOf(accentColor)
                setTextColor(ContextCompat.getColor(context, com.example.health_assistant.R.color.text_primary))
                chipBackgroundColor = ColorStateList.valueOf(
                    ContextCompat.getColor(context, com.example.health_assistant.R.color.card_background_alt)
                )

                // Set click listener
                setOnClickListener {
                    onActionClicked(section, action)
                }
            }
        }
    }
}