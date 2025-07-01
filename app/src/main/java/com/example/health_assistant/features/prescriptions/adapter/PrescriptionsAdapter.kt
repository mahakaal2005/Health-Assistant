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
 * RecyclerView adapter for displaying prescription items
 * Handles both category headers and prescription cards
 */
class PrescriptionsAdapter(
    private val onPrescriptionClick: (PrescriptionItem.PrescriptionCard) -> Unit,
    private val onPrescriptionEdit: (PrescriptionItem.PrescriptionCard) -> Unit,
    private val onPrescriptionDelete: (PrescriptionItem.PrescriptionCard) -> Unit,
    private val onPrescriptionView: (PrescriptionItem.PrescriptionCard) -> Unit
) : ListAdapter<PrescriptionItem, RecyclerView.ViewHolder>(PrescriptionDiffCallback()) {

    companion object {
        private const val TYPE_CATEGORY_HEADER = 0
        private const val TYPE_PRESCRIPTION_CARD = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is PrescriptionItem.CategoryHeader -> TYPE_CATEGORY_HEADER
            is PrescriptionItem.PrescriptionCard -> TYPE_PRESCRIPTION_CARD
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_CATEGORY_HEADER -> {
                val binding = ItemPrescriptionCategoryHeaderBinding.inflate(inflater, parent, false)
                CategoryHeaderViewHolder(binding)
            }
            TYPE_PRESCRIPTION_CARD -> {
                val binding = ItemPrescriptionCardBinding.inflate(inflater, parent, false)
                PrescriptionCardViewHolder(binding, onPrescriptionClick, onPrescriptionEdit, onPrescriptionDelete, onPrescriptionView)
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
                (holder as PrescriptionCardViewHolder).bind(item)
            }
        }
    }

    class CategoryHeaderViewHolder(
        private val binding: ItemPrescriptionCategoryHeaderBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PrescriptionItem.CategoryHeader) {
            binding.categoryName.text = item.category.displayName
            binding.prescriptionCount.text = "${item.count} prescription${if (item.count != 1) "s" else ""}"
        }
    }

    class PrescriptionCardViewHolder(
        private val binding: ItemPrescriptionCardBinding,
        private val onPrescriptionClick: (PrescriptionItem.PrescriptionCard) -> Unit,
        private val onPrescriptionEdit: (PrescriptionItem.PrescriptionCard) -> Unit,
        private val onPrescriptionDelete: (PrescriptionItem.PrescriptionCard) -> Unit,
        private val onPrescriptionView: (PrescriptionItem.PrescriptionCard) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PrescriptionItem.PrescriptionCard) {
            val prescription = item.prescription

            binding.doctorName.text = prescription.doctorName
            binding.categoryName.text = item.category.displayName
            binding.dateAdded.text = formatDate(prescription.dateAdded)

            // Handle notes visibility
            if (!prescription.notes.isNullOrBlank()) {
                binding.notes.text = prescription.notes
                binding.notes.visibility = android.view.View.VISIBLE
            } else {
                binding.notes.visibility = android.view.View.GONE
            }

            // Click listeners
            binding.root.setOnClickListener {
                onPrescriptionClick(item)
            }

            binding.root.setOnLongClickListener {
                showOptionsMenu(item)
                true
            }
        }

        private fun showOptionsMenu(item: PrescriptionItem.PrescriptionCard) {
            val context = binding.root.context
            val options = arrayOf("View", "Edit", "Delete")

            androidx.appcompat.app.AlertDialog.Builder(context)
                .setTitle("Prescription Options")
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> onPrescriptionView(item)
                        1 -> onPrescriptionEdit(item)
                        2 -> onPrescriptionDelete(item)
                    }
                }
                .show()
        }

        private fun formatDate(timestamp: Long): String {
            val formatter = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
            return formatter.format(java.util.Date(timestamp))
        }
    }
}

class PrescriptionDiffCallback : DiffUtil.ItemCallback<PrescriptionItem>() {
    override fun areItemsTheSame(oldItem: PrescriptionItem, newItem: PrescriptionItem): Boolean {
        return when {
            oldItem is PrescriptionItem.CategoryHeader && newItem is PrescriptionItem.CategoryHeader ->
                oldItem.category.id == newItem.category.id
            oldItem is PrescriptionItem.PrescriptionCard && newItem is PrescriptionItem.PrescriptionCard ->
                oldItem.prescription.id == newItem.prescription.id
            else -> false
        }
    }

    override fun areContentsTheSame(oldItem: PrescriptionItem, newItem: PrescriptionItem): Boolean {
        return oldItem == newItem
    }
}