package com.example.health_assistant.features.journal.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.health_assistant.R
import com.example.health_assistant.databinding.FragmentDiaryDetailBinding
import com.example.health_assistant.features.journal.JournalViewModel
import com.example.health_assistant.features.journal.domain.JournalEntry
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DiaryDetailFragment : Fragment() {
    private var _binding: FragmentDiaryDetailBinding? = null
    private val binding get() = _binding!!

    // Edit mode state
    private var isEditMode = false
    private var diaryId: Long = 0L
    private var originalTimestamp: Long = 0L // Add original timestamp
    private var originalTitle: String = ""
    private var originalContent: String = ""

    // Add ViewModel to handle database operations
    private val journalViewModel: JournalViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDiaryDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupToolbar()
        setupClickListeners()
        loadDiaryData()
    }

    private fun setupUI() {
        // Initially set to read-only mode
        setEditMode(false)
    }

    private fun setupToolbar() {
        // Setup the toolbar with fixed title
        binding.toolbar.title = "Diary Entry"
        binding.toolbar.setNavigationOnClickListener {
            if (isEditMode) {
                // If in edit mode, show confirmation dialog
                showDiscardChangesDialog()
            } else {
                requireActivity().onBackPressed()
            }
        }
    }

    private fun setupClickListeners() {
        // More options button click listener
        binding.moreOptionsButton.setOnClickListener {
            showMoreOptionsMenu()
        }
    }

    private fun showMoreOptionsMenu() {
        // Create popup menu following the prescription pattern
        val popupMenu = androidx.appcompat.widget.PopupMenu(
            requireContext(),
            binding.moreOptionsButton
        )

        // Add menu items based on current mode - text only
        if (isEditMode) {
            popupMenu.menu.add(0, 1, 0, "Save")
            popupMenu.menu.add(0, 2, 0, "Cancel")
        } else {
            popupMenu.menu.add(0, 3, 0, "Edit")
        }

        // Always show delete option
        popupMenu.menu.add(0, 4, 0, "Delete")

        // Set menu item click listener
        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                1 -> { // Save
                    saveDiary()
                    true
                }
                2 -> { // Cancel
                    showDiscardChangesDialog()
                    true
                }
                3 -> { // Edit
                    setEditMode(true)
                    true
                }
                4 -> { // Delete
                    deleteDiary()
                    true
                }
                else -> false
            }
        }

        popupMenu.show()
    }

    private fun showDiscardChangesDialog() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Discard Changes")
            .setMessage("Are you sure you want to discard your changes?")
            .setPositiveButton("Discard") { _, _ ->
                // Reload original data and exit edit mode
                loadDiaryData()
                setEditMode(false)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun loadDiaryData() {
        // Get data from arguments
        arguments?.let { bundle ->
            val title = bundle.getString("diaryTitle", "")
            val content = bundle.getString("diaryContent", "")
            diaryId = bundle.getLong("diaryId", 0L)
            originalTimestamp = bundle.getLong("diaryTimestamp", System.currentTimeMillis()) // Get original timestamp

            // Store original data for comparison
            originalTitle = title
            originalContent = content

            // Load the passed diary data
            binding.etDiaryTitle.setText(title)
            binding.etDiaryContent.setText(content)
        }
    }

    private fun setEditMode(editMode: Boolean) {
        isEditMode = editMode

        binding.apply {
            if (editMode) {
                // Switch to edit mode
                toolbar.title = "Edit Diary Entry"

                // Enable all EditText fields for editing
                etDiaryTitle.isEnabled = true
                etDiaryContent.isEnabled = true

                // Ensure they are focusable and clickable
                etDiaryTitle.isFocusable = true
                etDiaryContent.isFocusable = true
                etDiaryTitle.isFocusableInTouchMode = true
                etDiaryContent.isFocusableInTouchMode = true

                // Show cursors
                etDiaryTitle.isCursorVisible = true
                etDiaryContent.isCursorVisible = true

                // Update keyboard behavior for edit mode
                updateKeyboardBehaviorForEditMode(true)

                // Request focus on title field
                etDiaryTitle.requestFocus()
                etDiaryTitle.post {
                    val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                    imm.showSoftInput(etDiaryTitle, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
                }

            } else {
                // Switch to view mode
                toolbar.title = "Diary Entry"

                // Disable EditText fields for read-only mode
                etDiaryTitle.isEnabled = false
                etDiaryContent.isEnabled = false

                // Make them non-focusable
                etDiaryTitle.isFocusable = false
                etDiaryContent.isFocusable = false

                // Hide cursors
                etDiaryTitle.isCursorVisible = false
                etDiaryContent.isCursorVisible = false

                // Clear focus and hide keyboard
                etDiaryTitle.clearFocus()
                etDiaryContent.clearFocus()

                // Update keyboard behavior for view mode
                updateKeyboardBehaviorForEditMode(false)

                val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.hideSoftInputFromWindow(root.windowToken, 0)
            }
        }
    }

    private fun updateKeyboardBehaviorForEditMode(isEditMode: Boolean) {
        // Following the prescription pattern for keyboard behavior
        activity?.window?.apply {
            if (isEditMode) {
                // Enhanced keyboard behavior for edit mode
                setSoftInputMode(
                    android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or
                            android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
                )
            } else {
                // Standard behavior for view mode
                setSoftInputMode(
                    android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN or
                            android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN
                )
            }
        }
    }

    private fun saveDiary() {
        val title = binding.etDiaryTitle.text.toString().trim()
        val content = binding.etDiaryContent.text.toString().trim()

        if (title.isEmpty()) {
            binding.etDiaryTitle.error = "Title is required"
            return
        }

        if (content.isEmpty()) {
            binding.etDiaryContent.error = "Content is required"
            return
        }

        // Combine title and content for diary storage (similar to how it was parsed)
        val combinedContent = if (title.isNotEmpty()) {
            "$title\n\n$content"
        } else {
            content
        }

        // Create updated journal entry - PRESERVE ORIGINAL TIMESTAMP
        val updatedEntry = JournalEntry.Generic(
            id = diaryId,
            timestamp = originalTimestamp, // Use original timestamp, don't create new one
            type = "diary",
            content = combinedContent
        )

        // Update the entry in database via ViewModel
        journalViewModel.updateEntry(updatedEntry)

        // Update original data to reflect saved state
        originalTitle = title
        originalContent = content

        // Exit edit mode and show success
        setEditMode(false)

        // Show success message
        com.google.android.material.snackbar.Snackbar.make(
            binding.root,
            "Diary saved successfully",
            com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
        ).show()

        // Set result to indicate data was changed - this will help refresh the journal list
        requireActivity().setResult(android.app.Activity.RESULT_OK)
    }

    private fun deleteDiary() {
        // Show confirmation dialog
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete Diary Entry")
            .setMessage("Are you sure you want to delete this diary entry?")
            .setPositiveButton("Delete") { _, _ ->
                // Create journal entry for deletion - USE ORIGINAL TIMESTAMP
                val entryToDelete = JournalEntry.Generic(
                    id = diaryId,
                    timestamp = originalTimestamp, // Use original timestamp
                    type = "diary",
                    content = if (originalTitle.isNotEmpty()) {
                        "$originalTitle\n\n$originalContent"
                    } else {
                        originalContent
                    }
                )

                // Delete from database via ViewModel
                journalViewModel.deleteEntry(entryToDelete)

                // Navigate back
                requireActivity().onBackPressed()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}