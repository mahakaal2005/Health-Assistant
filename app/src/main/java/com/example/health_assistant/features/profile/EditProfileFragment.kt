package com.example.health_assistant.features.profile

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.health_assistant.R
import com.example.health_assistant.core.util.Result
import com.example.health_assistant.databinding.FragmentEditProfileBinding
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Fragment for editing user profile information
 * Supports editing: displayName, photo, birthday, gender, height, weight
 * Email is read-only
 */
@AndroidEntryPoint
class EditProfileFragment : Fragment() {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EditProfileViewModel by viewModels()

    // Image picker for profile photo
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                viewModel.updateProfilePhoto(uri)
            }
        }
    }

    // Date formatter for birthday display
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayDateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupObservers()
        setupClickListeners()

        // Load current profile data
        viewModel.loadProfile()
    }

    private fun setupUI() {
        // Setup gender dropdown
        val genderOptions = arrayOf("Male", "Female", "Other", "Prefer not to say")
        val genderAdapter = ArrayAdapter(requireContext(), R.layout.list_item, genderOptions)
        binding.genderDropdown.setAdapter(genderAdapter)
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            // Observe profile state
            viewModel.profileState.collect { state ->
                when (state) {
                    is EditProfileViewModel.ProfileState.Loading -> {
                        showLoading(true)
                    }
                    is EditProfileViewModel.ProfileState.Success -> {
                        showLoading(false)
                        populateFields(state.profile)
                    }
                    is EditProfileViewModel.ProfileState.Error -> {
                        showLoading(false)
                        showError(state.message)
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            // Observe save state
            viewModel.saveState.collect { state ->
                when (state) {
                    is EditProfileViewModel.SaveState.Idle -> {
                        setSaveButtonEnabled(true)
                    }
                    is EditProfileViewModel.SaveState.Saving -> {
                        setSaveButtonEnabled(false)
                        showSnackbar("Saving profile...")
                    }
                    is EditProfileViewModel.SaveState.Success -> {
                        setSaveButtonEnabled(true)
                        showSnackbar("Profile saved successfully!")
                        // Navigate back or close fragment
                        findNavController().navigateUp()
                    }
                    is EditProfileViewModel.SaveState.Error -> {
                        setSaveButtonEnabled(true)
                        showError(state.message)
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            // Observe validation errors
            viewModel.validationErrors.collect { errors ->
                clearFieldErrors()
                errors.forEach { (field, error) ->
                    showFieldError(field, error)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            // Observe photo URL changes
            viewModel.currentPhotoUrl.collect { photoUrl ->
                // Load image using existing image loading logic
                // This will be enhanced when we add Room for local storage
                updateProfileImage(photoUrl)
            }
        }
    }

    private fun setupClickListeners() {
        // Photo change button
        binding.changePhotoButton.setOnClickListener {
            openImagePicker()
        }

        // Birthday field
        binding.birthdayEditText.setOnClickListener {
            showDatePicker()
        }

        // Save button
        binding.saveButton.setOnClickListener {
            saveProfile()
        }

        // Cancel button
        binding.cancelButton.setOnClickListener {
            findNavController().navigateUp()
        }

        // Back button in toolbar
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun populateFields(profile: com.example.health_assistant.data.repository.interfaces.UserProfile) {
        binding.apply {
            // Read-only email
            emailTextView.text = profile.email

            // Editable fields
            displayNameEditText.setText(profile.displayName ?: "")

            profile.birthday?.let { birthday ->
                try {
                    val date = dateFormatter.parse(birthday)
                    birthdayEditText.setText(displayDateFormatter.format(date))
                } catch (e: Exception) {
                    birthdayEditText.setText(birthday)
                }
            }

            profile.gender?.let { gender ->
                genderDropdown.setText(gender, false)
            }

            profile.height?.let { height ->
                heightEditText.setText(height.toString())
            }

            profile.weight?.let { weight ->
                weightEditText.setText(weight.toString())
            }
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePickerLauncher.launch(intent)
    }

    private fun showDatePicker() {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select Birthday")
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            val date = Date(selection)
            val formattedDate = dateFormatter.format(date)
            val displayDate = displayDateFormatter.format(date)

            binding.birthdayEditText.setText(displayDate)
            // Store the ISO format for saving
            binding.birthdayEditText.tag = formattedDate
        }

        datePicker.show(parentFragmentManager, "DATE_PICKER")
    }

    private fun saveProfile() {
        val displayName = binding.displayNameEditText.text.toString().trim()
        val birthday = binding.birthdayEditText.tag as? String
        val gender = binding.genderDropdown.text.toString().takeIf { it.isNotBlank() }
        val heightText = binding.heightEditText.text.toString().trim()
        val weightText = binding.weightEditText.text.toString().trim()

        val height = heightText.toFloatOrNull()
        val weight = weightText.toFloatOrNull()

        viewModel.saveProfile(
            displayName = displayName,
            birthday = birthday,
            gender = gender,
            height = height,
            weight = weight
        )
    }

    private fun showLoading(show: Boolean) {
        binding.progressIndicator.visibility = if (show) View.VISIBLE else View.GONE
        binding.scrollView.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun setSaveButtonEnabled(enabled: Boolean) {
        binding.saveButton.isEnabled = enabled
    }

    private fun showError(message: String) {
        showSnackbar(message)
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    private fun clearFieldErrors() {
        binding.apply {
            displayNameInputLayout.error = null
            heightInputLayout.error = null
            weightInputLayout.error = null
            birthdayInputLayout.error = null
            genderInputLayout.error = null
        }
    }

    private fun showFieldError(field: String, error: String) {
        when (field) {
            "displayName" -> binding.displayNameInputLayout.error = error
            "height" -> binding.heightInputLayout.error = error
            "weight" -> binding.weightInputLayout.error = error
            "birthday" -> binding.birthdayInputLayout.error = error
            "gender" -> binding.genderInputLayout.error = error
        }
    }

    private fun updateProfileImage(photoUrl: String?) {
        // For now, this is a placeholder
        // Will be enhanced when we integrate Room for local image storage
        if (photoUrl != null) {
            // Load image using existing image loading infrastructure
            // binding.profileImageView.load(photoUrl)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}