package com.example.health_assistant.features.prescriptions.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.example.health_assistant.databinding.ItemPrescriptionCardBinding
import com.example.health_assistant.data.model.Prescription

/**
 * Simplified RecyclerView adapter for displaying prescriptions in a grid layout
 * No category headers - just individual prescription cards
 */
class PrescriptionsAdapter(
    private val onPrescriptionClick: (Prescription) -> Unit
) : ListAdapter<Prescription, PrescriptionViewHolder>(PrescriptionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PrescriptionViewHolder {
        val binding = ItemPrescriptionCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PrescriptionViewHolder(binding, onPrescriptionClick)
    }

    override fun onBindViewHolder(holder: PrescriptionViewHolder, position: Int) {
        val prescription = getItem(position)
        holder.bind(prescription)
    }
}

/**
 * DiffUtil callback for efficient list updates
 */
class PrescriptionDiffCallback : DiffUtil.ItemCallback<Prescription>() {
    override fun areItemsTheSame(oldItem: Prescription, newItem: Prescription): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Prescription, newItem: Prescription): Boolean {
        return oldItem == newItem
    }
}