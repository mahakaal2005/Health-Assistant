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
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.health_assistant.R
import com.example.health_assistant.databinding.CardAccountActionsBinding
import com.example.health_assistant.databinding.CardAppSettingsBinding
import com.example.health_assistant.databinding.CardHealthPreferencesBinding
import com.example.health_assistant.databinding.CardHelpSupportBinding
import com.example.health_assistant.databinding.CardNotificationsBinding
import com.example.health_assistant.databinding.CardUserOverviewBinding
import com.example.health_assistant.databinding.FragmentSettingsBinding
import com.example.health_assistant.features.settings.data.SettingsRepository
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Fragment for displaying and managing all app settings.
 */
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    // Card bindings for included layouts
    private lateinit var userOverviewBinding: CardUserOverviewBinding
    private lateinit var healthPreferencesBinding: CardHealthPreferencesBinding
    private lateinit var notificationsBinding: CardNotificationsBinding
    private lateinit var appSettingsBinding: CardAppSettingsBinding
    private lateinit var helpSupportBinding: CardHelpSupportBinding
    private lateinit var accountActionsBinding: CardAccountActionsBinding

    private lateinit var viewModel: SettingsViewModel

    // Image picker for profile avatar
    private val getContent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                // Update the avatar in ViewModel
                viewModel.updateAvatarUri(uri)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)

        // Initialize included layout bindings
        userOverviewBinding = CardUserOverviewBinding.bind(binding.userOverview.root)
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
        setupUserProfileCard()
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

    private fun setupUserProfileCard() {
        // Observe user profile data changes
        with(userOverviewBinding) {
            viewLifecycleOwner.lifecycleScope.launch {
                // UserName
                viewModel.userName.collectLatest { name ->
                    userName.text = name
                }
            }

            viewLifecycleOwner.lifecycleScope.launch {
                // User Age & Gender combined
                viewModel.userAgeGenderText.collectLatest { text ->
                    userAgeGender.text = text
                }
            }

            viewLifecycleOwner.lifecycleScope.launch {
                // Health Goal
                viewModel.userHealthGoal.collectLatest { goal ->
                    userHealthGoal.text = goal
                }
            }

            viewLifecycleOwner.lifecycleScope.launch {
                // Health Status
                viewModel.userHealthStatus.collectLatest { status ->
                    healthStatusValue.text = status
                }
            }

            viewLifecycleOwner.lifecycleScope.launch {
                // Avatar
                viewModel.avatarUri.collectLatest { uri ->
                    try {
                        if (uri != null) {
                            // Try to load the image safely
                            try {
                                // Check if we can access the URI before attempting to load it
                                context?.contentResolver?.openInputStream(uri)?.use {
                                    // Successfully opened stream, safe to load
                                    userAvatar.setImageURI(uri)
                                }
                            } catch (e: Exception) {
                                // Fallback to default avatar if any error occurs
                                userAvatar.setImageResource(R.drawable.default_avatar)
                                // Log the error but don't crash
                                android.util.Log.e("SettingsFragment", "Error loading avatar: ${e.message}")
                            }
                        } else {
                            userAvatar.setImageResource(R.drawable.default_avatar)
                        }
                    } catch (e: Exception) {
                        userAvatar.setImageResource(R.drawable.default_avatar)
                        android.util.Log.e("SettingsFragment", "Error in avatar flow: ${e.message}")
                    }
                }
            }

            // Set up click listeners
            editAvatarButton.setOnClickListener {
                openImagePicker()
            }

            editProfileButton.setOnClickListener {
                showEditProfileDialog()
            }
        }
    }

    private fun setupHealthPreferencesCard() {
        with(healthPreferencesBinding) {
            // Step Goal Slider
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.stepGoal.collectLatest { goal ->
                    stepGoalSlider.value = goal.toFloat()
                }
            }

            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.stepGoalText.collectLatest { text ->
                    stepGoalValue.text = text
                }
            }

            stepGoalSlider.addOnChangeListener { _, value, fromUser ->
                if (fromUser) {
                    viewModel.updateStepGoal(value.toInt())
                }
            }

            // Water Goal Slider
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.waterGoal.collectLatest { goal ->
                    waterGoalSlider.value = goal
                }
            }

            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.waterGoalText.collectLatest { text ->
                    waterGoalValue.text = text
                }
            }

            waterGoalSlider.addOnChangeListener { _, value, fromUser ->
                if (fromUser) {
                    viewModel.updateWaterGoal(value)
                }
            }

            // Sleep Goal Slider
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.sleepGoal.collectLatest { goal ->
                    sleepGoalSlider.value = goal
                }
            }

            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.sleepGoalText.collectLatest { text ->
                    sleepGoalValue.text = text
                }
            }

            sleepGoalSlider.addOnChangeListener { _, value, fromUser ->
                if (fromUser) {
                    viewModel.updateSleepGoal(value)
                }
            }

            // AI Personalization Toggle
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.aiPersonalizationEnabled.collectLatest { enabled ->
                    aiPersonalizationSwitch.isChecked = enabled
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
                viewModel.appLanguage.collectLatest { language ->
                    if (languageDropdown.text.toString() != language) {
                        languageDropdown.setText(language, false)
                    }
                }
            }

            languageDropdown.setOnItemClickListener { _, _, position, _ ->
                viewModel.updateAppLanguage(languages[position])
            }

            // Theme Selection
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.appTheme.collectLatest { theme ->
                    when (theme) {
                        "Light" -> themeLight.isChecked = true
                        "Dark" -> themeDark.isChecked = true
                        "System Default" -> themeSystem.isChecked = true
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
                viewModel.medicationRemindersEnabled.collectLatest { enabled ->
                    medicationReminderSwitch.isChecked = enabled
                }
            }

            medicationReminderSwitch.setOnCheckedChangeListener { _, isChecked ->
                viewModel.toggleMedicationReminders(isChecked)
            }

            // Wellness Check-ins Switch
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.wellnessCheckinsEnabled.collectLatest { enabled ->
                    wellnessCheckinSwitch.isChecked = enabled
                }
            }

            wellnessCheckinSwitch.setOnCheckedChangeListener { _, isChecked ->
                viewModel.toggleWellnessCheckins(isChecked)
            }

            // Activity Goals Switch
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.activityGoalsEnabled.collectLatest { enabled ->
                    activityGoalsSwitch.isChecked = enabled
                }
            }

            activityGoalsSwitch.setOnCheckedChangeListener { _, isChecked ->
                viewModel.toggleActivityGoals(isChecked)
            }

            // Water Reminders Switch
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.waterRemindersEnabled.collectLatest { enabled ->
                    waterReminderSwitch.isChecked = enabled
                }
            }

            waterReminderSwitch.setOnCheckedChangeListener { _, isChecked ->
                viewModel.toggleWaterReminders(isChecked)
            }

            // Health Reports Switch
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.healthReportsEnabled.collectLatest { enabled ->
                    healthReportsSwitch.isChecked = enabled
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
                viewModel.appRegion.collectLatest { region ->
                    if (regionDropdown.text.toString() != region) {
                        regionDropdown.setText(region, false)
                    }
                }
            }

            regionDropdown.setOnItemClickListener { _, _, position, _ ->
                viewModel.updateAppRegion(regions[position])
            }

            // Data Sync Switch
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.dataSyncEnabled.collectLatest { enabled ->
                    dataSyncSwitch.isChecked = enabled
                }
            }

            dataSyncSwitch.setOnCheckedChangeListener { _, isChecked ->
                viewModel.toggleDataSync(isChecked)
            }

            // App Lock Status Text
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.appLockStatusText.collectLatest { text ->
                    appLockStatus.text = text
                }
            }

            // App Lock Container
            appLockContainer.setOnClickListener {
                // This would typically open a PIN setup dialog
                // For demo, we'll just toggle the state
                viewModel.setAppLockEnabled(!viewModel.appLockEnabled.value)
            }

            // Biometric Auth Switch
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.biometricAuthEnabled.collectLatest { enabled ->
                    biometricAuthSwitch.isChecked = enabled
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
                viewModel.betaProgramEnabled.collectLatest { enabled ->
                    betaProgramSwitch.isChecked = enabled
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

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        getContent.launch(intent)
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
                Toast.makeText(requireContext(), "Logging out...", Toast.LENGTH_SHORT).show()
                // In a real app, you would trigger the logout process here
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteAccountDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Account")
            .setMessage("Are you sure you want to delete your account? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                showDeleteAccountConfirmationDialog()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteAccountConfirmationDialog() {
        // Second confirmation for destructive action
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Confirm Deletion")
            .setMessage("Please type 'DELETE' to confirm account deletion.")
            .setPositiveButton("Confirm") { _, _ ->
                Toast.makeText(requireContext(), "Account deletion process started", Toast.LENGTH_SHORT).show()
                // In a real app, you would trigger the account deletion process here
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}