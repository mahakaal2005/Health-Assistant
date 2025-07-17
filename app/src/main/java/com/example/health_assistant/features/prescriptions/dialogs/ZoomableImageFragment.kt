package com.example.health_assistant.features.prescriptions.dialogs

import android.app.Dialog
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import coil3.load
import coil3.request.crossfade
import coil3.request.error
import coil3.request.placeholder
import com.example.health_assistant.R
import com.example.health_assistant.databinding.FragmentZoomableImageBinding
import com.example.health_assistant.features.prescriptions.utils.FileManager
import com.example.health_assistant.utils.ImageOrientationFixer
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * DialogFragment for displaying prescription images with zoom and pan functionality
 * Uses the ZoomableImageView for consistent zoom behavior across the app
 */
@AndroidEntryPoint
class ZoomableImageFragment : DialogFragment() {

    private var _binding: FragmentZoomableImageBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var fileManager: FileManager

    private lateinit var imagePath: String

    companion object {
        private const val ARG_IMAGE_PATH = "image_path"

        fun newInstance(imagePath: String): ZoomableImageFragment {
            return ZoomableImageFragment().apply {
                arguments = bundleOf(ARG_IMAGE_PATH to imagePath)
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Create a full-screen dialog for image viewing
        val dialog = Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen)

        dialog.window?.apply {
            setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )
        }

        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentZoomableImageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        imagePath = arguments?.getString(ARG_IMAGE_PATH)
            ?: throw IllegalArgumentException("Image path is required")

        setupToolbar()
        loadImage()
        setupInstructions()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            dismiss() // Use dismiss() instead of findNavController().navigateUp() for DialogFragment
        }

        binding.shareImageButton.setOnClickListener {
            shareImage()
        }
    }

    private fun loadImage() {
        binding.loadingProgress.visibility = View.VISIBLE

        try {
            // Create a URI from the image path
            val imageUri = Uri.parse(imagePath)
            
            // Fix orientation if needed
            val fixedUri = if (imagePath.startsWith("file:") || imagePath.startsWith("/")) {
                ImageOrientationFixer.fixImageOrientation(requireContext(), imageUri)
            } else {
                imageUri
            }

            binding.zoomableImageView.load(fixedUri) {
                placeholder(R.drawable.ic_prescription_placeholder)
                error(R.drawable.ic_prescription_placeholder)
                crossfade(true)
                listener(
                    onSuccess = { _, _ ->
                        binding.loadingProgress.visibility = View.GONE
                    },
                    onError = { _, _ ->
                        binding.loadingProgress.visibility = View.GONE
                    }
                )
            }
        } catch (e: Exception) {
            // If there's an error, try loading the original path directly
            binding.zoomableImageView.load(imagePath) {
                placeholder(R.drawable.ic_prescription_placeholder)
                error(R.drawable.ic_prescription_placeholder)
                crossfade(true)
                listener(
                    onSuccess = { _, _ ->
                        binding.loadingProgress.visibility = View.GONE
                    },
                    onError = { _, _ ->
                        binding.loadingProgress.visibility = View.GONE
                    }
                )
            }
        }
    }

    private fun setupInstructions() {
        // Show instructions for a few seconds then fade out
        binding.instructionsText.visibility = View.VISIBLE
        binding.instructionsText.postDelayed({
            binding.instructionsText.animate()
                .alpha(0f)
                .setDuration(500)
                .withEndAction {
                    binding.instructionsText.visibility = View.GONE
                }
                .start()
        }, 3000)
    }

    private fun shareImage() {
        try {
            val shareableUri = fileManager.getShareableUri(imagePath)
            if (shareableUri != null) {
                val shareIntent = android.content.Intent().apply {
                    action = android.content.Intent.ACTION_SEND
                    type = "image/png"
                    putExtra(android.content.Intent.EXTRA_STREAM, shareableUri)
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                startActivity(android.content.Intent.createChooser(shareIntent, "Share Prescription Image"))
            } else {
                android.widget.Toast.makeText(
                    requireContext(),
                    "Error sharing image",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        } catch (e: Exception) {
            android.widget.Toast.makeText(
                requireContext(),
                "Error sharing image: ${e.message}",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}