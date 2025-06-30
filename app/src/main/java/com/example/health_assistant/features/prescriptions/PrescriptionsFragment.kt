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
        binding.toolbar.setNavigationOnClickListener {
            // Navigate back
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        prescriptionsAdapter = PrescriptionsAdapter(
            onPrescriptionClick = { prescriptionId ->
                // Open prescription detail view
                showPrescriptionDetail(prescriptionId)
            },
            onPrescriptionEdit = { prescriptionId ->
                // Open edit prescription dialog
                editPrescription(prescriptionId)
            },
            onPrescriptionDelete = { prescriptionId ->
                // Show delete confirmation
                confirmDeletePrescription(prescriptionId)
            },
            onPrescriptionView = { prescriptionId ->
                // Open prescription in full-screen view
                showPrescriptionDetail(prescriptionId)
            }
        )

        binding.prescriptionsRecyclerView.apply {
            adapter = prescriptionsAdapter
            layoutManager = LinearLayoutManager(requireContext())

            // Add simple item decoration instead of using the problematic spacing
            addItemDecoration(object : androidx.recyclerview.widget.RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: android.graphics.Rect,
                    view: android.view.View,
                    parent: androidx.recyclerview.widget.RecyclerView,
                    state: androidx.recyclerview.widget.RecyclerView.State
                ) {
                    outRect.bottom = 16 // 16dp spacing between items
                }
            })
        }
    }

    private fun setupSearchBar() {
        binding.searchEditText.addTextChangedListener { text ->
            viewModel.updateSearchQuery(text?.toString() ?: "")
        }

        // Handle search input layout end icon (clear text)
        binding.searchInputLayout.setEndIconOnClickListener {
            binding.searchEditText.text?.clear()
            viewModel.clearSearch()
        }
    }

    private fun setupFab() {
        binding.fabAddPrescription.setOnClickListener {
            // Open add prescription bottom sheet
            openAddPrescriptionDialog()
        }
    }

    private fun setupFragmentResultListeners() {
        // Listen for prescription deleted result
        parentFragmentManager.setFragmentResultListener("prescription_deleted", this) { _, bundle ->
            val message = bundle.getString("message")
            message?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show()
            }
        }

        // Listen for prescription updated result
        parentFragmentManager.setFragmentResultListener("prescription_updated", this) { _, bundle ->
            val message = bundle.getString("message")
            message?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show()
            }
        }

        // Listen for prescription errors
        parentFragmentManager.setFragmentResultListener("prescription_error", this) { _, bundle ->
            val error = bundle.getString("error")
            error?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun observeViewModel() {
        // Observe prescriptions list
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.prescriptions.collect { prescriptions ->
                prescriptionsAdapter.submitList(prescriptions)
            }
        }

        // Observe UI state
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                updateUIState(state)
            }
        }

        // Observe search query
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.searchQuery.collect { query ->
                if (binding.searchEditText.text.toString() != query) {
                    binding.searchEditText.setText(query)
                }
            }
        }
    }

    private fun updateUIState(state: PrescriptionsUiState) {
        // Show/hide loading
        binding.loadingLayout.visibility = if (state.isLoading) View.VISIBLE else View.GONE

        // Show/hide empty state
        binding.emptyStateLayout.visibility = if (state.isEmpty && !state.isLoading) {
            View.VISIBLE
        } else {
            View.GONE
        }

        // Show/hide prescriptions list
        binding.prescriptionsRecyclerView.visibility = if (!state.isEmpty && !state.isLoading) {
            View.VISIBLE
        } else {
            View.GONE
        }

        // Show messages
        state.message?.let { message ->
            Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
            viewModel.clearMessage()
        }

        // Show errors
        state.error?.let { error ->
            Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG).show()
        }
    }

    private fun showPrescriptionDetail(prescriptionId: String) {
        // Navigate to the combined detail/edit fragment
        val bundle = bundleOf("prescription_id" to prescriptionId)
        findNavController().navigate(
            R.id.action_prescriptions_to_prescription_detail,
            bundle
        )
    }

    private fun editPrescription(prescriptionId: String) {
        // Use the same detail fragment - it handles both viewing and editing
        showPrescriptionDetail(prescriptionId)
    }

    private fun confirmDeletePrescription(prescriptionId: String) {
        // Show confirmation dialog
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete Prescription?")
            .setMessage("This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deletePrescription(prescriptionId)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openAddPrescriptionDialog() {
        val addPrescriptionDialog = AddPrescriptionBottomSheet()
        addPrescriptionDialog.show(childFragmentManager, "AddPrescriptionBottomSheet")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}