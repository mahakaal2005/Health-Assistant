package com.example.health_assistant.features.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.health_assistant.R
import com.example.health_assistant.auth.AuthActivity
import com.example.health_assistant.databinding.FragmentProfileBinding
import com.example.health_assistant.features.profile.viewmodel.ProfileViewModel
import com.example.health_assistant.utils.ProfilePhotoManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    // Inject ProfileViewModel using Hilt
    private val viewModel: ProfileViewModel by viewModels()

    // Inject ProfilePhotoManager using Hilt
    @Inject
    lateinit var profilePhotoManager: ProfilePhotoManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize UI elements and set up any event listeners
        setupUI()
        observeViewModel()
        loadProfilePhoto()
    }

    override fun onResume() {
        super.onResume()
        // Refresh profile photo when returning from EditProfileFragment
        loadProfilePhoto()
        // Refresh profile data to show updated bio and other fields
        viewModel.refreshProfile()
    }

    private fun loadProfilePhoto() {
        // Load profile photo using the shared manager
        profilePhotoManager.loadProfilePhoto(
            context = requireContext(),
            imageView = binding.profileAvatar,
            lifecycleOwner = viewLifecycleOwner,
            enableFullScreenClick = true
        )
    }

    private fun observeViewModel() {
        viewModel.userEmail.observe(viewLifecycleOwner) { email ->
            if (!email.isNullOrEmpty()) {
                binding.profileIntroText.text = "Hello, $email"
            }
        }

        // Observe full user profile to display bio and other fields
        viewModel.userProfile.observe(viewLifecycleOwner) { profile ->
            profile?.let {
                // Update display name if available
                if (!it.displayName.isNullOrBlank()) {
                    binding.profileIntroText.text = "Hello, ${it.displayName}"
                } else if (!it.email.isNullOrEmpty()) {
                    binding.profileIntroText.text = "Hello, ${it.email}"
                }

                // Update bio field
                if (!it.bio.isNullOrBlank()) {
                    binding.userBio.text = it.bio
                    binding.userBio.visibility = View.VISIBLE
                } else {
                    binding.userBio.text = "Add your bio in Edit Profile"
                    binding.userBio.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun setupUI() {
        // Set up settings button click listener
        binding.settingsButton.setOnClickListener {
            // Navigate to Settings screen
            findNavController().navigate(R.id.action_profileFragment_to_settingsFragment)
        }

        // Set up edit profile button click listener
        binding.editProfileButton.setOnClickListener {
            // Navigate to Edit Profile screen
            findNavController().navigate(R.id.action_profileFragment_to_editProfileFragment)
        }

        // Set up logout button click listener
        binding.logoutButton.setOnClickListener {
            showLogoutConfirmationDialog()
        }

        // Note: Camera button has been removed from layout and no longer handled here
        // Profile photo editing is now only available in EditProfileFragment
    }

    /**
     * Shows a confirmation dialog before logging out
     */
    private fun showLogoutConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton("Logout") { _, _ ->
                performLogout()
            }
            .show()
    }

    /**
     * Performs the actual logout operation:
     * 1. Clears the session data
     * 2. Redirects to the AuthActivity
     */
    private fun performLogout() {
        // Sign out using the ViewModel
        viewModel.signOut()

        // Navigate to AuthActivity using Navigation Component
        findNavController().navigate(R.id.action_global_to_authActivity)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}