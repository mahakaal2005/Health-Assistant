package com.example.health_assistant.features.settings

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.health_assistant.R
import com.example.health_assistant.auth.viewmodel.AuthState
import com.example.health_assistant.auth.viewmodel.AuthViewModel
import com.example.health_assistant.core.util.Result
import com.example.health_assistant.databinding.CardAccountActionsBinding
import com.example.health_assistant.databinding.CardAppSettingsBinding
import com.example.health_assistant.databinding.CardHealthPreferencesBinding
import com.example.health_assistant.databinding.CardHelpSupportBinding
import com.example.health_assistant.databinding.CardNotificationsBinding
import com.example.health_assistant.databinding.FragmentSettingsBinding
import com.example.health_assistant.features.settings.data.SettingsRepository
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Fragment for displaying and managing all app settings.
 */
@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    // Card bindings for included layouts
    private lateinit var healthPreferencesBinding: CardHealthPreferencesBinding
    private lateinit var notificationsBinding: CardNotificationsBinding
    private lateinit var appSettingsBinding: CardAppSettingsBinding
    private lateinit var helpSupportBinding: CardHelpSupportBinding
    private lateinit var accountActionsBinding: CardAccountActionsBinding

    private lateinit var viewModel: SettingsViewModel

    // Inject AuthViewModel for logout and delete account functionality
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)

        // Initialize included layout bindings
        healthPreferencesBinding = CardHealthPreferencesBinding.bind(binding.healthPreferences.root)
        notificationsBinding = CardNotificationsBinding.bind(binding.notifications.root)
        appSettingsBinding = CardAppSettingsBinding.bind(binding.appSettings.root)
        helpSupportBinding = CardHelpSupportBinding.bind(binding.helpSupport.root)
        accountActionsBinding = CardAccountActionsBinding.bind(binding.accountActions.root)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViewModel()
        setupHealthPreferencesCard()
        setupNotificationsCard()
        setupAppSettingsCard()
        setupHelpSupportCard()
        setupAccountActionsCard()
    }

    private fun setupViewModel() {
        val repository = SettingsRepository(requireContext())
        val factory = SettingsViewModel.Factory(repository)
        viewModel = ViewModelProvider(this, factory)[SettingsViewModel::class.java]
    }


    private fun setupHealthPreferencesCard() {
        with(healthPreferencesBinding) {
            // Step Goal Slider
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.stepGoal.collectLatest { result ->
                    when (result) {
                        is Result.Success -> {
                            stepGoalSlider.value = result.data?.toFloat() ?: 5000f
                            stepGoalValue.text = "${result.data ?: 5000} steps"
                        }
                        is Result.Error -> stepGoalValue.text = "Error loading"
                        is Result.Loading -> stepGoalValue.text = "Loading..."
                    }
                }
            }

            stepGoalSlider.addOnChangeListener { _, value, fromUser ->
                if (fromUser) {
                    viewModel.updateStepGoal(value.toInt())
                }
            }

            // Water Goal Slider
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.waterGoal.collectLatest { result ->
                    when (result) {
                        is Result.Success -> {
                            waterGoalSlider.value = result.data ?: 2.0f
                            waterGoalValue.text = "${result.data ?: 2.0f} L"
                        }
                        is Result.Error -> waterGoalValue.text = "Error loading"
                        is Result.Loading -> waterGoalValue.text = "Loading..."
                    }
                }
            }

            waterGoalSlider.addOnChangeListener { _, value, fromUser ->
                if (fromUser) {
                    viewModel.updateWaterGoal(value)
                }
            }

            // Sleep Goal Slider
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.sleepGoal.collectLatest { result ->
                    when (result) {
                        is Result.Success -> {
                            sleepGoalSlider.value = result.data ?: 8.0f
                            sleepGoalValue.text = "${result.data ?: 8.0f} hours"
                        }
                        is Result.Error -> sleepGoalValue.text = "Error loading"
                        is Result.Loading -> sleepGoalValue.text = "Loading..."
                    }
                }
            }

            sleepGoalSlider.addOnChangeListener { _, value, fromUser ->
                if (fromUser) {
                    viewModel.updateSleepGoal(value)
                }
            }

            // AI Personalization Toggle
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.aiPersonalizationEnabled.collectLatest { result ->
                    when (result) {
                        is Result.Success -> aiPersonalizationSwitch.isChecked = result.data ?: false
                        is Result.Error -> aiPersonalizationSwitch.isChecked = false
                        is Result.Loading -> { /* Keep current state while loading */ }
                    }
                }
            }

            aiPersonalizationSwitch.setOnCheckedChangeListener { _, isChecked ->
                viewModel.toggleAiPersonalization(isChecked)
            }

            // Language Dropdown
            val languages = arrayOf("English (US)", "Spanish", "French", "German", "Japanese", "Chinese (Simplified)")
            val languageAdapter = ArrayAdapter(requireContext(), R.layout.list_item, languages)
            languageDropdown.setAdapter(languageAdapter)

            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.appLanguage.collectLatest { result ->
                    when (result) {
                        is Result.Success -> {
                            val language = result.data ?: "English (US)"
                            if (languageDropdown.text.toString() != language) {
                                languageDropdown.setText(language, false)
                            }
                        }
                        is Result.Error -> languageDropdown.setText("English (US)", false)
                        is Result.Loading -> { /* Keep current state while loading */ }
                    }
                }
            }

            languageDropdown.setOnItemClickListener { _, _, position, _ ->
                viewModel.updateAppLanguage(languages[position])
            }

            // Theme Selection
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.appTheme.collectLatest { result ->
                    when (result) {
                        is Result.Success -> {
                            when (result.data) {
                                "Light" -> themeLight.isChecked = true
                                "Dark" -> themeDark.isChecked = true
                                "System Default" -> themeSystem.isChecked = true
                                else -> themeLight.isChecked = true
                            }
                        }
                        is Result.Error -> themeLight.isChecked = true
                        is Result.Loading -> { /* Keep current state while loading */ }
                    }
                }
            }

            themeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
                val theme = when (checkedId) {
                    R.id.theme_light -> "Light"
                    R.id.theme_dark -> "Dark"
                    R.id.theme_system -> "System Default"
                    else -> "Light"
                }
                viewModel.updateAppTheme(theme)
            }
        }
    }

    private fun setupNotificationsCard() {
        with(notificationsBinding) {
            // Medication Reminders Switch
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.medicationRemindersEnabled.collectLatest { result ->
                    when (result) {
                        is Result.Success -> medicationReminderSwitch.isChecked = result.data ?: false
                        is Result.Error -> medicationReminderSwitch.isChecked = false
                        is Result.Loading -> { /* Keep current state while loading */ }
                    }
                }
            }

            medicationReminderSwitch.setOnCheckedChangeListener { _, isChecked ->
                viewModel.toggleMedicationReminders(isChecked)
            }

            // Wellness Check-ins Switch
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.wellnessCheckinsEnabled.collectLatest { result ->
                    when (result) {
                        is Result.Success -> wellnessCheckinSwitch.isChecked = result.data ?: false
                        is Result.Error -> wellnessCheckinSwitch.isChecked = false
                        is Result.Loading -> { /* Keep current state while loading */ }
                    }
                }
            }

            wellnessCheckinSwitch.setOnCheckedChangeListener { _, isChecked ->
                viewModel.toggleWellnessCheckins(isChecked)
            }

            // Activity Goals Switch
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.activityGoalsEnabled.collectLatest { result ->
                    when (result) {
                        is Result.Success -> activityGoalsSwitch.isChecked = result.data ?: false
                        is Result.Error -> activityGoalsSwitch.isChecked = false
                        is Result.Loading -> { /* Keep current state while loading */ }
                    }
                }
            }

            activityGoalsSwitch.setOnCheckedChangeListener { _, isChecked ->
                viewModel.toggleActivityGoals(isChecked)
            }

            // Water Reminders Switch
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.waterRemindersEnabled.collectLatest { result ->
                    when (result) {
                        is Result.Success -> waterReminderSwitch.isChecked = result.data ?: false
                        is Result.Error -> waterReminderSwitch.isChecked = false
                        is Result.Loading -> { /* Keep current state while loading */ }
                    }
                }
            }

            waterReminderSwitch.setOnCheckedChangeListener { _, isChecked ->
                viewModel.toggleWaterReminders(isChecked)
            }

            // Health Reports Switch
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.healthReportsEnabled.collectLatest { result ->
                    when (result) {
                        is Result.Success -> healthReportsSwitch.isChecked = result.data ?: false
                        is Result.Error -> healthReportsSwitch.isChecked = false
                        is Result.Loading -> { /* Keep current state while loading */ }
                    }
                }
            }

            healthReportsSwitch.setOnCheckedChangeListener { _, isChecked ->
                viewModel.toggleHealthReports(isChecked)
            }
        }
    }

    private fun setupAppSettingsCard() {
        with(appSettingsBinding) {
            // Region Dropdown
            val regions = arrayOf("United States", "United Kingdom", "Canada", "Australia",
                "Germany", "France", "Japan", "China", "India", "Brazil")
            val regionAdapter = ArrayAdapter(requireContext(), R.layout.list_item, regions)
            regionDropdown.setAdapter(regionAdapter)

            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.appRegion.collectLatest { result ->
                    when (result) {
                        is Result.Success -> {
                            val region = result.data ?: "United States"
                            if (regionDropdown.text.toString() != region) {
                                regionDropdown.setText(region, false)
                            }
                        }
                        is Result.Error -> regionDropdown.setText("United States", false)
                        is Result.Loading -> { /* Keep current state while loading */ }
                    }
                }
            }

            regionDropdown.setOnItemClickListener { _, _, position, _ ->
                viewModel.updateAppRegion(regions[position])
            }

            // Data Sync Switch
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.dataSyncEnabled.collectLatest { result ->
                    when (result) {
                        is Result.Success -> dataSyncSwitch.isChecked = result.data ?: false
                        is Result.Error -> dataSyncSwitch.isChecked = false
                        is Result.Loading -> { /* Keep current state while loading */ }
                    }
                }
            }

            dataSyncSwitch.setOnCheckedChangeListener { _, isChecked ->
                viewModel.toggleDataSync(isChecked)
            }

            // App Lock Status - Generate text based on enabled state
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.appLockEnabled.collectLatest { result ->
                    when (result) {
                        is Result.Success -> {
                            val isEnabled = result.data ?: false
                            appLockStatus.text = if (isEnabled) "Enabled" else "Disabled"
                        }
                        is Result.Error -> appLockStatus.text = "Error loading"
                        is Result.Loading -> appLockStatus.text = "Loading..."
                    }
                }
            }

            // App Lock Container
            appLockContainer.setOnClickListener {
                // This would typically open a PIN setup dialog
                // For demo, we'll just toggle the state
                viewLifecycleOwner.lifecycleScope.launch {
                    viewModel.appLockEnabled.value.let { result ->
                        if (result is Result.Success) {
                            viewModel.setAppLockEnabled(!(result.data ?: false))
                        }
                    }
                }
            }

            // Biometric Auth Switch
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.biometricAuthEnabled.collectLatest { result ->
                    when (result) {
                        is Result.Success -> biometricAuthSwitch.isChecked = result.data ?: false
                        is Result.Error -> biometricAuthSwitch.isChecked = false
                        is Result.Loading -> { /* Keep current state while loading */ }
                    }
                }
            }

            biometricAuthSwitch.setOnCheckedChangeListener { _, isChecked ->
                viewModel.toggleBiometricAuth(isChecked)
            }
        }
    }

    private fun setupHelpSupportCard() {
        with(helpSupportBinding) {
            // Help Center
            helpCenterItem.setOnClickListener {
                Toast.makeText(requireContext(), "Opening Help Center...", Toast.LENGTH_SHORT).show()
            }

            // Contact Us
            contactUsItem.setOnClickListener {
                Toast.makeText(requireContext(), "Opening Contact Form...", Toast.LENGTH_SHORT).show()
            }

            // FAQ
            faqItem.setOnClickListener {
                Toast.makeText(requireContext(), "Opening FAQ...", Toast.LENGTH_SHORT).show()
            }

            // Privacy Policy
            privacyPolicyItem.setOnClickListener {
                Toast.makeText(requireContext(), "Opening Privacy Policy...", Toast.LENGTH_SHORT).show()
            }

            // Terms of Service
            termsServiceItem.setOnClickListener {
                Toast.makeText(requireContext(), "Opening Terms of Service...", Toast.LENGTH_SHORT).show()
            }

            // Beta Program Switch
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.betaProgramEnabled.collectLatest { result ->
                    when (result) {
                        is Result.Success -> betaProgramSwitch.isChecked = result.data ?: false
                        is Result.Error -> betaProgramSwitch.isChecked = false
                        is Result.Loading -> { /* Keep current state while loading */ }
                    }
                }
            }

            betaProgramSwitch.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    showBetaProgramDialog()
                } else {
                    viewModel.toggleBetaProgram(false)
                }
            }
        }
    }

    private fun setupAccountActionsCard() {
        with(accountActionsBinding) {
            // Switch Account Button
            switchAccountButton.setOnClickListener {
                Toast.makeText(requireContext(), "Switching accounts...", Toast.LENGTH_SHORT).show()
            }

            // Clear Cache
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.cacheSize.collectLatest { size ->
                    cacheSize.text = size
                }
            }

            clearCacheButton.setOnClickListener {
                if (viewModel.clearCache()) {
                    Toast.makeText(requireContext(), "Cache cleared successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Failed to clear cache", Toast.LENGTH_SHORT).show()
                }
            }

            // Logout Button
            logoutButton.setOnClickListener {
                showLogoutConfirmationDialog()
            }

            // Delete Account Button
            deleteAccountButton.setOnClickListener {
                showDeleteAccountDialog()
            }
        }
    }


    private fun showEditProfileDialog() {
        // In a real app, this would be a more comprehensive dialog
        // For this example, we'll just show a simple message
        Toast.makeText(requireContext(), "Opening profile editor...", Toast.LENGTH_SHORT).show()
    }

    private fun showBetaProgramDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Join Beta Program")
            .setMessage("The beta program gives you early access to new features, but may include bugs. Are you sure you want to join?")
            .setPositiveButton("Join") { _, _ ->
                viewModel.toggleBetaProgram(true)
                Toast.makeText(requireContext(), "Welcome to the Beta Program!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                binding.helpSupport.betaProgramSwitch.isChecked = false
                dialog.dismiss()
            }
            .show()
    }

    private fun showLogoutConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Log Out")
            .setMessage("Are you sure you want to log out?")
            .setPositiveButton("Log Out") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteAccountDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Account")
            .setMessage("Are you sure you want to delete your account? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                performDeleteAccount()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Perform actual logout operation using AuthViewModel
     */
    private fun performLogout() {
        // Show loading state
        accountActionsBinding.logoutButton.isEnabled = false

        // Use the universal logout system
        com.example.health_assistant.core.util.AuthNavigationUtil.performUniversalLogout(
            authViewModel = authViewModel,
            context = requireContext(),
            activity = requireActivity(),
            onLogoutStart = {
                Snackbar.make(binding.root, "Logging out...", Snackbar.LENGTH_SHORT).show()
            },
            onLogoutSuccess = {
                Snackbar.make(binding.root, "Logged out successfully", Snackbar.LENGTH_SHORT).show()
            },
            onLogoutError = { error ->
                accountActionsBinding.logoutButton.isEnabled = true
                Snackbar.make(binding.root, "Logout failed: $error", Snackbar.LENGTH_LONG).show()
            }
        )
    }

    /**
     * Perform actual account deletion using AuthViewModel
     */
    private fun performDeleteAccount() {
        // Show loading state
        accountActionsBinding.deleteAccountButton.isEnabled = false

        // Use the universal account deletion system with password confirmation
        com.example.health_assistant.core.util.AuthNavigationUtil.performUniversalAccountDeletion(
            authViewModel = authViewModel,
            context = requireContext(),
            coroutineScope = viewLifecycleOwner.lifecycleScope,
            activity = requireActivity(),
            onDeleteStart = {
                // Show loading message when deletion starts
                Snackbar.make(binding.root, "Deleting account...", Snackbar.LENGTH_SHORT).show()
            },
            onDeleteSuccess = {
                Snackbar.make(binding.root, "Account deleted successfully", Snackbar.LENGTH_SHORT).show()
            },
            onDeleteError = { error ->
                accountActionsBinding.deleteAccountButton.isEnabled = true
                Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG).show()
            }
        )
    }
}