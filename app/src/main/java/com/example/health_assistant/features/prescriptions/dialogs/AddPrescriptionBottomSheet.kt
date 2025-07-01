package com.example.health_assistant.features.prescriptions.dialogs

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
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

    private lateinit var cameraManager: CameraManager
    private var capturedImageUri: Uri? = null
    private var selectedCategory: DiseaseCategory? = null

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

        setupUI()
        setupCategoryDropdown()
        setupClickListeners()
        setupTextWatchers()
        setupFragmentResultListeners()
    }

    private fun setupUI() {
        // Initially disable save button
        binding.saveButton.isEnabled = false

        // Hide photo preview initially
        binding.photoPreview.visibility = View.GONE
        binding.retakePhotoButton.visibility = View.GONE
    }

    private fun setupCategoryDropdown() {
        val categories = DiseaseCategory.getDefaultCategories()
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            categories.map { it.displayName }
        )

        binding.diseaseCategoryDropdown.setAdapter(adapter)
        binding.diseaseCategoryDropdown.setOnItemClickListener { _, _, position, _ ->
            selectedCategory = categories[position]
            validateForm()
        }
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

        val validationResult = PrescriptionUtils.validatePrescription(
            doctorName = doctorName,
            diseaseCategory = selectedCategory,
            imageUri = capturedImageUri?.toString()
        )

        binding.saveButton.isEnabled = validationResult is PrescriptionValidationResult.Valid
    }

    private fun savePrescription() {
        val doctorName = binding.doctorNameEditText.text?.toString()?.trim() ?: ""
        val notes = binding.notesEditText.text?.toString()?.trim()
        val imageUri = capturedImageUri ?: return
        val category = selectedCategory ?: return

        lifecycleScope.launch {
            try {
                // Get current user ID from session manager
                val currentUserId = sessionManager.getCurrentUserId()
                if (currentUserId.isNullOrEmpty()) {
                    showError("User not logged in. Please log in to save prescriptions.")
                    return@launch
                }

                // Validate that the category exists in the database
                val categoryExists = prescriptionRepository.categoryExists(category.id)
                if (!categoryExists) {
                    showError("Invalid category selected. Please select a valid category.")
                    return@launch
                }

                // Save and compress image
                val result = fileManager.saveAndCompressImage(imageUri)

                if (result.isSuccess) {
                    val savedPath = result.getOrNull() ?: ""
                    // Create prescription object with valid user ID and category ID
                    val prescription = PrescriptionUtils.createPrescription(
                        imageUri = imageUri.toString(),
                        localImagePath = savedPath,
                        doctorName = doctorName,
                        diseaseCategory = category,
                        notes = notes?.takeIf { it.isNotBlank() },
                        userId = currentUserId // Use actual user ID from session
                    )

                    // Save to repository
                    val saveResult = prescriptionRepository.insertPrescription(prescription)
                    if (saveResult.isSuccess) {
                        showSuccess(getString(R.string.prescription_saved_successfully))
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