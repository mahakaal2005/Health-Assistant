package com.example.health_assistant.features.prescriptions.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ArrayAdapter
import androidx.core.os.bundleOf
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import coil3.load
import coil3.request.crossfade
import coil3.request.error
import coil3.request.placeholder
import com.example.health_assistant.R
import com.example.health_assistant.data.model.DiseaseCategory
import com.example.health_assistant.data.model.Prescription
import com.example.health_assistant.databinding.DialogPrescriptionDetailBinding
import com.example.health_assistant.features.prescriptions.viewmodel.PrescriptionsViewModel
import com.example.health_assistant.features.prescriptions.utils.PrescriptionUtils
import com.example.health_assistant.utils.ImageZoomManager
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Full-screen dialog for viewing and editing prescription details with zoomable image
 */
@AndroidEntryPoint
class PrescriptionDetailDialog : DialogFragment() {

    private var _binding: DialogPrescriptionDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PrescriptionsViewModel by viewModels()
    private lateinit var prescriptionId: String
    private var prescription: Prescription? = null
    private var isEditMode = false
    private var selectedCategory: DiseaseCategory? = null
    private val categories = DiseaseCategory.getDefaultCategories()

    @Inject
    lateinit var imageZoomManager: ImageZoomManager

    companion object {
        private const val ARG_PRESCRIPTION_ID = "prescription_id"

        fun newInstance(prescriptionId: String): PrescriptionDetailDialog {
            return PrescriptionDetailDialog().apply {
                arguments = bundleOf(ARG_PRESCRIPTION_ID to prescriptionId)
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Create a fullscreen dialog that behaves like an Activity
        val dialog = Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen)

        // Configure window for GUARANTEED keyboard handling
        dialog.window?.apply {
            // CRITICAL: Use ADJUST_PAN which moves entire window up above keyboard
            setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN or
                WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN
            )

            // Make it truly fullscreen
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )

            // Remove all flags that interfere with keyboard
            clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

            // Essential flags for proper behavior
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)

            // Set window attributes for keyboard handling
            attributes = attributes?.apply {
                softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN
                flags = flags or WindowManager.LayoutParams.FLAG_FULLSCREEN
            }
        }

        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogPrescriptionDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prescriptionId = arguments?.getString(ARG_PRESCRIPTION_ID)
            ?: throw IllegalArgumentException("Prescription ID is required")

        setupToolbar()
        setupCategoryDropdown()
        setupClickListeners()
        setupTextWatchers()
        observeViewModel()
        loadPrescriptionDetails()
    }

    private fun setupToolbar() {
        // Setup navigation (back) button
        binding.toolbar.setNavigationOnClickListener {
            if (isEditMode) {
                // If in edit mode, show confirmation dialog
                showDiscardChangesDialog()
            } else {
                dismiss()
            }
        }
    }

    private fun setupCategoryDropdown() {
        val adapter = ArrayAdapter<String>(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            categories.map { it.name }
        )
        binding.categoryDropdown.setAdapter(adapter)
        binding.categoryDropdown.setOnItemClickListener { _, _, position, _ ->
            selectedCategory = categories[position]
        }
    }

    private fun setupClickListeners() {
        // More options menu
        binding.moreOptionsButton.setOnClickListener {
            showMoreOptionsMenu()
        }

        // Edit mode buttons
        binding.saveButton.setOnClickListener {
            savePrescription()
        }

        binding.cancelButton.setOnClickListener {
            showDiscardChangesDialog()
        }
    }

    private fun setupTextWatchers() {
        binding.doctorNameEditText.addTextChangedListener {
            clearFieldError(binding.doctorNameInputLayout)
        }

        // Enhanced scrolling behavior when keyboard appears
        setupScrollingForKeyboard()
    }

    private fun setupScrollingForKeyboard() {
        // BULLETPROOF keyboard handling - manual detection and scrolling
        val globalLayoutListener = object : android.view.ViewTreeObserver.OnGlobalLayoutListener {
            private var previousKeyboardHeight = 0

            override fun onGlobalLayout() {
                if (!isEditMode) return

                val rect = android.graphics.Rect()
                binding.root.getWindowVisibleDisplayFrame(rect)

                val rootHeight = binding.root.height
                val visibleHeight = rect.height()
                val keyboardHeight = rootHeight - visibleHeight

                // Keyboard appeared or size changed
                if (keyboardHeight > 200 && keyboardHeight != previousKeyboardHeight) {
                    handleKeyboardShown(keyboardHeight, visibleHeight)
                } else if (keyboardHeight <= 200 && previousKeyboardHeight > 200) {
                    handleKeyboardHidden()
                }

                previousKeyboardHeight = keyboardHeight
            }
        }

        binding.root.viewTreeObserver.addOnGlobalLayoutListener(globalLayoutListener)
    }

    private fun handleKeyboardShown(keyboardHeight: Int, visibleHeight: Int) {
        android.util.Log.d("KeyboardDebug", "Keyboard shown: height=$keyboardHeight, visibleHeight=$visibleHeight")

        // NUCLEAR OPTION: Instead of scrolling, add bottom padding to push content up
        val scrollView = binding.scrollView
        val requiredPadding = keyboardHeight + 150 // Extra padding for safety

        android.util.Log.d("KeyboardDebug", "Setting bottom padding: $requiredPadding")

        scrollView.setPadding(
            scrollView.paddingLeft,
            scrollView.paddingTop,
            scrollView.paddingRight,
            requiredPadding
        )

        // Auto-scroll to focused field with improved logic
        val focusedView = binding.root.findFocus()
        focusedView?.let { view ->
            android.util.Log.d("KeyboardDebug", "Focused view: ${view.javaClass.simpleName}")

            // Delay to ensure padding is applied first
            scrollView.postDelayed({
                val parentInputLayout = findParentInputLayout(view)
                parentInputLayout?.let { inputLayout ->
                    // Get the input layout's position relative to scroll view content
                    val scrollViewContentTop = scrollView.scrollY
                    val inputLayoutTop = getViewTopInScrollView(inputLayout, scrollView)

                    // Calculate target scroll position to center the field in visible area
                    val availableHeight = visibleHeight - 200 // Leave some padding at top/bottom
                    val targetScrollY = inputLayoutTop - (availableHeight / 3) // Position at 1/3 from top

                    android.util.Log.d("KeyboardDebug", "Auto-scrolling to center field: $targetScrollY")

                    scrollView.smoothScrollTo(0, maxOf(0, targetScrollY))
                }
            }, 100) // Small delay to ensure padding is applied
        }
    }

    private fun getViewTopInScrollView(view: View, scrollView: androidx.core.widget.NestedScrollView): Int {
        val viewLocation = IntArray(2)
        val scrollViewLocation = IntArray(2)

        view.getLocationInWindow(viewLocation)
        scrollView.getLocationInWindow(scrollViewLocation)

        return viewLocation[1] - scrollViewLocation[1] + scrollView.scrollY
    }

    private fun handleKeyboardHidden() {
        android.util.Log.d("KeyboardDebug", "Keyboard hidden, resetting padding")

        // Reset padding when keyboard hides with smooth transition
        val scrollView = binding.scrollView
        // Use the original padding from XML (120dp converted to pixels)
        val originalPadding = (120 * resources.displayMetrics.density).toInt()

        scrollView.setPadding(
            scrollView.paddingLeft,
            scrollView.paddingTop,
            scrollView.paddingRight,
            originalPadding
        )

        // Optional: Smoothly scroll back to a reasonable position
        scrollView.postDelayed({
            if (scrollView.scrollY > 500) { // If scrolled down significantly
                scrollView.smoothScrollTo(0, 0) // Scroll back to top
            }
        }, 200)
    }

    private fun setupLegacyKeyboardHandling() {
        binding.root.viewTreeObserver.addOnGlobalLayoutListener {
            if (isEditMode) {
                val rect = android.graphics.Rect()
                binding.root.getWindowVisibleDisplayFrame(rect)
                val screenHeight = binding.root.rootView.height
                val keypadHeight = screenHeight - rect.bottom

                // Keyboard is visible if it takes more than 15% of screen
                val isKeyboardVisible = keypadHeight > screenHeight * 0.15

                if (isKeyboardVisible) {
                    handleLegacyKeyboardVisible(rect, keypadHeight)
                }
            }
        }
    }

    private fun handleLegacyKeyboardVisible(visibleRect: android.graphics.Rect, keyboardHeight: Int) {
        val focusedView = binding.root.findFocus()
        focusedView?.let { view ->
            val location = IntArray(2)
            view.getLocationOnScreen(location)
            val viewBottom = location[1] + view.height

            // If view is hidden by keyboard
            if (viewBottom > visibleRect.bottom - 50) {
                val parentInputLayout = findParentInputLayout(view)
                parentInputLayout?.let { inputLayout ->
                    // Calculate scroll position to bring input above keyboard
                    val scrollY = inputLayout.top - (visibleRect.height() - inputLayout.height - 200)
                    binding.scrollView.smoothScrollTo(0, maxOf(0, scrollY))
                }
            }
        }
    }

    private fun findParentInputLayout(view: View): com.google.android.material.textfield.TextInputLayout? {
        return when (view.id) {
            binding.doctorNameEditText.id -> binding.doctorNameInputLayout
            binding.notesEditText.id -> binding.notesInputLayout
            binding.categoryDropdown.id -> binding.categoryInputLayout
            else -> null
        }
    }

    private fun setupFieldFocusListeners() {
        // Enhanced focus listeners with immediate scrolling
        binding.doctorNameEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && isEditMode) {
                binding.root.post {
                    ensureFieldVisible(binding.doctorNameInputLayout, 200)
                }
            }
        }

        binding.categoryDropdown.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && isEditMode) {
                binding.root.post {
                    ensureFieldVisible(binding.categoryInputLayout, 180)
                }
            }
        }

        binding.notesEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && isEditMode) {
                binding.root.post {
                    ensureFieldVisible(binding.notesInputLayout, 150)
                }
            }
        }
    }

    private fun ensureFieldVisible(inputLayout: com.google.android.material.textfield.TextInputLayout, padding: Int) {
        // Get current keyboard state
        val rect = android.graphics.Rect()
        binding.root.getWindowVisibleDisplayFrame(rect)
        val screenHeight = binding.root.rootView.height
        val keyboardHeight = screenHeight - rect.bottom
        val availableHeight = rect.height()

        // Calculate optimal scroll position
        val location = IntArray(2)
        inputLayout.getLocationOnScreen(location)
        val fieldTop = location[1]
        val fieldBottom = fieldTop + inputLayout.height

        // If field would be hidden by keyboard or too close to bottom
        if (fieldBottom > availableHeight - padding) {
            val scrollY = inputLayout.top - padding
            binding.scrollView.smoothScrollTo(0, maxOf(0, scrollY))
        }
    }

    private fun showMoreOptionsMenu() {
        // Use a more reliable approach with proper positioning
        val popupMenu = androidx.appcompat.widget.PopupMenu(
            requireContext(),
            binding.moreOptionsButton
        )

        // Add menu items programmatically for better control
        popupMenu.menu.add(0, 1, 0, "Edit").setIcon(R.drawable.ic_edit)
        popupMenu.menu.add(0, 2, 0, "Delete").setIcon(R.drawable.ic_delete)
        popupMenu.menu.add(0, 3, 0, "Share").setIcon(R.drawable.ic_share)

        // Force show icons and set proper gravity
        try {
            val popupField = androidx.appcompat.widget.PopupMenu::class.java.getDeclaredField("mPopup")
            popupField.isAccessible = true
            val menuPopup = popupField.get(popupMenu)

            // Set gravity to ensure menu appears on screen
            menuPopup.javaClass.getDeclaredMethod("setGravity", Int::class.javaPrimitiveType)
                .invoke(menuPopup, android.view.Gravity.END or android.view.Gravity.TOP)

            // Show icons
            menuPopup.javaClass.getDeclaredMethod("setForceShowIcon", Boolean::class.javaPrimitiveType)
                .invoke(menuPopup, true)
        } catch (e: Exception) {
            // Fallback: manual positioning if reflection fails
            android.util.Log.d("PopupMenu", "Using fallback positioning")
        }

        // Set menu item click listener
        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                1 -> {
                    toggleEditMode()
                    true
                }
                2 -> {
                    showDeleteConfirmation()
                    true
                }
                3 -> {
                    sharePrescription()
                    true
                }
                else -> false
            }
        }

        // Show the popup menu
        popupMenu.show()
    }

    private fun toggleEditMode() {
        isEditMode = !isEditMode

        binding.apply {
            if (isEditMode) {
                // Switch to edit mode
                toolbar.title = "Edit Prescription"
                viewGroup.visibility = View.GONE
                editGroup.visibility = View.VISIBLE
                notesDetail.visibility = View.GONE
                notesInputLayout.visibility = View.VISIBLE
                moreOptionsButton.visibility = View.GONE
                editActionButtons.visibility = View.VISIBLE

                // Populate edit fields with current data
                prescription?.let { populateEditFields(it) }

                // Update window keyboard behavior for edit mode
                updateKeyboardBehaviorForEditMode(true)
            } else {
                // Switch to view mode
                toolbar.title = "Prescription Details"
                viewGroup.visibility = View.VISIBLE
                editGroup.visibility = View.GONE
                notesDetail.visibility = View.VISIBLE
                notesInputLayout.visibility = View.GONE
                moreOptionsButton.visibility = View.VISIBLE
                editActionButtons.visibility = View.GONE

                // Reset edit fields and clear any errors
                clearAllFieldErrors()

                // Update window keyboard behavior for view mode
                updateKeyboardBehaviorForEditMode(false)
            }
        }
    }

    private fun updateKeyboardBehaviorForEditMode(isEditMode: Boolean) {
        dialog?.window?.apply {
            if (isEditMode) {
                // Enhanced keyboard behavior for edit mode
                setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or
                            WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
                )

                // Ensure the window can be properly resized
                clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN)

                // Force layout update
                decorView.requestLayout()
            } else {
                // Standard behavior for view mode
                setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or
                            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
                )
            }
        }
    }

    private fun populateEditFields(prescription: Prescription) {
        binding.apply {
            doctorNameEditText.setText(prescription.doctorName)
            notesEditText.setText(prescription.notes ?: "")

            // Set category dropdown selection
            val categoryIndex = categories.indexOfFirst { it.id == prescription.categoryId }
            if (categoryIndex >= 0) {
                categoryDropdown.setText(categories[categoryIndex].name, false)
                selectedCategory = categories[categoryIndex]
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
            try {
                // Create updated prescription with correct parameters and timestamp
                val updatedPrescription = currentPrescription.copy(
                    doctorName = doctorName,
                    categoryId = category!!.id,
                    notes = notes?.takeIf { it.isNotBlank() },
                    dateModified = java.util.Date(System.currentTimeMillis())
                )

                // Update prescription
                viewModel.updatePrescription(updatedPrescription)

                // Update local prescription reference
                prescription = updatedPrescription

                // Switch back to view mode and update display
                toggleEditMode()
                displayPrescriptionDetails(updatedPrescription)

                // Show success message
                showSuccess("Prescription updated successfully")

            } catch (e: Exception) {
                showError("Error updating prescription: ${e.message}")
            }
        }
    }

    private fun validateInput(doctorName: String, category: DiseaseCategory?): Boolean {
        var isValid = true

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

    private fun showDiscardChangesDialog() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Discard Changes?")
            .setMessage("Any unsaved changes will be lost.")
            .setPositiveButton("Discard") { _, _ ->
                if (isEditMode) {
                    toggleEditMode()
                } else {
                    dismiss()
                }
            }
            .setNegativeButton("Keep Editing", null)
            .show()
    }

    private fun clearFieldError(inputLayout: com.google.android.material.textfield.TextInputLayout) {
        inputLayout.error = null
    }

    private fun clearAllFieldErrors() {
        binding.doctorNameInputLayout.error = null
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.prescriptions.collect { prescriptions ->
                // Find prescription by ID directly from the list - convert String to Long for comparison
                val prescriptionIdLong = prescriptionId.toLongOrNull()
                val prescription = prescriptions.find { it.id == prescriptionIdLong }
                prescription?.let {
                    this@PrescriptionDetailDialog.prescription = it
                    displayPrescriptionDetails(it)
                }
            }
        }
    }

    private fun loadPrescriptionDetails() {
        // Details will be loaded through the observed prescriptions flow
    }

    private fun displayPrescriptionDetails(prescription: Prescription) {
        binding.apply {
            // Load prescription image with error handling and make it clickable
            prescriptionImageDetail.load(prescription.localImagePath ?: "") {
                placeholder(R.drawable.ic_prescription_placeholder)
                error(R.drawable.ic_prescription_placeholder)
                crossfade(true)
            }

            // Make image clickable to open zoom view
            prescriptionImageDetail.setOnClickListener {
                openZoomableImage(prescription.localImagePath ?: "")
            }

            // Set prescription details
            doctorNameDetail.text = prescription.doctorName

            // Get category by ID - directly use the categoryId Long value
            val category = PrescriptionUtils.getCategoryById(prescription.categoryId)
            diseaseCategoryDetailChip.text = category?.name ?: "Unknown Category"

            dateAddedDetail.text = PrescriptionUtils.formatDate(prescription.dateAdded.time)

            // Handle modified date
            if (prescription.dateModified != prescription.dateAdded) {
                dateModifiedDetail.text = PrescriptionUtils.formatDate(prescription.dateModified.time)
                dateModifiedDetail.visibility = View.VISIBLE
            } else {
                dateModifiedDetail.visibility = View.GONE
            }

            // Show notes
            if (!prescription.notes.isNullOrBlank()) {
                notesDetail.text = prescription.notes
                notesCard.visibility = View.VISIBLE
            } else {
                notesDetail.text = "No notes added"
                notesCard.visibility = View.VISIBLE
            }
        }
    }

    private fun openZoomableImage(imagePath: String) {
        try {
            // Use the ImageZoomManager for consistent zoom experience
            imageZoomManager.showImageFullscreen(
                context = requireContext(),
                imagePath = imagePath
            )
        } catch (e: Exception) {
            android.widget.Toast.makeText(
                requireContext(),
                "Error opening image: ${e.message}",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun showDeleteConfirmation() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(R.string.prescription_delete_confirmation_title)
            .setMessage(R.string.prescription_delete_confirmation_message)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                deletePrescription()
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun deletePrescription() {
        lifecycleScope.launch {
            try {
                viewModel.deletePrescription(prescriptionId)
                dismiss()

                // Show success message
                parentFragmentManager.setFragmentResult(
                    "prescription_deleted",
                    bundleOf("message" to getString(R.string.prescription_deleted_successfully))
                )
            } catch (e: Exception) {
                // Show error message
                parentFragmentManager.setFragmentResult(
                    "prescription_error",
                    bundleOf("error" to getString(R.string.error_deleting_prescription))
                )
            }
        }
    }

    private fun sharePrescription() {
        lifecycleScope.launch {
            try {
                // Find the current prescription - convert String to Long for comparison
                val prescriptionIdLong = prescriptionId.toLongOrNull()
                val prescription = viewModel.prescriptions.value.find { it.id == prescriptionIdLong }
                prescription?.let {
                    val shareText = buildString {
                        append("Prescription Details\n")
                        append("Doctor: ${it.doctorName}\n")
                        append("Date: ${PrescriptionUtils.formatDate(it.dateAdded.time)}\n")
                        val category = PrescriptionUtils.getCategoryById(it.categoryId)
                        append("Category: ${category?.name ?: "General"}\n")
                        if (!it.notes.isNullOrBlank()) {
                            append("Notes: ${it.notes}\n")
                        }
                    }

                    val shareIntent = android.content.Intent().apply {
                        action = android.content.Intent.ACTION_SEND
                        type = "text/plain"
                        putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                        putExtra(android.content.Intent.EXTRA_SUBJECT, "Prescription from ${it.doctorName}")
                    }

                    startActivity(android.content.Intent.createChooser(shareIntent, "Share Prescription"))
                }
            } catch (e: Exception) {
                android.widget.Toast.makeText(
                    requireContext(),
                    "Error sharing prescription: ${e.message}",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun showSuccess(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    override fun onStart() {
        super.onStart()

        // Remove any layout overrides - let the dialog handle keyboard naturally
        dialog?.window?.apply {
            // Ensure keyboard behavior is maintained
            setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN or
                WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}