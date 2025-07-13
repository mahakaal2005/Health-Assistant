package com.example.health_assistant.features.journal.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.health_assistant.R
import com.example.health_assistant.databinding.FragmentNoteDetailBinding
import com.example.health_assistant.features.journal.domain.JournalEntry
import com.example.health_assistant.features.journal.JournalViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NoteDetailFragment : Fragment() {
    private var _binding: FragmentNoteDetailBinding? = null
    private val binding get() = _binding!!

    // Edit mode state
    private var isEditMode = false
    private var isCreateMode = false // Add creation mode flag
    private var noteId: Long = 0L
    private var originalTimestamp: Long = 0L // Add original timestamp
    private var originalContent: String = ""

    // Add ViewModel to handle database operations
    private val journalViewModel: JournalViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNoteDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupToolbar()
        setupClickListeners()
        loadNoteData()
    }

    private fun setupUI() {
        // Initially set to read-only mode
        setEditMode(false)
    }

    private fun setupToolbar() {
        // Setup the toolbar with fixed title
        binding.toolbar.title = "Note"
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
        // Create popup menu
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
                    saveNote()
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
                    deleteNote()
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
                loadNoteData()
                setEditMode(false)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun loadNoteData() {
        // Get data from arguments
        arguments?.let { bundle ->
            isCreateMode = bundle.getBoolean("isCreateMode", false)
            val content = bundle.getString("noteContent", "")
            noteId = bundle.getLong("noteId", 0L)
            originalTimestamp = bundle.getLong("noteTimestamp", System.currentTimeMillis())

            if (isCreateMode) {
                // Setup for creation mode
                setEditMode(true)
                binding.toolbar.title = "Create Note"
                binding.etNoteContent.setText("") // Start with empty content
                originalContent = ""

                // Request focus for immediate typing
                binding.etNoteContent.requestFocus()
            } else {
                // Setup for edit mode of existing note
                originalContent = content
                binding.etNoteContent.setText(content)
                setEditMode(false) // Start in view mode for existing notes
            }
        }
    }

    private fun setEditMode(editMode: Boolean) {
        isEditMode = editMode

        binding.apply {
            if (editMode) {
                // Switch to edit mode
                toolbar.title = "Edit Note"

                // Enable EditText field for editing
                etNoteContent.isEnabled = true

                // Ensure it is focusable and clickable
                etNoteContent.isFocusable = true
                etNoteContent.isFocusableInTouchMode = true

                // Show cursor
                etNoteContent.isCursorVisible = true

                // Update keyboard behavior for edit mode
                updateKeyboardBehaviorForEditMode(true)

                // Request focus on content field
                etNoteContent.requestFocus()
                etNoteContent.post {
                    val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                    imm.showSoftInput(etNoteContent, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
                }

            } else {
                // Switch to view mode
                toolbar.title = "Note"

                // Disable EditText field for read-only mode
                etNoteContent.isEnabled = false

                // Make it non-focusable
                etNoteContent.isFocusable = false

                // Hide cursor
                etNoteContent.isCursorVisible = false

                // Clear focus and hide keyboard
                etNoteContent.clearFocus()

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

    private fun saveNote() {
        val content = binding.etNoteContent.text.toString().trim()

        if (content.isEmpty()) {
            binding.etNoteContent.error = "Note content is required"
            return
        }

        val entry = if (isCreateMode) {
            // Create new entry
            JournalEntry.Generic(
                id = 0L, // Database will assign ID
                timestamp = System.currentTimeMillis(),
                type = "note",
                content = content
            )
        } else {
            // Update existing entry - preserve original timestamp
            JournalEntry.Generic(
                id = noteId,
                timestamp = originalTimestamp,
                type = "note",
                content = content
            )
        }

        // Save the entry via ViewModel
        if (isCreateMode) {
            journalViewModel.addEntry(entry)
        } else {
            journalViewModel.updateEntry(entry)
        }

        // Update state and UI
        originalContent = content

        if (isCreateMode) {
            // For creation, navigate back to journal
            requireActivity().setResult(android.app.Activity.RESULT_OK)
            requireActivity().onBackPressed()
        } else {
            // For editing, exit edit mode
            setEditMode(false)
        }

        // Show success message
        val message = if (isCreateMode) "Note created successfully" else "Note saved successfully"
        com.google.android.material.snackbar.Snackbar.make(
            binding.root,
            message,
            com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
        ).show()
    }

    private fun deleteNote() {
        // Show confirmation dialog
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete Note")
            .setMessage("Are you sure you want to delete this note?")
            .setPositiveButton("Delete") { _, _ ->
                // Create journal entry for deletion - USE ORIGINAL TIMESTAMP
                val entryToDelete = JournalEntry.Generic(
                    id = noteId,
                    timestamp = originalTimestamp, // Use original timestamp
                    type = "note",
                    content = originalContent
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