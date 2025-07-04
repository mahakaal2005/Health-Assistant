package com.example.health_assistant.features.journal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.health_assistant.databinding.FragmentJournalBinding
import com.example.health_assistant.features.journal.presentation.JournalAdapter
import com.example.health_assistant.features.journal.domain.JournalEntry
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class JournalFragment : Fragment() {
    private var _binding: FragmentJournalBinding? = null
    private val binding get() = _binding!!

    private val viewModel: JournalViewModel by viewModels()
    private lateinit var journalAdapter: JournalAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentJournalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        // Initialize adapter
        journalAdapter = JournalAdapter(
            onEdit = { journalEntry ->
                // Handle edit journal entry
                showSuccessMessage("Edit functionality coming soon")
            },
            onDelete = { journalEntry ->
                showDeleteConfirmation(journalEntry)
            }
        )

        // Setup RecyclerView for NestedScrollView compatibility
        binding.recyclerJournalEntries.apply {
            adapter = journalAdapter
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(false) // Important for NestedScrollView
            isNestedScrollingEnabled = false // Disable nested scrolling

            // Add item decoration for spacing - remove if JournalItemDecoration has issues
            // if (itemDecorationCount == 0) {
            //     addItemDecoration(JournalItemDecoration())
            // }
        }

        // Setup FAB
        binding.fabAddEntry.setOnClickListener {
            showAddJournalEntryDialog()
        }

        // Setup filter chips
        setupFilterChips()
    }

    private fun setupFilterChips() {
        // Setup chip click listeners
        binding.chipAll.setOnClickListener {
            viewModel.selectFilter(JournalFilterType.ALL)
        }
        binding.chipNotes.setOnClickListener {
            viewModel.selectFilter(JournalFilterType.NOTES)
        }
        binding.chipHealth.setOnClickListener {
            viewModel.selectFilter(JournalFilterType.HEALTH)
        }
        binding.chipActivity.setOnClickListener {
            viewModel.selectFilter(JournalFilterType.ACTIVITY)
        }
        binding.chipMood.setOnClickListener {
            viewModel.selectFilter(JournalFilterType.MOOD)
        }
    }

    private fun observeViewModel() {
        // Observe journal entries
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.entries.collectLatest { entries ->
                journalAdapter.submitList(entries)
                updateEmptyState(entries.isEmpty())
            }
        }

        // Observe filter changes to update chip selection
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.selectedFilterType.collectLatest { filterType ->
                updateChipSelection(filterType)
            }
        }

        // Observe loading state
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collectLatest { isLoading ->
                binding.progressLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }
    }

    private fun updateChipSelection(filterType: JournalFilterType) {
        // Reset all chips
        binding.chipAll.isChecked = false
        binding.chipNotes.isChecked = false
        binding.chipHealth.isChecked = false
        binding.chipActivity.isChecked = false
        binding.chipMood.isChecked = false

        // Select the active chip
        when (filterType) {
            JournalFilterType.ALL -> binding.chipAll.isChecked = true
            JournalFilterType.NOTES -> binding.chipNotes.isChecked = true
            JournalFilterType.HEALTH -> binding.chipHealth.isChecked = true
            JournalFilterType.ACTIVITY -> binding.chipActivity.isChecked = true
            JournalFilterType.MOOD -> binding.chipMood.isChecked = true
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            binding.recyclerJournalEntries.visibility = View.GONE
            try {
                binding.emptyStateView.root.visibility = View.VISIBLE
            } catch (e: Exception) {
                // If empty state view doesn't exist, show message
                showSuccessMessage("No journal entries found. Tap + to add one!")
            }
        } else {
            binding.recyclerJournalEntries.visibility = View.VISIBLE
            try {
                binding.emptyStateView.root.visibility = View.GONE
            } catch (e: Exception) {
                // Empty state view doesn't exist, no action needed
            }
        }
    }

    private fun showDeleteConfirmation(entry: JournalEntry) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete Entry")
            .setMessage("Are you sure you want to delete this journal entry?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteEntry(entry)
                showSuccessMessage("Entry deleted successfully")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAddJournalEntryDialog() {
        val dialogOptions = arrayOf(
            "Add Note",
            "Add Mood Entry",
            "Add Health Measurement",
            "Add Activity Log"
        )

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Add Journal Entry")
            .setItems(dialogOptions) { _, which ->
                when (which) {
                    0 -> addSampleNote()
                    1 -> addSampleMood()
                    2 -> addSampleHealth()
                    3 -> addSampleActivity()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addSampleNote() {
        val timestamp = System.currentTimeMillis()
        val entry = JournalEntry.Generic(
            id = 0L,
            timestamp = timestamp,
            type = "note",
            content = "New journal note created at ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(timestamp)}"
        )
        viewModel.addEntry(entry)
        showSuccessMessage("Note added successfully!")
    }

    private fun addSampleMood() {
        val timestamp = System.currentTimeMillis()
        val entry = JournalEntry.Mood(
            id = 0L,
            timestamp = timestamp,
            moodLevel = 4,
            emoji = "😊",
            description = "Feeling good today!",
            note = "Added via journal FAB"
        )
        viewModel.addEntry(entry)
        showSuccessMessage("Mood entry added successfully!")
    }

    private fun addSampleHealth() {
        val timestamp = System.currentTimeMillis()
        val entry = JournalEntry.Weight(
            id = 0L,
            timestamp = timestamp,
            weight = 70.0, // Changed from 70.0f to 70.0 (Double instead of Float)
            unit = "kg",
            note = "Sample weight measurement"
        )
        viewModel.addEntry(entry)
        showSuccessMessage("Health measurement added successfully!")
    }

    private fun addSampleActivity() {
        val timestamp = System.currentTimeMillis()
        val entry = JournalEntry.Workout(
            id = 0L,
            timestamp = timestamp,
            activityType = "Walking",
            duration = 30,
            summary = "30-minute walk added via journal"
        )
        viewModel.addEntry(entry)
        showSuccessMessage("Activity log added successfully!")
    }

    private fun showSuccessMessage(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}