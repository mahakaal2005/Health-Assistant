package com.example.health_assistant.features.prescriptions.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
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
 * Fragment for displaying and editing prescription details
 */
@AndroidEntryPoint
class PrescriptionDetailFragment : Fragment() {

    private var _binding: FragmentPrescriptionDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PrescriptionsViewModel by viewModels()

    private var prescription: Prescription? = null
    private var isEditMode = false
    private var selectedCategory: DiseaseCategory? = null
    private val categories = DiseaseCategory.getDefaultCategories()

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

        setupToolbar()
        setupCategoryDropdown()
        setupClickListeners()
        setupTextWatchers()
        loadPrescription()
        observeViewModel()
    }

    private fun setupToolbar() {
        binding.toolbar.title = "Prescription Details"
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupCategoryDropdown() {
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            categories.map { it.displayName }
        )
        binding.categoryDropdown.setAdapter(adapter)
        binding.categoryDropdown.setOnItemClickListener { _, _, position, _ ->
            selectedCategory = categories[position]
        }
    }

    private fun setupClickListeners() {
        binding.editButton.setOnClickListener {
            toggleEditMode()
        }

        binding.saveButton.setOnClickListener {
            savePrescription()
        }

        binding.cancelButton.setOnClickListener {
            toggleEditMode()
        }
    }

    private fun setupTextWatchers() {
        binding.doctorNameEditText.addTextChangedListener {
            clearFieldError(binding.doctorNameInputLayout)
        }
    }

    private fun loadPrescription() {
        val prescriptionId = arguments?.getString("prescription_id") ?: return

        lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE

            prescription = viewModel.getPrescriptionById(prescriptionId)
            prescription?.let {
                displayPrescription(it)
            } ?: run {
                showError("Prescription not found")
                findNavController().navigateUp()
            }

            binding.progressBar.visibility = View.GONE
        }
    }

    private fun displayPrescription(prescription: Prescription) {
        // Display prescription details
        binding.apply {
            doctorNameText.text = prescription.doctorName

            // Fixed: Get category by ID instead of accessing non-existent diseaseCategory property
            val category = PrescriptionUtils.getCategoryById(prescription.categoryId)
            categoryViewText.text = category?.displayName ?: "Unknown Category"

            // Fixed: Use PrescriptionUtils.formatDate() which now exists
            dateAddedText.text = PrescriptionUtils.formatDate(prescription.dateAdded)

            if (prescription.dateModified != prescription.dateAdded) {
                dateModifiedText.text = PrescriptionUtils.formatDate(prescription.dateModified)
                dateModifiedText.visibility = View.VISIBLE
                dateModifiedLabel.visibility = View.VISIBLE
            } else {
                dateModifiedText.visibility = View.GONE
                dateModifiedLabel.visibility = View.GONE
            }

            // Handle notes
            if (!prescription.notes.isNullOrBlank()) {
                notesText.text = prescription.notes
                notesText.visibility = View.VISIBLE
                notesLabel.visibility = View.VISIBLE
            } else {
                notesText.visibility = View.GONE
                notesLabel.visibility = View.GONE
            }

            // Set edit text values for edit mode
            doctorNameEditText.setText(prescription.doctorName)
            notesEditText.setText(prescription.notes ?: "")

            // Fixed: Set category dropdown selection properly
            val categoryIndex = categories.indexOfFirst { it.id == prescription.categoryId }
            if (categoryIndex >= 0) {
                categoryDropdown.setText(categories[categoryIndex].displayName, false)
                selectedCategory = categories[categoryIndex]
            }
        }
    }

    private fun toggleEditMode() {
        isEditMode = !isEditMode

        binding.apply {
            if (isEditMode) {
                // Show edit views, hide display views
                viewGroup.visibility = View.GONE
                editGroup.visibility = View.VISIBLE
                editButton.visibility = View.GONE
                saveButton.visibility = View.VISIBLE
                cancelButton.visibility = View.VISIBLE
            } else {
                // Show display views, hide edit views
                viewGroup.visibility = View.VISIBLE
                editGroup.visibility = View.GONE
                editButton.visibility = View.VISIBLE
                saveButton.visibility = View.GONE
                cancelButton.visibility = View.GONE

                // Reset edit fields to original values
                prescription?.let { displayPrescription(it) }
                clearAllFieldErrors()
            }
        }
    }

    private fun savePrescription() {
        val doctorName = binding.doctorNameEditText.text?.toString()?.trim() ?: ""
        val notes = binding.notesEditText.text?.toString()?.trim()
        val category = selectedCategory

        if (!validateInput(doctorName, category)) {
            return
        }

        val currentPrescription = prescription ?: return

        lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            binding.saveButton.isEnabled = false

            try {
                // Fixed: Create updated prescription with correct parameters and timestamp
                val updatedPrescription = currentPrescription.copy(
                    doctorName = doctorName,
                    categoryId = category!!.id, // Fixed: Use categoryId instead of diseaseCategory
                    notes = notes?.takeIf { it.isNotBlank() }, // Fixed: Proper null safety
                    dateModified = System.currentTimeMillis() // Fixed: Use Long timestamp instead of LocalDateTime
                )

                // Fixed: Use updatePrescription() method which now exists
                viewModel.updatePrescription(updatedPrescription)

                // Update local prescription reference
                prescription = updatedPrescription

                // Switch back to view mode
                toggleEditMode()

                // Show success message
                showSuccess("Prescription updated successfully")

            } catch (e: Exception) {
                showError("Error updating prescription: ${e.message}")
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.saveButton.isEnabled = true
            }
        }
    }

    private fun validateInput(doctorName: String, category: DiseaseCategory?): Boolean {
        var isValid = true

        // Fixed: Use PrescriptionUtils.isValidDoctorName() which now exists
        if (doctorName.isBlank()) {
            binding.doctorNameInputLayout.error = "Doctor name is required"
            isValid = false
        } else if (!PrescriptionUtils.isValidDoctorName(doctorName)) {
            binding.doctorNameInputLayout.error = "Please enter a valid doctor name"
            isValid = false
        }

        if (category == null) {
            showError("Please select a category")
            isValid = false
        }

        return isValid
    }

    private fun clearFieldError(inputLayout: com.google.android.material.textfield.TextInputLayout) {
        inputLayout.error = null
    }

    private fun clearAllFieldErrors() {
        binding.doctorNameInputLayout.error = null
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                if (state.isLoading) {
                    binding.progressBar.visibility = View.VISIBLE
                } else {
                    binding.progressBar.visibility = View.GONE
                }

                state.successMessage?.let { message ->
                    showSuccess(message)
                    viewModel.clearSuccessMessage()
                }

                state.errorMessage?.let { error ->
                    showError(error)
                    viewModel.clearErrorMessage()
                }
            }
        }
    }

    private fun showSuccess(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "PrescriptionDetailFragment"

        fun newInstance(prescriptionId: String): PrescriptionDetailFragment {
            return PrescriptionDetailFragment().apply {
                arguments = Bundle().apply {
                    putString("prescription_id", prescriptionId)
                }
            }
        }
    }
}