package com.example.health_assistant.features.prescriptions.dialogs

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import coil3.load
import coil3.request.crossfade
import coil3.request.error
import coil3.request.placeholder
import com.example.health_assistant.R
import com.example.health_assistant.data.model.DiseaseCategory
import com.example.health_assistant.data.model.Prescription
import com.example.health_assistant.databinding.FragmentPrescriptionDetailBinding
import com.example.health_assistant.features.prescriptions.PrescriptionsViewModel
import com.example.health_assistant.features.prescriptions.utils.PrescriptionUtils
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Combined fragment for viewing and editing prescription details
 * Toggles between view mode and edit mode in a single interface
 */
@AndroidEntryPoint
class PrescriptionDetailFragment : Fragment() {

    private var _binding: FragmentPrescriptionDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PrescriptionsViewModel by viewModels()
    private lateinit var prescriptionId: String
    private var currentPrescription: Prescription? = null
    private var isEditMode = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPrescriptionDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get prescription ID from arguments
        prescriptionId = arguments?.getString("prescription_id")
            ?: throw IllegalArgumentException("Prescription ID is required")

        setupViews()
        setupCategorySpinner()
        observeViewModel()
        loadPrescriptionData()
    }

    private fun setupViews() {
        binding.apply {
            // Setup toolbar navigation
            toolbar.setNavigationOnClickListener {
                if (isEditMode) {
                    // If in edit mode, show discard changes dialog
                    showDiscardChangesDialog()
                } else {
                    // Navigate back
                    findNavController().navigateUp()
                }
            }

            // Setup edit toggle button
            editToggleButton.setOnClickListener {
                toggleEditMode()
            }

            // Setup action buttons
            cancelButton.setOnClickListener {
                toggleEditMode() // Cancel edit and return to view mode
            }

            saveButton.setOnClickListener {
                saveChanges()
            }

            // Setup delete button
            deleteButton.setOnClickListener {
                showDeleteConfirmation()
            }

            // Setup image zoom functionality
            prescriptionImageView.setOnClickListener {
                // TODO: Implement image zoom functionality
                showImageZoom()
            }
        }
    }

    private fun setupCategorySpinner() {
        val categories = DiseaseCategory.getDefaultCategories()
        val categoryNames = categories.map { it.displayName }

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            categoryNames
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.categorySpinner.adapter = adapter
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.prescriptions.collect { prescriptionItems ->
                // Extract prescription from PrescriptionItem.PrescriptionCard
                val prescription = prescriptionItems
                    .filterIsInstance<com.example.health_assistant.features.prescriptions.PrescriptionItem.PrescriptionCard>()
                    .map { it.prescription }
                    .find { it.id == prescriptionId }

                if (prescription != null) {
                    currentPrescription = prescription
                    displayPrescriptionDetails(prescription)
                }
            }
        }
    }

    private fun loadPrescriptionData() {
        // Data will be loaded through the observed prescriptions flow
    }

    @SuppressLint("StringFormatInvalid")
    private fun displayPrescriptionDetails(prescription: Prescription) {
        binding.apply {
            // Load prescription image
            prescriptionImageView.load(prescription.localImagePath) {
                placeholder(R.drawable.ic_prescription_placeholder)
                error(R.drawable.ic_prescription_placeholder)
                crossfade(true)
            }

            // Display prescription information
            doctorNameViewText.text = prescription.doctorName
            categoryViewText.text = prescription.diseaseCategory.displayName
            dateAddedText.text = PrescriptionUtils.formatDate(prescription.dateAdded)

            // Show/hide modified date
            if (prescription.dateModified != prescription.dateAdded) {
                dateModifiedContainer.visibility = View.VISIBLE
                dateModifiedText.text = PrescriptionUtils.formatDate(prescription.dateModified)
            } else {
                dateModifiedContainer.visibility = View.GONE
            }

            // Display notes
            if (!prescription.notes.isNullOrBlank()) {
                notesViewText.text = prescription.notes
            } else {
                notesViewText.text = getString(R.string.prescription_notes_empty)
            }

            // Populate edit fields (hidden initially)
            doctorNameInput.setText(prescription.doctorName)
            notesInput.setText(prescription.notes ?: "")

            // Set category spinner selection
            val categories = DiseaseCategory.getDefaultCategories()
            val categoryIndex = categories.indexOfFirst { it.id == prescription.diseaseCategory.id }
            if (categoryIndex >= 0) {
                categorySpinner.setSelection(categoryIndex)
            }
        }
    }

    private fun toggleEditMode() {
        isEditMode = !isEditMode
        updateUIForMode()
    }

    private fun updateUIForMode() {
        binding.apply {
            if (isEditMode) {
                // Switch to edit mode
                editToggleButton.text = getString(R.string.prescription_action_cancel)
                editToggleButton.setIconResource(R.drawable.ic_close)

                // Hide view elements, show edit elements
                doctorNameViewText.visibility = View.GONE
                doctorNameInputLayout.visibility = View.VISIBLE

                categoryViewText.visibility = View.GONE
                categorySpinner.visibility = View.VISIBLE

                notesViewText.visibility = View.GONE
                notesInputLayout.visibility = View.VISIBLE

                actionButtonsContainer.visibility = View.VISIBLE
                deleteButton.visibility = View.GONE

                // Update toolbar title
                toolbar.title = getString(R.string.edit_prescription_title)

            } else {
                // Switch to view mode
                editToggleButton.text = getString(R.string.prescription_action_edit)
                editToggleButton.setIconResource(R.drawable.ic_edit)

                // Show view elements, hide edit elements
                doctorNameViewText.visibility = View.VISIBLE
                doctorNameInputLayout.visibility = View.GONE

                categoryViewText.visibility = View.VISIBLE
                categorySpinner.visibility = View.GONE

                notesViewText.visibility = View.VISIBLE
                notesInputLayout.visibility = View.GONE

                actionButtonsContainer.visibility = View.GONE
                deleteButton.visibility = View.VISIBLE

                // Update toolbar title
                toolbar.title = getString(R.string.prescription_detail_title)

                // Clear any validation errors
                doctorNameInputLayout.error = null
            }
        }
    }

    private fun saveChanges() {
        val doctorName = binding.doctorNameInput.text.toString().trim()
        val notes = binding.notesInput.text.toString().trim()
        val selectedCategoryIndex = binding.categorySpinner.selectedItemPosition

        // Validate input
        if (!validateInput(doctorName)) return

        val selectedCategory = DiseaseCategory.getDefaultCategories()[selectedCategoryIndex]

        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE
                binding.saveButton.isEnabled = false

                currentPrescription?.let { prescription ->
                    val updatedPrescription = prescription.copy(
                        doctorName = doctorName,
                        diseaseCategory = selectedCategory,
                        notes = notes.ifBlank { null },
                        dateModified = java.time.LocalDateTime.now()
                    )

                    viewModel.updatePrescription(updatedPrescription)

                    // Switch back to view mode
                    toggleEditMode()

                    // Show success message
                    Snackbar.make(
                        binding.root,
                        getString(R.string.prescription_updated_successfully),
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Snackbar.make(
                    binding.root,
                    getString(R.string.error_updating_prescription),
                    Snackbar.LENGTH_LONG
                ).show()
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.saveButton.isEnabled = true
            }
        }
    }

    private fun validateInput(doctorName: String): Boolean {
        return when {
            doctorName.isBlank() -> {
                binding.doctorNameInputLayout.error = getString(R.string.error_doctor_name_required)
                false
            }
            !PrescriptionUtils.isValidDoctorName(doctorName) -> {
                binding.doctorNameInputLayout.error = getString(R.string.error_invalid_doctor_name)
                false
            }
            else -> {
                binding.doctorNameInputLayout.error = null
                true
            }
        }
    }

    private fun showDiscardChangesDialog() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.discard_changes_title))
            .setMessage(getString(R.string.discard_changes_message))
            .setPositiveButton(getString(R.string.action_discard)) { _, _ ->
                // Reset fields and switch to view mode
                currentPrescription?.let { displayPrescriptionDetails(it) }
                toggleEditMode()
            }
            .setNegativeButton(getString(R.string.action_continue_editing), null)
            .show()
    }

    private fun showDeleteConfirmation() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.prescription_delete_confirmation_title))
            .setMessage(getString(R.string.prescription_delete_confirmation_message))
            .setPositiveButton(getString(R.string.action_delete)) { _, _ ->
                deletePrescription()
            }
            .setNegativeButton(getString(R.string.action_cancel), null)
            .show()
    }

    private fun deletePrescription() {
        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE
                viewModel.deletePrescription(prescriptionId)

                // Navigate back with success message
                Snackbar.make(
                    binding.root,
                    getString(R.string.prescription_deleted_successfully),
                    Snackbar.LENGTH_SHORT
                ).show()

                findNavController().navigateUp()

            } catch (e: Exception) {
                Snackbar.make(
                    binding.root,
                    getString(R.string.error_deleting_prescription),
                    Snackbar.LENGTH_LONG
                ).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun showImageZoom() {
        // TODO: Implement image zoom dialog or activity
        currentPrescription?.let { prescription ->
            // For now, show a simple message
            Snackbar.make(
                binding.root,
                "Image zoom functionality - Coming soon!",
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}