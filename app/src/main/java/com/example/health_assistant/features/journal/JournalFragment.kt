package com.example.health_assistant.features.journal

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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

    private var allEntries: List<JournalEntry> = emptyList()
    private var filteredEntries: List<JournalEntry> = emptyList()

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
        }

        // Setup FAB to show dialog instead of navigation
        binding.fabAddEntry.setOnClickListener {
            showAddJournalEntryDialog()
        }

        // Setup search functionality
        setupSearchBar()
    }

    private fun setupSearchBar() {
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val searchQuery = s.toString().trim()
                performSearch(searchQuery)
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // Setup filter button like prescriptions fragment
        binding.filterButton.setOnClickListener {
            showSimpleJournalFilter()
        }
    }

    private fun showSimpleJournalFilter() {
        val popupMenu = androidx.appcompat.widget.PopupMenu(requireContext(), binding.filterButton)

        // Add journal filter options
        val filterOptions = listOf("All", "Activity", "Note", "Diary")

        filterOptions.forEachIndexed { index, option ->
            popupMenu.menu.add(0, index, 0, option)
        }

        popupMenu.setOnMenuItemClickListener { menuItem ->
            val selectedFilter = filterOptions[menuItem.itemId]
            applyJournalFilter(selectedFilter)
            true
        }

        popupMenu.show()
    }

    private fun applyJournalFilter(selectedFilter: String) {
        val newFilteredEntries = when (selectedFilter) {
            "All" -> allEntries
            "Activity" -> allEntries.filter { entry ->
                entry is JournalEntry.Workout || entry.type.contains("activity", ignoreCase = true)
            }
            "Note" -> allEntries.filter { entry ->
                entry is JournalEntry.Generic && entry.type.contains("note", ignoreCase = true)
            }
            "Diary" -> allEntries.filter { entry ->
                entry is JournalEntry.Generic && entry.type.contains("diary", ignoreCase = true)
            }
            else -> allEntries
        }

        filteredEntries = newFilteredEntries
        journalAdapter.submitList(newFilteredEntries)

        // Clear search when filter changes
        binding.searchEditText.setText("")
    }

    private fun performSearch(query: String) {
        if (query.isEmpty()) {
            // Show filtered entries when search is empty (respects current filter)
            journalAdapter.submitList(filteredEntries)
        } else {
            // Search within currently filtered entries, not all entries
            val searchResults = filteredEntries.filter { entry ->
                when (entry) {
                    is JournalEntry.Generic -> entry.content.contains(query, ignoreCase = true)
                    is JournalEntry.Weight -> entry.note?.contains(query, ignoreCase = true) == true
                    is JournalEntry.Workout -> entry.activityType.contains(query, ignoreCase = true) ||
                                              entry.summary.contains(query, ignoreCase = true)
                    else -> false
                }
            }
            journalAdapter.submitList(searchResults)
        }
    }

    private fun observeViewModel() {
        // Observe journal entries
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.entries.collectLatest { entries ->
                journalAdapter.submitList(entries)
                updateEmptyState(entries.isEmpty())
                allEntries = entries // Cache all entries
                filteredEntries = entries // Initialize filtered entries
            }
        }

        // Observe loading state
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collectLatest { isLoading ->
                binding.progressLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }
    }


    private fun updateEmptyState(isEmpty: Boolean) {
        binding.emptyStateGroup.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.recyclerJournalEntries.visibility = if (isEmpty) View.GONE else View.VISIBLE
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
        val dialog = AddJournalEntryDialogFragment.newInstance()
        dialog.show(parentFragmentManager, "AddJournalEntryDialog")
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