package com.example.health_assistant.features.prescriptions.adapter

import androidx.recyclerview.widget.RecyclerView
import com.example.health_assistant.databinding.ItemPrescriptionCardBinding
import com.example.health_assistant.features.prescriptions.PrescriptionItem
import com.example.health_assistant.features.prescriptions.utils.PrescriptionUtils

/**
 * ViewHolder for prescription card items in prescriptions list
 * Handles prescription display and user interactions
 */
class PrescriptionViewHolder(
    private val binding: ItemPrescriptionCardBinding,
    private val onPrescriptionClick: (String) -> Unit,
    private val onPrescriptionEdit: (String) -> Unit,
    private val onPrescriptionDelete: (String) -> Unit,
    private val onPrescriptionView: (String) -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    private var currentPrescription: PrescriptionItem.PrescriptionCard? = null
    private var isQuickActionsVisible = false

    fun bind(item: PrescriptionItem.PrescriptionCard) {
        currentPrescription = item
        val prescription = item.prescription

        binding.apply {
            // Set doctor name
            doctorName.text = prescription.doctorName

            // Set disease category chip
            diseaseCategoryChip.text = prescription.diseaseCategory.displayName

            // Set formatted date
            dateAdded.text = prescription.getDateAddedDisplay()

            // Load prescription image (placeholder for now, will be replaced with actual image loading)
            prescriptionImage.setImageResource(
                prescription.diseaseCategory.iconRes
                    ?: com.example.health_assistant.R.drawable.ic_prescription_placeholder
            )

            // Set up click listeners
            setupClickListeners(prescription.id)

            // Initially hide quick actions
            quickActionsLayout.visibility = android.view.View.GONE
            isQuickActionsVisible = false
        }
    }

    private fun setupClickListeners(prescriptionId: String) {
        binding.apply {
            // Card click - show prescription details
            root.setOnClickListener {
                onPrescriptionClick(prescriptionId)
            }

            // More actions button - toggle quick actions
            moreActionsButton.setOnClickListener {
                toggleQuickActions()
            }

            // Quick action buttons
            viewButton.setOnClickListener {
                onPrescriptionView(prescriptionId)
                hideQuickActions()
            }

            editButton.setOnClickListener {
                onPrescriptionEdit(prescriptionId)
                hideQuickActions()
            }

            deleteButton.setOnClickListener {
                onPrescriptionDelete(prescriptionId)
                hideQuickActions()
            }
        }
    }

    private fun toggleQuickActions() {
        if (isQuickActionsVisible) {
            hideQuickActions()
        } else {
            showQuickActions()
        }
    }

    private fun showQuickActions() {
        binding.quickActionsLayout.visibility = android.view.View.VISIBLE
        isQuickActionsVisible = true

        // Animate the appearance
        binding.quickActionsLayout.alpha = 0f
        binding.quickActionsLayout.animate()
            .alpha(1f)
            .setDuration(200)
            .start()
    }

    private fun hideQuickActions() {
        binding.quickActionsLayout.animate()
            .alpha(0f)
            .setDuration(200)
            .withEndAction {
                binding.quickActionsLayout.visibility = android.view.View.GONE
                isQuickActionsVisible = false
            }
            .start()
    }
}