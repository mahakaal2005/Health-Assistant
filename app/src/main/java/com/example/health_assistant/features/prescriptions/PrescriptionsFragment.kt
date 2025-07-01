package com.example.health_assistant.features.prescriptions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.health_assistant.R
import com.example.health_assistant.databinding.FragmentPrescriptionsBinding
import com.example.health_assistant.features.prescriptions.adapter.PrescriptionsAdapter
import com.example.health_assistant.features.prescriptions.dialogs.AddPrescriptionBottomSheet
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Main fragment for displaying and managing prescriptions
 * Implements MVVM pattern with ViewBinding and reactive UI updates
 */
@AndroidEntryPoint
class PrescriptionsFragment : Fragment() {

    private var _binding: FragmentPrescriptionsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PrescriptionsViewModel by viewModels()
    private lateinit var prescriptionsAdapter: PrescriptionsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPrescriptionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupRecyclerView()
        setupSearchBar()
        setupFab()
        setupFragmentResultListeners()
        observeViewModel()
    }

    private fun setupToolbar() {
        binding.toolbar.title = "Prescriptions" // Fixed: Use hardcoded string instead of non-existent resource
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupRecyclerView() {
        prescriptionsAdapter = PrescriptionsAdapter(
            onPrescriptionClick = { prescription ->
                navigateToPrescriptionDetail(prescription.prescription.id) // Fixed: Access prescription.id correctly
            },
            onPrescriptionEdit = { prescription -> // Fixed: Use correct parameter name
                editPrescription(prescription.prescription)
            },
            onPrescriptionDelete = { prescription -> // Fixed: Use correct parameter name
                deletePrescription(prescription.prescription)
            },
            onPrescriptionView = { prescription -> // Fixed: Use correct parameter name
                navigateToPrescriptionDetail(prescription.prescription.id)
            }
        )

        binding.recyclerViewPrescriptions.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = prescriptionsAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupSearchBar() {
        binding.searchEditText.addTextChangedListener { editable ->
            val query = editable?.toString() ?: ""
            viewModel.updateSearchQuery(query)
        }

        // Clear search functionality
        binding.clearSearchButton.setOnClickListener {
            binding.searchEditText.text?.clear()
            viewModel.updateSearchQuery("")
        }
    }

    private fun setupFab() {
        binding.fabAddPrescription.setOnClickListener {
            showAddPrescriptionDialog()
        }
    }

    private fun setupFragmentResultListeners() {
        // Listen for prescription addition results
        childFragmentManager.setFragmentResultListener(
            "prescription_added",
            this
        ) { _, result ->
            val success = result.getBoolean("success", false)
            if (success) {
                viewModel.refreshPrescriptions()
                showMessage("Prescription added successfully") // Fixed: Use hardcoded string
            }
        }
    }

    private fun observeViewModel() {
        // Observe prescriptions list
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.prescriptions.collect { prescriptionItems ->
                prescriptionsAdapter.submitList(prescriptionItems)
                updateEmptyState(prescriptionItems.isEmpty())
            }
        }

        // Observe search query
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.searchQuery.collect { query ->
                if (binding.searchEditText.text.toString() != query) {
                    binding.searchEditText.setText(query)
                    binding.searchEditText.setSelection(query.length)
                }
            }
        }

        // Observe UI state - Fixed: Use correct property names
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                updateLoadingState(state.isLoading)

                // Handle success messages - Fixed: Use successMessage property
                state.successMessage?.let { message ->
                    showMessage(message)
                    viewModel.clearSuccessMessage() // Fixed: Use correct method name
                }

                // Handle error messages - Fixed: Use errorMessage property
                state.errorMessage?.let { error ->
                    showError(error)
                    viewModel.clearErrorMessage() // Fixed: Use correct method name
                }
            }
        }
    }

    private fun updateLoadingState(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.fabAddPrescription.isEnabled = !isLoading
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.emptyStateGroup.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.recyclerViewPrescriptions.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun showAddPrescriptionDialog() {
        val dialog = AddPrescriptionBottomSheet.newInstance()
        dialog.show(childFragmentManager, AddPrescriptionBottomSheet.TAG)
    }

    private fun navigateToPrescriptionDetail(prescriptionId: String) {
        val bundle = bundleOf("prescription_id" to prescriptionId)
        // TODO: Add navigation action in nav graph
        // findNavController().navigate(R.id.action_prescriptionsFragment_to_prescriptionDetailFragment, bundle)
        // For now, show a placeholder message
        showMessage("Navigation to prescription detail - ID: $prescriptionId")
    }

    private fun showPrescriptionOptions(prescription: PrescriptionItem.PrescriptionCard) {
        // Create options dialog for prescription actions (view, edit, delete)
        val options = arrayOf(
            "View", // Fixed: Use hardcoded strings
            "Edit",
            "Delete"
        )

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Prescription Options") // Fixed: Use hardcoded string
            .setItems(options) { _, which ->
                when (which) {
                    0 -> navigateToPrescriptionDetail(prescription.prescription.id)
                    1 -> editPrescription(prescription.prescription)
                    2 -> deletePrescription(prescription.prescription)
                }
            }
            .show()
    }

    private fun editPrescription(prescription: com.example.health_assistant.data.model.Prescription) {
        // TODO: Add navigation action in nav graph
        // findNavController().navigate(R.id.action_prescriptionsFragment_to_editPrescriptionFragment, bundle)
        // For now, show a placeholder message
        showMessage("Edit prescription - ID: ${prescription.id}")
    }

    private fun deletePrescription(prescription: com.example.health_assistant.data.model.Prescription) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete Prescription") // Fixed: Use hardcoded string
            .setMessage("Are you sure you want to delete the prescription from ${prescription.doctorName}?") // Fixed: Use hardcoded string
            .setPositiveButton("Delete") { _, _ -> // Fixed: Use hardcoded string
                viewModel.deletePrescription(prescription.id)
            }
            .setNegativeButton("Cancel", null) // Fixed: Use hardcoded string
            .show()
    }

    // Fixed: Use explicit String type to resolve Snackbar overload ambiguity
    private fun showMessage(message: String) {
        Snackbar.make(binding.root, message as CharSequence, Snackbar.LENGTH_SHORT).show()
    }

    // Fixed: Use explicit String type to resolve Snackbar overload ambiguity
    private fun showError(error: String) {
        Snackbar.make(binding.root, error as CharSequence, Snackbar.LENGTH_LONG).show()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshPrescriptions()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}