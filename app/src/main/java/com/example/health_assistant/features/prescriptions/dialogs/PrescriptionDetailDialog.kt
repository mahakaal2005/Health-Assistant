package com.example.health_assistant.features.prescriptions.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import coil3.load
import coil3.request.crossfade
import coil3.request.error
import coil3.request.placeholder
import com.example.health_assistant.R
import com.example.health_assistant.databinding.DialogPrescriptionDetailBinding
import com.example.health_assistant.features.prescriptions.PrescriptionsViewModel
import com.example.health_assistant.features.prescriptions.utils.PrescriptionUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Full-screen dialog for viewing prescription details with zoomable image
 */
@AndroidEntryPoint
class PrescriptionDetailDialog : DialogFragment() {

    private var _binding: DialogPrescriptionDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PrescriptionsViewModel by viewModels()
    private lateinit var prescriptionId: String

    companion object {
        private const val ARG_PRESCRIPTION_ID = "prescription_id"

        fun newInstance(prescriptionId: String): PrescriptionDetailDialog {
            return PrescriptionDetailDialog().apply {
                arguments = bundleOf(ARG_PRESCRIPTION_ID to prescriptionId)
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)

        // Make dialog full screen
        dialog.window?.apply {
            setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }

        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogPrescriptionDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prescriptionId = arguments?.getString(ARG_PRESCRIPTION_ID)
            ?: throw IllegalArgumentException("Prescription ID is required")

        setupToolbar()
        observeViewModel()
        loadPrescriptionDetails()
    }

    private fun setupToolbar() {
        // Setup close button (replaces navigation click)
        binding.closeButton.setOnClickListener {
            dismiss()
        }

        // Setup edit button
        binding.editButton.setOnClickListener {
            openEditDialog()
        }

        // Setup delete button if it exists in the layout
        // Note: We'll need to add a delete button to the layout or handle it differently
        // For now, we can add a long press listener to the edit button for delete functionality
        binding.editButton.setOnLongClickListener {
            showDeleteConfirmation()
            true
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.prescriptions.collect { prescriptionItems ->
                // Extract prescription from PrescriptionItem.PrescriptionCard
                val prescription = prescriptionItems
                    .filterIsInstance<com.example.health_assistant.features.prescriptions.PrescriptionItem.PrescriptionCard>()
                    .map { it.prescription }
                    .find { it.id == prescriptionId }

                prescription?.let { displayPrescriptionDetails(it) }
            }
        }
    }

    private fun loadPrescriptionDetails() {
        // Details will be loaded through the observed prescriptions flow
    }

    private fun displayPrescriptionDetails(prescription: com.example.health_assistant.data.model.Prescription) {
        binding.apply {
            // Load prescription image with error handling
            prescriptionImageDetail.load(prescription.localImagePath) {
                placeholder(R.drawable.ic_prescription_placeholder)
                error(R.drawable.ic_prescription_placeholder)
                crossfade(true)
            }

            // Set prescription details
            doctorNameDetail.text = prescription.doctorName
            diseaseCategoryDetailChip.text = prescription.diseaseCategory.displayName
            dateAddedDetail.text = PrescriptionUtils.formatDate(prescription.dateAdded)

            // Handle modified date
            if (prescription.dateModified != prescription.dateAdded) {
                dateModifiedDetail.text = PrescriptionUtils.formatDate(prescription.dateModified)
                dateModifiedDetail.visibility = View.VISIBLE
            } else {
                dateModifiedDetail.visibility = View.GONE
            }

            // Show notes if available
            if (!prescription.notes.isNullOrBlank()) {
                notesDetail.text = prescription.notes
                notesCard.visibility = View.VISIBLE
            } else {
                notesCard.visibility = View.GONE
            }

            // Set dialog title
            detailTitle.text = getString(R.string.prescription_detail_title)
        }
    }

    private fun openEditDialog() {
        // Since we removed EditPrescriptionDialog, we'll show a simple message
        // In a real implementation, you would navigate to the edit fragment
        android.widget.Toast.makeText(
            requireContext(),
            "Edit functionality - Use unified fragment instead",
            android.widget.Toast.LENGTH_SHORT
        ).show()
        dismiss()
    }

    private fun showDeleteConfirmation() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(R.string.prescription_delete_confirmation_title)
            .setMessage(R.string.prescription_delete_confirmation_message)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                deletePrescription()
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun deletePrescription() {
        lifecycleScope.launch {
            try {
                viewModel.deletePrescription(prescriptionId)
                dismiss()

                // Show success message
                parentFragmentManager.setFragmentResult(
                    "prescription_deleted",
                    bundleOf("message" to getString(R.string.prescription_deleted_successfully))
                )
            } catch (e: Exception) {
                // Show error message
                parentFragmentManager.setFragmentResult(
                    "prescription_error",
                    bundleOf("error" to getString(R.string.error_deleting_prescription))
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()

        // Make dialog full screen
        dialog?.window?.apply {
            setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}