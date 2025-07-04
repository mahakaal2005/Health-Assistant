package com.example.health_assistant.features.prescriptions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.health_assistant.databinding.FragmentPrescriptionsBinding
import com.example.health_assistant.features.prescriptions.adapter.PrescriptionsAdapter
import com.example.health_assistant.features.prescriptions.adapter.GridSpacingItemDecoration
import com.example.health_assistant.features.prescriptions.dialogs.AddPrescriptionBottomSheet
import com.example.health_assistant.features.prescriptions.viewmodel.PrescriptionsViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

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

    @Inject
    lateinit var categoryManager: com.example.health_assistant.data.manager.CategoryManager

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
        observeSelectedCategories()
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
                navigateToPrescriptionDetail(prescription.id.toString())
            }
        )

        binding.recyclerViewPrescriptions.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = prescriptionsAdapter
            setHasFixedSize(true)
            // Add spacing between grid items
            addItemDecoration(GridSpacingItemDecoration(2, 8, true))
        }
    }

    private fun setupSearchBar() {
        binding.searchEditText.addTextChangedListener { editable ->
            val query = editable?.toString() ?: ""
            viewModel.updateSearchQuery(query)
        }

        // Simple category filter button - just shows a popup menu
        binding.categoryFilterButton.setOnClickListener {
            showSimpleCategoryFilter()
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
        val dialog = com.example.health_assistant.features.prescriptions.dialogs.PrescriptionDetailDialog.newInstance(prescriptionId)
        dialog.show(parentFragmentManager, "PrescriptionDetailDialog")
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
                viewModel.deletePrescription(prescription.id.toString())
            }
            .setNegativeButton("Cancel", null) // Fixed: Use hardcoded string
            .show()
    }

    /**
     * Enhanced category filter using dynamic categories from CategoryManager
     */
    private fun showSimpleCategoryFilter() {
        val popupMenu = androidx.appcompat.widget.PopupMenu(requireContext(), binding.categoryFilterButton)

        // Get dynamic categories from CategoryManager (includes both predefined and user-created)
        val categories = categoryManager.getCategoriesForFilter()

        categories.forEachIndexed { index, category ->
            popupMenu.menu.add(0, index, 0, category)
        }

        popupMenu.setOnMenuItemClickListener { menuItem ->
            val selectedCategory = categories[menuItem.itemId]
            viewModel.updateCategoryFilter(selectedCategory)
            updateFilterIndicator(selectedCategory)
            true
        }

        popupMenu.show()
    }

    /**
     * Simple filter indicator update
     */
    private fun updateFilterIndicator(selectedCategory: String) {
        if (selectedCategory == "All Categories") {
            binding.activeFilterIndicator.visibility = View.GONE
        } else {
            binding.activeFilterIndicator.visibility = View.VISIBLE
            binding.filterCountText.text = "1"
        }
    }

    /**
     * Remove complex category observation - keep it simple
     */
    private fun observeSelectedCategories() {
        // Simple implementation - just observe the current filter
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentCategoryFilter.collect { filter ->
                updateFilterIndicator(filter)
            }
        }
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