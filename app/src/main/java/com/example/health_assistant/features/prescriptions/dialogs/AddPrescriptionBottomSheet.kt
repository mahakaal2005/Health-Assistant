package com.example.health_assistant.features.prescriptions.dialogs

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import coil3.load
import coil3.request.crossfade
import com.example.health_assistant.R
import com.example.health_assistant.auth.session.SessionManager
import com.example.health_assistant.core.util.Result
import com.example.health_assistant.data.model.DiseaseCategory
import com.example.health_assistant.data.repository.interfaces.PrescriptionRepository
import com.example.health_assistant.databinding.BottomSheetAddPrescriptionBinding
import com.example.health_assistant.features.prescriptions.camera.CameraManager
import com.example.health_assistant.features.prescriptions.camera.CameraCaptureFragment
import com.example.health_assistant.features.prescriptions.utils.FileManager
import com.example.health_assistant.features.prescriptions.utils.PrescriptionUtils
import com.example.health_assistant.features.prescriptions.utils.PrescriptionValidationResult
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/**
 * Bottom sheet dialog for adding new prescriptions
 * Handles photo capture, form input, and validation
 */
@AndroidEntryPoint
class AddPrescriptionBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetAddPrescriptionBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var fileManager: FileManager

    @Inject
    lateinit var prescriptionRepository: PrescriptionRepository

    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var categoryManager: com.example.health_assistant.data.manager.CategoryManager

    private lateinit var cameraManager: CameraManager
    private var capturedImageUri: Uri? = null
    private var selectedCategory: String? = null

    // Camera permission launcher
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            capturePhoto()
        } else {
            showError(getString(R.string.prescription_error_camera_permission))
        }
    }

    // Photo capture launcher
    private val photoCaptureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        try {
            if (success && capturedImageUri != null) {
                showPhotoPreview(capturedImageUri!!)
            } else {
                // Handle the case where photo capture failed or was cancelled
                showError("Photo capture was cancelled or failed")
                capturedImageUri = null
            }
        } catch (e: Exception) {
            showError("Error processing captured photo: ${e.message}")
            capturedImageUri = null
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetAddPrescriptionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cameraManager = CameraManager(requireContext())

        // Configure keyboard handling for better UX
        setupKeyboardHandling()

        setupUI()
        setupCategoryDropdown()
        setupClickListeners()
        setupTextWatchers()
        setupFragmentResultListeners()
    }

    private fun setupKeyboardHandling() {
        // Configure the bottom sheet dialog for proper keyboard adjustment
        dialog?.window?.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or
            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE
        )

        // Enhanced scrolling behavior when keyboard appears
        setupScrollingForKeyboard()
    }

    private fun setupScrollingForKeyboard() {
        // Enhanced auto-scroll behavior with better timing and positioning
        binding.notesEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                // Use a longer delay to ensure keyboard is fully shown and layout adjusted
                binding.root.postDelayed({
                    // Calculate optimal scroll position to show notes field and buttons
                    val notesLocation = IntArray(2)
                    binding.notesEditText.getLocationOnScreen(notesLocation)

                    // Scroll to ensure the notes field and save button are visible
                    binding.root.smoothScrollTo(0, binding.notesInputLayout.top - 50)
                }, 400)
            }
        }

        // Improved doctor name field scrolling
        binding.doctorNameEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.root.postDelayed({
                    // Scroll to show doctor name field optimally
                    binding.root.smoothScrollTo(0, binding.doctorNameInputLayout.top - 100)
                }, 250)
            }
        }

        // Add scroll behavior for category dropdown
        binding.diseaseCategoryDropdown.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.root.postDelayed({
                    binding.root.smoothScrollTo(0, binding.categoryInputLayout.top - 80)
                }, 200)
            }
        }

        // Enhanced scrolling for better UX
        setupAdvancedScrollBehavior()
    }

    private fun setupAdvancedScrollBehavior() {
        // Enable smooth scrolling with better responsiveness
        binding.root.apply {
            isScrollContainer = true
            isFocusableInTouchMode = true
            descendantFocusability = ViewGroup.FOCUS_BEFORE_DESCENDANTS

            // Add scroll listener to handle dynamic adjustments
            setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
                // Optional: Add any dynamic behavior based on scroll position
                val scrollDelta = scrollY - oldScrollY

                // Ensure smooth scrolling performance
                if (kotlin.math.abs(scrollDelta) > 50) {
                    // Large scroll detected, ensure smooth animation
                    post {
                        smoothScrollBy(0, 0) // This helps with scroll momentum
                    }
                }
            }
        }
    }

    private fun setupUI() {
        // Initially disable save button
        binding.saveButton.isEnabled = false

        // Hide photo preview initially
        binding.photoPreview.visibility = View.GONE
        binding.retakePhotoButton.visibility = View.GONE
    }

    private fun setupCategoryDropdown() {
        // Get categories from the dynamic CategoryManager
        val categories = categoryManager.getCategoriesForDropdown()

        val adapter = ArrayAdapter<String>(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            categories
        )

        binding.diseaseCategoryDropdown.setAdapter(adapter)
        binding.diseaseCategoryDropdown.setOnItemClickListener { _, _, position, _ ->
            val selectedCategoryName = categories[position]

            if (selectedCategoryName == "➕ Add Custom Category...") {
                // Show custom category input dialog
                showCustomCategoryDialog()
            } else {
                // Regular category selection
                selectedCategory = selectedCategoryName
                validateForm()
            }
        }
    }

    /**
     * Show dialog for adding custom category
     */
    private fun showCustomCategoryDialog() {
        val editText = android.widget.EditText(requireContext())
        editText.hint = "Enter category name"

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Add Custom Category")
            .setMessage("Enter a new category name for your prescription:")
            .setView(editText)
            .setPositiveButton("Add") { _, _ ->
                val customCategory = editText.text.toString().trim()
                if (customCategory.isNotBlank()) {
                    val added = categoryManager.addCustomCategory(customCategory)
                    if (added) {
                        selectedCategory = customCategory
                        binding.diseaseCategoryDropdown.setText(customCategory, false)

                        // Refresh the dropdown with new category
                        setupCategoryDropdown()
                        validateForm()

                        showSuccess("Category '$customCategory' added successfully!")
                    } else {
                        showError("Category already exists or invalid name")
                    }
                } else {
                    showError("Please enter a valid category name")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupClickListeners() {
        // Photo capture
        binding.capturePhotoPlaceholder.setOnClickListener {
            requestCameraPermissionAndCapture()
        }

        // Retake photo
        binding.retakePhotoButton.setOnClickListener {
            requestCameraPermissionAndCapture()
        }

        // Cancel button
        binding.cancelButton.setOnClickListener {
            dismiss()
        }

        // Save button
        binding.saveButton.setOnClickListener {
            savePrescription()
        }
    }

    private fun setupTextWatchers() {
        binding.doctorNameEditText.addTextChangedListener {
            validateForm()
        }
    }

    private fun setupFragmentResultListeners() {
        // Listen for camera capture results
        childFragmentManager.setFragmentResultListener("camera_capture_result", this) { _, result ->
            val capturedImageUriString = result.getString("captured_image_uri")
            capturedImageUriString?.let { uriString ->
                val uri = Uri.parse(uriString)
                capturedImageUri = uri
                showPhotoPreview(uri)
            }
        }
    }

    private fun requestCameraPermissionAndCapture() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                openCameraCapture()
            }
            else -> {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun openCameraCapture() {
        // Since CameraCaptureFragment is a regular Fragment, not a DialogFragment,
        // we need to navigate to it differently. For now, let's use the old camera approach
        // or create a simple camera dialog approach
        capturePhotoUsingManager()
    }

    private fun capturePhotoUsingManager() {
        lifecycleScope.launch {
            try {
                // First, we need to setup camera before capturing
                // Since we don't have a PreviewView in the bottom sheet,
                // let's use the simpler approach with TakePicture contract
                capturePhotoWithContract()
            } catch (e: Exception) {
                showError("Camera error: ${e.message}")
            }
        }
    }

    private fun capturePhotoWithContract() {
        try {
            // Create a temporary file for the photo
            val photoFile = createTempImageFile()
            val photoUri = androidx.core.content.FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                photoFile
            )

            // Store the URI so we can access it in the callback
            capturedImageUri = photoUri

            // Launch the camera
            photoCaptureLauncher.launch(photoUri)
        } catch (e: Exception) {
            showError("Failed to start camera: ${e.message}")
        }
    }

    private fun createTempImageFile(): File {
        val timeStamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
        val imageFileName = "PRESCRIPTION_${timeStamp}.jpg"

        // Create prescriptions directory if it doesn't exist
        val prescriptionsDir = File(requireContext().filesDir, "prescriptions")
        if (!prescriptionsDir.exists()) {
            prescriptionsDir.mkdirs()
        }

        return File(prescriptionsDir, imageFileName)
    }

    private fun capturePhoto() {
        // Use the camera manager directly for now
        capturePhotoUsingManager()
    }

    private fun showPhotoPreview(uri: Uri) {
        try {
            // For FileProvider URIs, we don't need to check file existence manually
            // The URI itself is valid if it was created successfully
            binding.photoPreview.apply {
                visibility = View.VISIBLE
                load(uri) {
                    crossfade(true)
                }
            }

            binding.capturePhotoPlaceholder.visibility = View.GONE
            binding.retakePhotoButton.visibility = View.VISIBLE

            validateForm()
        } catch (e: Exception) {
            showError("Error displaying photo: ${e.message}")
        }
    }

    private fun validateForm() {
        val doctorName = binding.doctorNameEditText.text?.toString()?.trim() ?: ""
        val hasPhoto = capturedImageUri != null
        val hasCategory = selectedCategory != null

        // Simple validation - just check if required fields are filled
        binding.saveButton.isEnabled = doctorName.isNotBlank() && hasPhoto && hasCategory
    }

    private fun savePrescription() {
        val doctorName = binding.doctorNameEditText.text?.toString()?.trim() ?: ""
        val notes = binding.notesEditText.text?.toString()?.trim()
        val imageUri = capturedImageUri ?: return
        val categoryName = selectedCategory ?: return

        lifecycleScope.launch {
            try {
                // Get current user ID from session manager
                val currentUserId = sessionManager.getCurrentUserId()
                if (currentUserId.isNullOrEmpty()) {
                    showError("User not logged in. Please log in to save prescriptions.")
                    return@launch
                }

                // Save and compress image
                val result = fileManager.saveAndCompressImage(imageUri)

                if (result.isSuccess) {
                    val savedPath = result.getOrNull() ?: ""

                    // Create prescription object using simplified approach
                    val prescription = com.example.health_assistant.data.model.Prescription(
                        medicationName = "Medication", // Default or prompt user for this
                        dosage = "As prescribed", // Default or prompt user for this
                        frequency = "Daily", // Default or prompt user for this
                        startDate = java.util.Date(),
                        endDate = null,
                        instructions = notes,
                        doctorName = doctorName,
                        isActive = true,
                        userId = currentUserId,
                        categoryId = categoryName.hashCode().toLong(), // Keep for backwards compatibility
                        displayName = categoryName, // Store the actual category name in displayName field
                        notes = notes,
                        imageUri = imageUri.toString(),
                        localImagePath = savedPath
                    )

                    // Save to repository
                    val saveResult = prescriptionRepository.insertPrescription(prescription)
                    if (saveResult.isSuccess) {
                        // Notify parent fragment about successful save
                        parentFragmentManager.setFragmentResult(
                            "prescription_added",
                            bundleOf("success" to true)
                        )

                        showSuccess("Prescription saved successfully")
                        dismiss()
                    } else {
                        val errorMessage = if (saveResult is Result.Error) {
                            saveResult.message
                        } else {
                            "Unknown error"
                        }
                        showError("Failed to save prescription: $errorMessage")
                    }
                } else {
                    // Handle Kotlin standard Result failure properly
                    val exception = result.exceptionOrNull()
                    val errorMessage = exception?.message ?: "Failed to save image"
                    showError("Failed to save prescription: $errorMessage")
                }
            } catch (e: Exception) {
                showError("Error saving prescription: ${e.message}")
            }
        }
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    private fun showSuccess(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraManager.releaseCamera()
        _binding = null
    }

    companion object {
        const val TAG = "AddPrescriptionBottomSheet"

        fun newInstance(): AddPrescriptionBottomSheet {
            return AddPrescriptionBottomSheet()
        }
    }
}