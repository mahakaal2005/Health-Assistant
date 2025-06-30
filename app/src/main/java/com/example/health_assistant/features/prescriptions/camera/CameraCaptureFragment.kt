package com.example.health_assistant.features.prescriptions.camera

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.health_assistant.R
import com.example.health_assistant.databinding.FragmentCameraCaptureBinding
import kotlinx.coroutines.launch

/**
 * Camera capture fragment for prescription photos
 * Integrates with CameraManager for photo capture functionality
 */
class CameraCaptureFragment : Fragment() {

    private var _binding: FragmentCameraCaptureBinding? = null
    private val binding get() = _binding!!

    private lateinit var cameraManager: CameraManager
    private var isFlashEnabled = false

    // Camera permission launcher
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            setupCamera()
        } else {
            showPermissionDeniedMessage()
        }
    }

    // Gallery launcher for selecting existing photos
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            returnCapturedImage(selectedUri)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCameraCaptureBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize camera manager
        cameraManager = CameraManager(requireContext())

        setupUI()
        checkCameraPermission()
    }

    private fun setupUI() {
        // Back button
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        // Capture button
        binding.btnCapture.setOnClickListener {
            capturePhoto()
        }

        // Gallery button
        binding.btnGallery.setOnClickListener {
            openGallery()
        }

        // Flash toggle
        binding.btnFlash.setOnClickListener {
            toggleFlash()
        }

        // Switch camera (if multiple cameras available)
        binding.btnSwitchCamera.setOnClickListener {
            // For now, just show a message - can be implemented later
            Toast.makeText(requireContext(), "Camera switch not implemented yet", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                setupCamera()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                // Show explanation and request permission
                showPermissionRationale()
            }
            else -> {
                // Request permission directly
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun setupCamera() {
        if (!cameraManager.isCameraAvailable()) {
            showCameraUnavailableMessage()
            return
        }

        lifecycleScope.launch {
            cameraManager.setupCamera(binding.cameraPreview, viewLifecycleOwner)
                .onSuccess {
                    // Camera setup successful
                    binding.cameraPreview.visibility = View.VISIBLE
                }
                .onFailure { exception ->
                    showCameraSetupError(exception.message)
                }
        }
    }

    private fun capturePhoto() {
        binding.loadingOverlay.visibility = View.VISIBLE

        lifecycleScope.launch {
            cameraManager.capturePhoto()
                .onSuccess { uri ->
                    binding.loadingOverlay.visibility = View.GONE
                    returnCapturedImage(uri)
                }
                .onFailure { exception ->
                    binding.loadingOverlay.visibility = View.GONE
                    showCaptureError(exception.message)
                }
        }
    }

    private fun openGallery() {
        galleryLauncher.launch("image/*")
    }

    private fun toggleFlash() {
        isFlashEnabled = !isFlashEnabled
        val flashIcon = if (isFlashEnabled) {
            R.drawable.ic_flash_on_24
        } else {
            R.drawable.ic_flash_off_24
        }
        binding.btnFlash.setImageResource(flashIcon)

        // TODO: Implement actual flash control with camera
        Toast.makeText(
            requireContext(),
            if (isFlashEnabled) "Flash enabled" else "Flash disabled",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun returnCapturedImage(uri: Uri) {
        // Return the captured/selected image URI to the calling fragment
        val result = Bundle().apply {
            putString("captured_image_uri", uri.toString())
        }
        setFragmentResult("camera_capture_result", result)
        findNavController().navigateUp()
    }

    private fun showPermissionRationale() {
        Toast.makeText(
            requireContext(),
            "Camera permission is required to capture prescription photos",
            Toast.LENGTH_LONG
        ).show()
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun showPermissionDeniedMessage() {
        Toast.makeText(
            requireContext(),
            "Camera permission denied. You can enable it in app settings.",
            Toast.LENGTH_LONG
        ).show()
        findNavController().navigateUp()
    }

    private fun showCameraUnavailableMessage() {
        Toast.makeText(
            requireContext(),
            "Camera is not available on this device",
            Toast.LENGTH_LONG
        ).show()
        findNavController().navigateUp()
    }

    private fun showCameraSetupError(message: String?) {
        Toast.makeText(
            requireContext(),
            "Camera setup failed: ${message ?: "Unknown error"}",
            Toast.LENGTH_LONG
        ).show()
        findNavController().navigateUp()
    }

    private fun showCaptureError(message: String?) {
        Toast.makeText(
            requireContext(),
            "Photo capture failed: ${message ?: "Unknown error"}",
            Toast.LENGTH_LONG
        ).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraManager.releaseCamera()
        _binding = null
    }
}