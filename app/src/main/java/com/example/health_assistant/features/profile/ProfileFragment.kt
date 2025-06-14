package com.example.health_assistant.features.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.health_assistant.auth.AuthActivity
import com.example.health_assistant.auth.session.SessionManager
import com.example.health_assistant.databinding.FragmentProfileBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var sessionManager: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        sessionManager = SessionManager(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize UI elements and set up any event listeners
        setupUI()
    }

    private fun setupUI() {
        // Display user email if available
        val userEmail = sessionManager.getUserEmail()
        if (!userEmail.isNullOrEmpty()) {
            binding.profileIntroText.text = "Hello, $userEmail"
        }

        // Set up logout button click listener
        binding.logoutButton.setOnClickListener {
            showLogoutConfirmationDialog()
        }
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
        // Clear user session
        sessionManager.logout()

        // Navigate to AuthActivity
        val intent = Intent(requireContext(), AuthActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}