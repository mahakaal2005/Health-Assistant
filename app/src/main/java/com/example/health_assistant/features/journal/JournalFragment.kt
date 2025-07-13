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
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.health_assistant.R
import com.example.health_assistant.databinding.FragmentJournalBinding
import com.example.health_assistant.features.journal.presentation.JournalAdapter
import com.example.health_assistant.features.journal.domain.JournalEntry
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.*
import com.google.android.material.floatingactionbutton.FloatingActionButton
import android.widget.LinearLayout

@AndroidEntryPoint
class JournalFragment : Fragment() {
    private var _binding: FragmentJournalBinding? = null
    private val binding get() = _binding!!

    private val viewModel: JournalViewModel by viewModels()
    private lateinit var journalAdapter: JournalAdapter

    private var allEntries: List<JournalEntry> = emptyList()
    private var filteredEntries: List<JournalEntry> = emptyList()

    // Expanding FAB Menu state and animation variables
    private var isFabMenuOpen = false
    private lateinit var fabMain: FloatingActionButton
    private lateinit var fabAddNoteContainer: LinearLayout
    private lateinit var fabAddDiaryContainer: LinearLayout
    private lateinit var fabOverlay: View

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
                // Navigate to appropriate detail fragment based on entry type
                navigateToDetailFragment(journalEntry)
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

        // Setup Expanding FAB Menu
        setupExpandingFabMenu()

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


    private fun navigateToCreateNote() {
        val bundle = Bundle().apply {
            putBoolean("isCreateMode", true)
            putString("noteContent", "")
            putLong("noteId", 0L)
        }
        findNavController().navigate(R.id.action_journalFragment_to_noteDetailFragment, bundle)
    }

    private fun navigateToCreateDiary() {
        val bundle = Bundle().apply {
            putBoolean("isCreateMode", true)
            putString("diaryTitle", "")
            putString("diaryContent", "")
            putLong("diaryId", 0L)
        }
        findNavController().navigate(R.id.action_journalFragment_to_diaryDetailFragment, bundle)
    }

    private fun showSuccessMessage(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    private fun navigateToDetailFragment(journalEntry: JournalEntry) {
        try {
            when {
                // Check for activity-related entries
                journalEntry is JournalEntry.Workout ||
                journalEntry.type.contains("activity", ignoreCase = true) -> {
                    findNavController().navigate(R.id.action_journalFragment_to_activityDetailFragment)
                }
                // Check for diary-related entries
                journalEntry is JournalEntry.Generic &&
                journalEntry.type.contains("diary", ignoreCase = true) -> {
                    val bundle = Bundle().apply {
                        // Extract title from content (first line) or use content if short
                        val content = journalEntry.content
                        val title = if (content.length > 50) {
                            content.lines().firstOrNull()?.take(50) ?: "Untitled"
                        } else {
                            content.ifEmpty { "Untitled" }
                        }
                        putString("diaryTitle", title)
                        putString("diaryContent", content)
                        putLong("diaryId", journalEntry.id)
                        putLong("diaryTimestamp", journalEntry.timestamp) // Add original timestamp
                    }
                    findNavController().navigate(R.id.action_journalFragment_to_diaryDetailFragment, bundle)
                }
                // Default to note detail for notes and other types
                else -> {
                    val bundle = Bundle().apply {
                        val content = when (journalEntry) {
                            is JournalEntry.Generic -> journalEntry.content
                            else -> ""
                        }
                        putString("noteContent", content)
                        putLong("noteId", journalEntry.id)
                        putLong("noteTimestamp", journalEntry.timestamp) // Add original timestamp
                    }
                    findNavController().navigate(R.id.action_journalFragment_to_noteDetailFragment, bundle)
                }
            }
        } catch (e: Exception) {
            // Fallback if navigation fails
            showSuccessMessage("Opening ${journalEntry.type} details...")
        }
    }

    // New function to setup expanding FAB menu
    private fun setupExpandingFabMenu() {
        // Initialize view references with correct IDs from layout
        fabMain = binding.fabMain
        fabAddNoteContainer = binding.fabAddNoteContainer
        fabAddDiaryContainer = binding.fabAddDiaryContainer
        fabOverlay = binding.fabOverlay

        // Initially hide the expanded FABs and overlay
        fabAddNoteContainer.visibility = View.GONE
        fabAddDiaryContainer.visibility = View.GONE
        fabOverlay.visibility = View.GONE

        // Set initial states for animation
        fabAddNoteContainer.alpha = 0f
        fabAddDiaryContainer.alpha = 0f
        fabAddNoteContainer.translationY = 100f
        fabAddDiaryContainer.translationY = 100f

        // Toggle FAB menu on main FAB click
        fabMain.setOnClickListener {
            if (isFabMenuOpen) {
                closeFabMenu()
            } else {
                openFabMenu()
            }
        }

        // Hide menu when overlay is clicked
        fabOverlay.setOnClickListener {
            closeFabMenu()
        }

        // Navigate to create note/diary on FAB clicks
        fabAddNoteContainer.setOnClickListener {
            navigateToCreateNote()
            closeFabMenu()
        }

        fabAddDiaryContainer.setOnClickListener {
            navigateToCreateDiary()
            closeFabMenu()
        }
    }

    private fun openFabMenu() {
        isFabMenuOpen = true

        // Show containers
        fabAddNoteContainer.visibility = View.VISIBLE
        fabAddDiaryContainer.visibility = View.VISIBLE
        fabOverlay.visibility = View.VISIBLE

        // Animate main FAB rotation (180 degrees) and change icon to cross
        fabMain.animate()
            .rotation(180f)
            .setDuration(300)
            .withStartAction {
                // Change icon to cross during rotation
                fabMain.setImageResource(R.drawable.ic_close)
            }
            .start()

        // Animate sub FABs with staggered timing (Google Fit style)
        fabAddDiaryContainer.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(300)
            .setStartDelay(0)
            .start()

        fabAddNoteContainer.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(300)
            .setStartDelay(50) // Slight delay for staggered effect
            .start()

        // Animate overlay fade in with stronger dimming effect
        fabOverlay.animate()
            .alpha(0.6f) // Increased from 0.3f to 0.6f for more dimming
            .setDuration(300)
            .start()
    }

    private fun closeFabMenu() {
        isFabMenuOpen = false

        // Animate main FAB rotation back (0 degrees) and change icon back to plus
        fabMain.animate()
            .rotation(0f)
            .setDuration(300)
            .withStartAction {
                // Change icon back to plus during rotation
                fabMain.setImageResource(R.drawable.ic_add)
            }
            .start()

        // Animate sub FABs out with staggered timing
        fabAddNoteContainer.animate()
            .alpha(0f)
            .translationY(100f)
            .setDuration(250)
            .setStartDelay(0)
            .withEndAction {
                fabAddNoteContainer.visibility = View.GONE
            }
            .start()

        fabAddDiaryContainer.animate()
            .alpha(0f)
            .translationY(100f)
            .setDuration(250)
            .setStartDelay(30) // Slight delay for staggered effect
            .withEndAction {
                fabAddDiaryContainer.visibility = View.GONE
            }
            .start()

        // Animate overlay fade out
        fabOverlay.animate()
            .alpha(0f)
            .setDuration(250)
            .withEndAction {
                fabOverlay.visibility = View.GONE
            }
            .start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}