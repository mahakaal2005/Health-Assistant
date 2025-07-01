package com.example.health_assistant.features.prescriptions.adapter

import androidx.recyclerview.widget.RecyclerView
import com.example.health_assistant.databinding.ItemPrescriptionCategoryHeaderBinding
import com.example.health_assistant.features.prescriptions.PrescriptionItem

/**
 * ViewHolder for prescription category header items
 * Displays category name, icon, and prescription count
 */
class CategoryHeaderViewHolder(
    private val binding: ItemPrescriptionCategoryHeaderBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: PrescriptionItem.CategoryHeader) {
        binding.apply {
            // Set category name
            categoryName.text = item.category.displayName

            // Set category icon if available
            item.category.iconRes?.let { iconRes ->
                categoryIcon.setImageResource(iconRes)
                categoryIcon.visibility = android.view.View.VISIBLE
            } ?: run {
                categoryIcon.visibility = android.view.View.GONE
            }

            // Fixed: Use correct property name 'count' instead of 'prescriptionCount'
            prescriptionCount.text = item.count.toString()
        }
    }
}