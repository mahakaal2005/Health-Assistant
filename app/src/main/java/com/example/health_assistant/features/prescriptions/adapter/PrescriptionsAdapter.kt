package com.example.health_assistant.features.prescriptions.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.health_assistant.databinding.ItemPrescriptionCardBinding
import com.example.health_assistant.databinding.ItemPrescriptionCategoryHeaderBinding
import com.example.health_assistant.features.prescriptions.PrescriptionItem

/**
 * RecyclerView adapter for prescriptions with multiple view types
 * Handles both category headers and prescription cards
 */
class PrescriptionsAdapter(
    private val onPrescriptionClick: (String) -> Unit,
    private val onPrescriptionEdit: (String) -> Unit,
    private val onPrescriptionDelete: (String) -> Unit,
    private val onPrescriptionView: (String) -> Unit
) : ListAdapter<PrescriptionItem, RecyclerView.ViewHolder>(PrescriptionDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_CATEGORY_HEADER = 0
        private const val VIEW_TYPE_PRESCRIPTION_CARD = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is PrescriptionItem.CategoryHeader -> VIEW_TYPE_CATEGORY_HEADER
            is PrescriptionItem.PrescriptionCard -> VIEW_TYPE_PRESCRIPTION_CARD
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        return when (viewType) {
            VIEW_TYPE_CATEGORY_HEADER -> {
                val binding = ItemPrescriptionCategoryHeaderBinding.inflate(inflater, parent, false)
                CategoryHeaderViewHolder(binding)
            }
            VIEW_TYPE_PRESCRIPTION_CARD -> {
                val binding = ItemPrescriptionCardBinding.inflate(inflater, parent, false)
                PrescriptionViewHolder(
                    binding = binding,
                    onPrescriptionClick = onPrescriptionClick,
                    onPrescriptionEdit = onPrescriptionEdit,
                    onPrescriptionDelete = onPrescriptionDelete,
                    onPrescriptionView = onPrescriptionView
                )
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is PrescriptionItem.CategoryHeader -> {
                (holder as CategoryHeaderViewHolder).bind(item)
            }
            is PrescriptionItem.PrescriptionCard -> {
                (holder as PrescriptionViewHolder).bind(item)
            }
        }
    }
}

/**
 * DiffUtil callback for efficient list updates
 */
class PrescriptionDiffCallback : DiffUtil.ItemCallback<PrescriptionItem>() {

    override fun areItemsTheSame(oldItem: PrescriptionItem, newItem: PrescriptionItem): Boolean {
        return when {
            oldItem is PrescriptionItem.CategoryHeader && newItem is PrescriptionItem.CategoryHeader -> {
                oldItem.category.id == newItem.category.id
            }
            oldItem is PrescriptionItem.PrescriptionCard && newItem is PrescriptionItem.PrescriptionCard -> {
                oldItem.prescription.id == newItem.prescription.id
            }
            else -> false
        }
    }

    override fun areContentsTheSame(oldItem: PrescriptionItem, newItem: PrescriptionItem): Boolean {
        return oldItem == newItem
    }
}