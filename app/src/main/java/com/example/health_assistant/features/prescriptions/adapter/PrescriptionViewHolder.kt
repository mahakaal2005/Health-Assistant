package com.example.health_assistant.features.prescriptions.adapter

import androidx.recyclerview.widget.RecyclerView
import coil3.load
import coil3.request.crossfade
import coil3.request.error
import coil3.request.placeholder
import com.example.health_assistant.R
import com.example.health_assistant.databinding.ItemPrescriptionCardBinding
import com.example.health_assistant.data.model.Prescription
import com.example.health_assistant.features.prescriptions.utils.PrescriptionUtils

/**
 * ViewHolder for prescription card items in grid layout
 * Handles prescription display with background image and overlay content
 */
class PrescriptionViewHolder(
    private val binding: ItemPrescriptionCardBinding,
    private val onPrescriptionClick: (Prescription) -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(prescription: Prescription) {
        binding.apply {
            // Load prescription image as background
            backgroundImage.load(prescription.localImagePath) {
                placeholder(R.drawable.ic_prescription_placeholder)
                error(R.drawable.ic_prescription_placeholder)
                crossfade(true)
            }

            // Set category chip - always use categoryId to look up the category name
            val category = PrescriptionUtils.getCategoryById(prescription.categoryId)
            categoryChip.text = category?.name ?: "General"

            // Set doctor name with Dr. prefix
            val doctorNameText = prescription.doctorName ?: "Unknown Doctor"
            doctorName.text = if (doctorNameText.startsWith("Dr.")) {
                doctorNameText
            } else {
                "Dr. $doctorNameText"
            }

            // Set date
            dateAdded.text = PrescriptionUtils.formatDate(prescription.dateAdded.time)


            // Set up click listener for the entire card
            root.setOnClickListener {
                onPrescriptionClick(prescription)
            }
        }
    }
}