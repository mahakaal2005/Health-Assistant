package com.example.health_assistant.utils

import android.content.Context
import android.net.Uri
import android.view.View
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.example.health_assistant.R
import com.example.health_assistant.data.repository.interfaces.UserProfileRepository
import com.example.health_assistant.features.profile.data.ProfileImageManager
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility class for managing profile photos across all fragments
 * Handles loading, syncing, and full-screen viewing of profile photos
 */
@Singleton
class ProfilePhotoManager @Inject constructor(
    private val userProfileRepository: UserProfileRepository,
    private val profileImageManager: ProfileImageManager
) {

    /**
     * Load and display user's profile photo in any CircleImageView
     */
    fun loadProfilePhoto(
        context: Context,
        imageView: CircleImageView,
        lifecycleOwner: LifecycleOwner,
        enableFullScreenClick: Boolean = true
    ) {
        lifecycleOwner.lifecycleScope.launch {
            try {
                // Get current user profile
                val profileResult = userProfileRepository.getUserProfile()
                if (profileResult is com.example.health_assistant.core.util.Result.Success) {
                    val profile = profileResult.data
                    if (profile != null) {
                        // Try to get local photo first
                        val localPhotoPath = profileImageManager.getProfileImagePath(profile.userId)

                        if (localPhotoPath != null) {
                            // Load from local storage
                            val uri = Uri.parse("file://$localPhotoPath")
                            imageView.setImageURI(uri)
                        } else {
                            // Use default avatar
                            imageView.setImageResource(R.drawable.ic_person)
                        }
                    } else {
                        // No profile found, use default
                        imageView.setImageResource(R.drawable.ic_person)
                    }
                } else {
                    // Error loading profile, use default
                    imageView.setImageResource(R.drawable.ic_person)
                }

                // Set up full-screen click listener if enabled
                if (enableFullScreenClick) {
                    imageView.setOnClickListener {
                        showFullScreenImage(context, imageView)
                    }
                }
            } catch (e: Exception) {
                // Handle errors gracefully
                imageView.setImageResource(R.drawable.ic_person)
                android.util.Log.e("ProfilePhotoManager", "Error loading profile photo", e)
            }
        }
    }

    /**
     * Load profile image from a specific URL or path into a CircleImageView
     * This method is used by EditProfileFragment to synchronize photo updates
     */
    fun loadProfileImage(photoUrl: String?, imageView: CircleImageView) {
        if (photoUrl.isNullOrBlank()) {
            // Show default avatar
            imageView.setImageResource(R.drawable.ic_person)
            return
        }

        try {
            // Handle different types of photo URLs/paths
            when {
                photoUrl.startsWith("file://") -> {
                    // Local file path
                    val uri = Uri.parse(photoUrl)
                    imageView.setImageURI(uri)
                }
                photoUrl.startsWith("content://") -> {
                    // Content URI
                    val uri = Uri.parse(photoUrl)
                    imageView.setImageURI(uri)
                }
                photoUrl.startsWith("http://") || photoUrl.startsWith("https://") -> {
                    // Remote URL - for future Firebase Storage integration
                    // For now, fallback to default
                    imageView.setImageResource(R.drawable.ic_person)
                }
                else -> {
                    // Assume it's a local file path
                    val uri = Uri.parse("file://$photoUrl")
                    imageView.setImageURI(uri)
                }
            }
        } catch (e: Exception) {
            // Fallback to default image
            imageView.setImageResource(R.drawable.ic_person)
            android.util.Log.w("ProfilePhotoManager", "Failed to load profile image: ${e.message}")
        }
    }

    /**
     * Show profile photo in full screen with zoom functionality
     */
    private fun showFullScreenImage(context: Context, imageView: CircleImageView) {
        val drawable = imageView.drawable
        if (drawable == null) {
            return
        }

        // Create and show full screen image dialog
        val dialog = android.app.Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val dialogBinding = com.example.health_assistant.databinding.DialogFullscreenImageBinding.inflate(
            android.view.LayoutInflater.from(context)
        )
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

    /**
     * Refresh profile photo in a specific ImageView
     * Call this after a photo has been updated in EditProfileFragment
     */
    fun refreshProfilePhoto(
        context: Context,
        imageView: CircleImageView,
        lifecycleOwner: LifecycleOwner
    ) {
        loadProfilePhoto(context, imageView, lifecycleOwner, true)
    }
}