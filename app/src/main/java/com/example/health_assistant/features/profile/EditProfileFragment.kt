package com.example.health_assistant.features.profile

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.health_assistant.R
import com.example.health_assistant.databinding.FragmentEditProfileBinding
import com.example.health_assistant.features.profile.state.*
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Enhanced Edit Profile Fragment with modern UX patterns, accessibility, and reactive state management
 * Features: Real-time validation, comprehensive error handling, progress indicators, and smooth user experience
 */
@AndroidEntryPoint
class EditProfileFragment : Fragment() {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EditProfileViewModel by viewModels()

    // Date formatters for birthday handling
    private val isoDateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayDateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    // Image picker for profile photo with enhanced error handling
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        when (result.resultCode) {
            Activity.RESULT_OK -> {
                result.data?.data?.let { uri ->
                    viewModel.updateProfilePhoto(uri)
                } ?: showError("No image selected. Please try again.")
            }
            Activity.RESULT_CANCELED -> {
                // User cancelled, no action needed
            }
            else -> {
                showError("Failed to select image. Please check permissions and try again.")
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Always sync profile from Firestore to local storage when Edit Profile is opened
        viewModel.syncProfileOnEditOpen()

        setupUI()
        setupObservers()
        setupRealTimeValidation()

        // Load profile data
        viewModel.loadProfile()
    }

    /**
     * Setup UI components and click listeners with enhanced accessibility
     */
    private fun setupUI() {
        with(binding) {
            // Toolbar navigation
            toolbar.setNavigationOnClickListener {
                handleBackNavigation()
            }

            // Profile image click - show full screen view
            profileImageView.setOnClickListener { showFullScreenImage() }

            // Camera icon click - open image picker for changing photo
            changePhotoButton.setOnClickListener { openImagePicker() }

            // Birthday field setup
            birthdayEditText.setOnClickListener { showDatePicker() }

            // Gender dropdown setup
            setupGenderDropdown()

            // Toolbar save button
            toolbarSaveButton.setOnClickListener {
                viewModel.saveProfile()
            }
        }
    }

    /**
     * Setup comprehensive state observers for reactive UI updates
     */
    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            // Main UI state
            viewModel.uiState.collect { state ->
                handleUiState(state)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            // Save operation state
            viewModel.saveState.collect { state ->
                handleSaveState(state)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            // Photo upload state
            viewModel.photoUploadState.collect { state ->
                handlePhotoUploadState(state)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            // Form state for save button enablement
            viewModel.formState.collect { state ->
                updateSaveButtonState(state)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            // Loading states
            viewModel.loadingState.collect { state ->
                handleLoadingState(state)
            }
        }

        // Individual field validation observers
        observeFieldValidation()

        // Current form data observers
        observeFormData()
    }

    /**
     * Setup real-time validation for all form fields
     */
    private fun setupRealTimeValidation() {
        with(binding) {
            // Display name validation
            displayNameEditText.addTextChangedListener { text ->
                val displayName = text?.toString() ?: ""
                viewModel.updateDisplayName(displayName)
            }

            // Bio validation - Now enabled with ViewModel support
            bioEditText.addTextChangedListener { text ->
                val bio = text?.toString()?.takeIf { it.isNotBlank() }
                viewModel.updateBio(bio)
            }

            // Height validation
            heightEditText.addTextChangedListener { text ->
                val height = text?.toString()?.takeIf { it.isNotBlank() }
                viewModel.updateHeight(height)
            }

            // Weight validation
            weightEditText.addTextChangedListener { text ->
                val weight = text?.toString()?.takeIf { it.isNotBlank() }
                viewModel.updateWeight(weight)
            }
        }
    }

    /**
     * Handle main UI state changes
     */
    private fun handleUiState(state: EditProfileUiState) {
        when (state) {
            is EditProfileUiState.Initial -> {
                binding.progressOverlay.visibility = View.VISIBLE
            }
            is EditProfileUiState.Loading -> {
                binding.progressOverlay.visibility = View.VISIBLE
                binding.progressText.text = getString(R.string.loading_profile)
            }
            is EditProfileUiState.Success -> {
                binding.progressOverlay.visibility = View.GONE
                populateFields(state.profile)
            }
            is EditProfileUiState.Error -> {
                binding.progressOverlay.visibility = View.GONE
                showError(state.message)

                // If profile not found locally, try to sync from Firestore
                if (state.message.contains("Profile not found", ignoreCase = true)) {
                    forceSyncProfileFromRemote()
                }

                // Show retry option based on error cause
                when (state.cause) {
                    ErrorCause.NETWORK -> {
                        showRetrySnackbar("Network error. Tap to retry.") {
                            viewModel.loadProfile()
                        }
                    }
                    else -> {
                        showError(state.message)
                    }
                }
            }
        }
    }

    /**
     * Force sync profile from Firestore if local data is missing
     */
    private fun forceSyncProfileFromRemote() {
        viewLifecycleOwner.lifecycleScope.launch {
            val userId = viewModel.getCurrentUserId()
            if (!userId.isNullOrBlank()) {
                viewModel.syncProfileFromRemote(userId)
            }
        }
    }

    /**
     * Handle save operation state changes
     */
    private fun handleSaveState(state: SaveOperationState) {
        when (state) {
            is SaveOperationState.Idle -> {
                // Normal state
            }
            is SaveOperationState.Saving -> {
                binding.progressOverlay.visibility = View.VISIBLE
                binding.progressText.text = getString(R.string.saving_profile)
                binding.toolbarSaveButton.isEnabled = false
            }
            is SaveOperationState.Success -> {
                binding.progressOverlay.visibility = View.GONE
                binding.toolbarSaveButton.isEnabled = true
                showSuccess(getString(R.string.profile_saved_successfully))

                // Navigate back after short delay
                binding.root.postDelayed({
                    findNavController().navigateUp()
                }, 1000)
            }
            is SaveOperationState.Error -> {
                binding.progressOverlay.visibility = View.GONE
                binding.toolbarSaveButton.isEnabled = true

                when (state.cause) {
                    SaveErrorCause.VALIDATION -> {
                        showError(state.message)
                        // Focus on first error field
                        scrollToFirstError()
                    }
                    SaveErrorCause.NETWORK -> {
                        showRetrySnackbar("Network error. Tap to retry saving.") {
                            viewModel.saveProfile()
                        }
                    }
                    else -> {
                        showError(state.message)
                    }
                }
            }
        }
    }

    /**
     * Handle photo upload state changes
     */
    private fun handlePhotoUploadState(state: PhotoUploadState) {
        when (state) {
            is PhotoUploadState.Idle -> {
                binding.photoUploadProgress.visibility = View.GONE
            }
            is PhotoUploadState.Uploading -> {
                binding.photoUploadProgress.visibility = View.VISIBLE
            }
            is PhotoUploadState.Success -> {
                binding.photoUploadProgress.visibility = View.GONE
                // Load the image (will be enhanced with Room integration)
                loadProfileImage(state.photoUrl)
            }
            is PhotoUploadState.Error -> {
                binding.photoUploadProgress.visibility = View.GONE
                if (state.retryable) {
                    showRetrySnackbar("Photo upload failed. Tap to retry.") {
                        openImagePicker()
                    }
                } else {
                    showError(state.message)
                }
            }
        }
    }

    /**
     * Observe individual field validation states
     */
    private fun observeFieldValidation() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.displayNameValidation.collect { state ->
                updateFieldValidation(binding.displayNameInputLayout, state)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.bioValidation.collect { state ->
                updateFieldValidation(binding.bioInputLayout, state)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.birthdayValidation.collect { state ->
                updateFieldValidation(binding.birthdayInputLayout, state)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.genderValidation.collect { state ->
                updateFieldValidation(binding.genderInputLayout, state)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.heightValidation.collect { state ->
                updateFieldValidation(binding.heightInputLayout, state)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.weightValidation.collect { state ->
                updateFieldValidation(binding.weightInputLayout, state)
            }
        }
    }

    /**
     * Observe current form data for UI updates
     */
    private fun observeFormData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentDisplayName.collect { displayName ->
                if (binding.displayNameEditText.text.toString() != displayName) {
                    binding.displayNameEditText.setText(displayName)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentBio.collect { bio ->
                if (binding.bioEditText.text.toString() != (bio ?: "")) {
                    binding.bioEditText.setText(bio ?: "")
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentBirthday.collect { birthday ->
                val displayText = birthday?.let { iso ->
                    try {
                        val date = isoDateFormatter.parse(iso)
                        date?.let { displayDateFormatter.format(it) } ?: iso
                    } catch (e: Exception) {
                        iso
                    }
                } ?: ""

                if (binding.birthdayEditText.text.toString() != displayText) {
                    binding.birthdayEditText.setText(displayText)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentGender.collect { gender ->
                if (binding.genderDropdown.text.toString() != (gender ?: "")) {
                    binding.genderDropdown.setText(gender ?: "", false)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentHeight.collect { height ->
                if (binding.heightEditText.text.toString() != (height ?: "")) {
                    binding.heightEditText.setText(height ?: "")
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentWeight.collect { weight ->
                if (binding.weightEditText.text.toString() != (weight ?: "")) {
                    binding.weightEditText.setText(weight ?: "")
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentPhotoUrl.collect { photoUrl ->
                loadProfileImage(photoUrl)
            }
        }
    }

    /**
     * Update field validation UI based on validation state
     */
    private fun updateFieldValidation(inputLayout: TextInputLayout, state: FieldValidationState) {
        when (state) {
            is FieldValidationState.Valid -> {
                inputLayout.error = null
                inputLayout.isErrorEnabled = false
            }
            is FieldValidationState.Invalid -> {
                inputLayout.error = state.errorMessage
                inputLayout.isErrorEnabled = true
            }
            is FieldValidationState.Error -> {
                inputLayout.error = state.errorMessage
                inputLayout.isErrorEnabled = true
            }
            is FieldValidationState.Idle -> {
                inputLayout.error = null
                inputLayout.isErrorEnabled = false
            }
            is FieldValidationState.Validating -> {
                // Show validation in progress if needed
                inputLayout.error = null
            }
        }
    }

    /**
     * Update save button state based on form validation
     */
    private fun updateSaveButtonState(state: FormState) {
        binding.toolbarSaveButton.isEnabled = state.isValid && state.hasChanges

        // Visual feedback for save button state
        binding.toolbarSaveButton.alpha = if (state.isValid && state.hasChanges) 1.0f else 0.6f
    }

    /**
     * Handle loading states with proper UI feedback
     */
    private fun handleLoadingState(state: LoadingState) {
        with(binding) {
            when {
                state.isLoadingProfile -> {
                    progressOverlay.visibility = View.VISIBLE
                    progressText.text = getString(R.string.loading_profile)
                }
                state.isSaving -> {
                    progressOverlay.visibility = View.VISIBLE
                    progressText.text = getString(R.string.saving_changes)
                }
                state.isUploadingPhoto -> {
                    photoUploadProgress.visibility = View.VISIBLE
                }
                state.isValidating -> {
                    // Subtle validation feedback if needed
                }
                else -> {
                    progressOverlay.visibility = View.GONE
                    photoUploadProgress.visibility = View.GONE
                }
            }
        }
    }

    /**
     * Handle back navigation with unsaved changes check
     */
    private fun handleBackNavigation() {
        if (viewModel.hasUnsavedChanges()) {
            showUnsavedChangesDialog()
        } else {
            findNavController().navigateUp()
        }
    }

    /**
     * Show dialog for unsaved changes confirmation
     */
    private fun showUnsavedChangesDialog() {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("Unsaved Changes")
            .setMessage("You have unsaved changes. Are you sure you want to leave?")
            .setPositiveButton("Leave") { _, _ ->
                findNavController().navigateUp()
            }
            .setNegativeButton("Stay", null)
            .show()
    }

    /**
     * Scroll to first error field for better UX
     */
    private fun scrollToFirstError() {
        binding.scrollView.post {
            val errorFields = listOf(
                binding.displayNameInputLayout to binding.displayNameInputLayout.error,
                binding.bioInputLayout to binding.bioInputLayout.error,
                binding.birthdayInputLayout to binding.birthdayInputLayout.error,
                binding.genderInputLayout to binding.genderInputLayout.error,
                binding.heightInputLayout to binding.heightInputLayout.error,
                binding.weightInputLayout to binding.weightInputLayout.error
            )

            val firstErrorField = errorFields.firstOrNull { it.second != null }?.first
            firstErrorField?.let { field ->
                val location = IntArray(2)
                field.getLocationOnScreen(location)
                binding.scrollView.smoothScrollTo(0, location[1] - 200) // Offset for better visibility

                // Request focus and show keyboard if it's an editable field
                field.editText?.requestFocus()
            }
        }
    }

    /**
     * Load and display profile image with fallback handling
     */
    private fun loadProfileImage(photoUrl: String?) {
        if (photoUrl.isNullOrBlank()) {
            // Show default avatar
            binding.profileImageView.setImageResource(R.drawable.ic_person)
            return
        }

        try {
            // For now, basic URI handling - will be enhanced with Room integration
            val uri = Uri.parse(photoUrl)
            binding.profileImageView.setImageURI(uri)
        } catch (e: Exception) {
            // Fallback to default image
            binding.profileImageView.setImageResource(R.drawable.ic_person)
            android.util.Log.w("EditProfile", "Failed to load profile image: ${e.message}")
        }
    }

    /**
     * Setup gender dropdown with accessibility support
     */
    private fun setupGenderDropdown() {
        val genderOptions = Gender.getAllOptions()
        val adapter = ArrayAdapter(requireContext(), R.layout.list_item, genderOptions)
        binding.genderDropdown.setAdapter(adapter)

        binding.genderDropdown.setOnItemClickListener { _, _, position, _ ->
            val selectedGender = genderOptions[position]
            viewModel.updateGender(selectedGender)
        }

        // Accessibility support
        binding.genderDropdown.contentDescription = "Select your gender from the available options"
    }

    /**
     * Populate form fields from profile data, with fallback to clear fields if data is missing
     */
    private fun populateFields(profile: ProfileData?) {
        with(binding) {
            if (profile == null) {
                // Clear all fields if profile is missing
                emailTextView.text = ""
                displayNameEditText.setText("")
                bioEditText.setText("")
                birthdayEditText.setText("")
                birthdayEditText.tag = null
                genderDropdown.setText("", false)
                heightEditText.setText("")
                weightEditText.setText("")
                loadProfileImage(null)
                return
            }

            // Email (read-only)
            emailTextView.text = profile.email

            // Display name
            if (displayNameEditText.text.toString() != profile.displayName) {
                displayNameEditText.setText(profile.displayName)
            }

            // Bio
            profile.bio?.let { bio ->
                if (bioEditText.text.toString() != bio) {
                    bioEditText.setText(bio)
                }
            } ?: run {
                bioEditText.setText("")
            }

            // Birthday
            profile.birthday?.let { birthdayIso ->
                try {
                    val date = isoDateFormatter.parse(birthdayIso)
                    date?.let {
                        val displayDate = displayDateFormatter.format(it)
                        if (birthdayEditText.text.toString() != displayDate) {
                            birthdayEditText.setText(displayDate)
                            birthdayEditText.tag = birthdayIso // Store ISO format
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.w("EditProfile", "Failed to parse birthday: ${e.message}")
                }
            }

            // Gender
            profile.gender?.displayName?.let { genderName ->
                if (genderDropdown.text.toString() != genderName) {
                    genderDropdown.setText(genderName, false)
                }
            }

            // Height
            profile.height?.let { height ->
                val heightStr = height.toString()
                if (heightEditText.text.toString() != heightStr) {
                    heightEditText.setText(heightStr)
                }
            }

            // Weight
            profile.weight?.let { weight ->
                val weightStr = weight.toString()
                if (weightEditText.text.toString() != weightStr) {
                    weightEditText.setText(weightStr)
                }
            }

            // Profile photo
            loadProfileImage(profile.photoUrl)
        }
    }

    /**
     * Enhanced date picker with better UX
     */
    private fun showDatePicker() {
        // Calculate reasonable date bounds
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val maxDate = calendar.timeInMillis

        calendar.set(currentYear - 120, 0, 1) // 120 years ago
        val minDate = calendar.timeInMillis

        // Default to 25 years ago if no current birthday
        calendar.set(currentYear - 25, 0, 1)
        val defaultDate = calendar.timeInMillis

        val constraints = CalendarConstraints.Builder()
            .setStart(minDate)
            .setEnd(maxDate)
            .build()

        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select your birthday")
            .setSelection(defaultDate)
            .setCalendarConstraints(constraints)
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            val selectedDate = Date(selection)
            val isoDate = isoDateFormatter.format(selectedDate)
            val displayDate = displayDateFormatter.format(selectedDate)

            // Update UI
            binding.birthdayEditText.setText(displayDate)

            // Update ViewModel
            viewModel.updateBirthday(displayDate, isoDate)
        }

        datePicker.addOnDismissListener {
            // Clear focus from birthday field
            binding.birthdayEditText.clearFocus()
        }

        datePicker.show(parentFragmentManager, "BIRTHDAY_PICKER")
    }

    /**
     * Enhanced image picker with better error handling and accessibility
     */
    private fun openImagePicker() {
        try {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/jpeg", "image/png", "image/webp"))
            }

            if (intent.resolveActivity(requireContext().packageManager) != null) {
                imagePickerLauncher.launch(intent)
            } else {
                showError("No image picker app found. Please install a gallery app.")
            }
        } catch (e: Exception) {
            showError("Failed to open image picker. Please try again.")
            android.util.Log.e("EditProfile", "Failed to open image picker", e)
        }
    }

    /**
     * Show the profile image in full screen with zoom functionality
     */
    private fun showFullScreenImage() {
        // Get the current image from the profile image view
        val drawable = binding.profileImageView.drawable
        if (drawable == null) {
            showError("No image to display")
            return
        }

        // Create and show full screen image dialog
        val dialog = android.app.Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val dialogBinding = com.example.health_assistant.databinding.DialogFullscreenImageBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        // Set the image
        dialogBinding.fullscreenImage.setImageDrawable(drawable)

        // Show instructions for a few seconds
        dialogBinding.instructionsText.visibility = View.VISIBLE
        dialogBinding.instructionsText.postDelayed({
            if (dialog.isShowing) {
                dialogBinding.instructionsText.animate()
                    .alpha(0f)
                    .setDuration(500)
                    .withEndAction {
                        dialogBinding.instructionsText.visibility = View.GONE
                    }
                    .start()
            }
        }, 3000)

        // Set up close button
        dialogBinding.closeButton.setOnClickListener {
            dialog.dismiss()
        }

        // Close on tap (the ZoomableImageView will handle single tap)
        dialogBinding.fullscreenImage.setOnClickListener {
            dialog.dismiss()
        }

        // Close on background tap
        dialogBinding.root.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    private fun showSuccess(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    private fun showRetrySnackbar(message: String, retryAction: () -> Unit) {
        val snackbar = Snackbar.make(binding.root, message, Snackbar.LENGTH_INDEFINITE)
        snackbar.setAction("RETRY") {
            retryAction()
            snackbar.dismiss()
        }
        snackbar.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}