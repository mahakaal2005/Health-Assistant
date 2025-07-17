package com.example.health_assistant.utils

import android.app.Dialog
import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import coil3.load
import coil3.request.crossfade
import coil3.request.error
import coil3.request.placeholder
import com.example.health_assistant.R
import com.example.health_assistant.databinding.DialogFullscreenImageBinding
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility for showing any image in fullscreen with zoom functionality
 * Reuses the same zoom system as profile pictures for consistency
 */
@Singleton
class ImageZoomManager @Inject constructor() {

    /**
     * Show any image in fullscreen with zoom functionality
     * Uses the same proven zoom system as profile pictures
     */
    fun showImageFullscreen(
        context: Context,
        imagePath: String
    ) {
        try {
            // Create fullscreen dialog using the same layout as profile photos
            val dialog = Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
            val dialogBinding = DialogFullscreenImageBinding.inflate(
                LayoutInflater.from(context)
            )
            dialog.setContentView(dialogBinding.root)

            // Show loading indicator
            dialogBinding.loadingIndicator.visibility = View.VISIBLE

            // Make sure instructions are visible initially
            dialogBinding.instructionsText.visibility = View.VISIBLE

            try {
                // Create a URI from the image path
                val imageUri = Uri.parse(imagePath)
                
                // Fix orientation if needed
                val fixedUri = if (imagePath.startsWith("file:") || imagePath.startsWith("/")) {
                    ImageOrientationFixer.fixImageOrientation(context, imageUri)
                } else {
                    imageUri
                }

                // Load the image using Coil
                dialogBinding.fullscreenImage.load(fixedUri) {
                    placeholder(R.drawable.ic_prescription_placeholder)
                    error(R.drawable.ic_prescription_placeholder)
                    crossfade(true)
                    listener(
                        onSuccess = { _, _ ->
                            dialogBinding.loadingIndicator.visibility = View.GONE
                            showInstructions(dialogBinding)
                        },
                        onError = { _, _ ->
                            dialogBinding.loadingIndicator.visibility = View.GONE
                            Toast.makeText(context, "Error loading image", Toast.LENGTH_SHORT).show()
                            // Still show instructions even if image fails to load
                            showInstructions(dialogBinding)
                        }
                    )
                }
            } catch (e: Exception) {
                // Fallback to original path if URI parsing or orientation fixing fails
                dialogBinding.fullscreenImage.load(imagePath) {
                    placeholder(R.drawable.ic_prescription_placeholder)
                    error(R.drawable.ic_prescription_placeholder)
                    crossfade(true)
                    listener(
                        onSuccess = { _, _ ->
                            dialogBinding.loadingIndicator.visibility = View.GONE
                            showInstructions(dialogBinding)
                        },
                        onError = { _, _ ->
                            dialogBinding.loadingIndicator.visibility = View.GONE
                            Toast.makeText(context, "Error loading image", Toast.LENGTH_SHORT).show()
                            // Still show instructions even if image fails to load
                            showInstructions(dialogBinding)
                        }
                    )
                }
            }

            // Set up close button
            dialogBinding.closeButton.setOnClickListener {
                dialog.dismiss()
            }

            // Set up single tap to close on the ZoomableImageView
            dialogBinding.fullscreenImage.setOnClickListener {
                dialog.dismiss()
            }

            // Close on background tap
            dialogBinding.root.setOnClickListener { v ->
                if (v == dialogBinding.root) {
                    dialog.dismiss()
                }
            }

            dialog.show()
        } catch (e: Exception) {
            Toast.makeText(context, "Error displaying image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showInstructions(dialogBinding: DialogFullscreenImageBinding) {
        // Show instructions for a few seconds
        dialogBinding.instructionsText.visibility = View.VISIBLE
        dialogBinding.instructionsText.postDelayed({
            dialogBinding.instructionsText.animate()
                .alpha(0f)
                .setDuration(500)
                .withEndAction {
                    dialogBinding.instructionsText.visibility = View.GONE
                }
                .start()
        }, 3000)
    }
}