package com.example.health_assistant.features.completeprofile

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.health_assistant.R
import com.example.health_assistant.databinding.FragmentCompleteProfileBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Fragment for completing user profile with personal health information
 */
@AndroidEntryPoint
class CompleteProfileFragment : Fragment() {

    private var _binding: FragmentCompleteProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CompleteProfileViewModel by viewModels()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCompleteProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        // Skip button
        binding.btnSkip.setOnClickListener {
            viewModel.skipProfile()
        }

        // Done button
        binding.btnDone.setOnClickListener {
            viewModel.saveProfile()
        }

        // Field click listeners
        binding.llGender.setOnClickListener {
            showGenderPicker()
        }

        binding.llHeight.setOnClickListener {
            showHeightPicker()
        }

        binding.llWeight.setOnClickListener {
            showWeightPicker()
        }

        binding.llBirthday.setOnClickListener {
            showDatePicker()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                updateUI(state)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.events.collect { event ->
                handleEvent(event)
            }
        }
    }

    private fun updateUI(state: CompleteProfileUiState) {
        // Update field values
        binding.tvGenderValue.text = state.selectedGender ?: getString(R.string.not_set)
        binding.tvHeightValue.text = state.selectedHeight?.let {
            getString(R.string.height_format, it)
        } ?: getString(R.string.not_set)
        binding.tvWeightValue.text = state.selectedWeight?.let {
            getString(R.string.weight_format, it)
        } ?: getString(R.string.not_set)
        binding.tvBirthdayValue.text = state.selectedBirthday ?: getString(R.string.not_set)

        // Update done button state with proper visual feedback using app's primary theme
        binding.btnDone.isEnabled = state.isFormValid

        // Update button appearance based on state using consistent app colors
        if (state.isFormValid) {
            binding.btnDone.backgroundTintList = android.content.res.ColorStateList.valueOf(
                androidx.core.content.ContextCompat.getColor(requireContext(), R.color.colorPrimary)
            )
            binding.btnDone.alpha = 1.0f
        } else {
            binding.btnDone.backgroundTintList = android.content.res.ColorStateList.valueOf(
                androidx.core.content.ContextCompat.getColor(requireContext(), R.color.textDisabled)
            )
            binding.btnDone.alpha = 0.6f
        }

        // Show loading state
        binding.btnDone.text = if (state.isLoading) {
            "Saving..."
        } else {
            getString(R.string.done)
        }
    }

    private fun handleEvent(event: CompleteProfileEvent) {
        when (event) {
            is CompleteProfileEvent.NavigateToHome -> {
                // Navigate to MainActivity instead of trying to access homeFragment directly
                // Since we're in AuthActivity, we need to start MainActivity
                try {
                    com.example.health_assistant.main.MainActivity.startWithHomeFragment(requireContext())
                    requireActivity().finish()
                } catch (e: Exception) {
                    // Fallback: try using the navigation action to MainActivity
                    try {
                        findNavController().navigate(R.id.action_completeProfile_to_mainActivity)
                    } catch (navException: Exception) {
                        Toast.makeText(requireContext(), "Navigation error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
            is CompleteProfileEvent.ShowError -> {
                Toast.makeText(requireContext(), event.message, Toast.LENGTH_LONG).show()
            }
            is CompleteProfileEvent.ShowSuccess -> {
                Toast.makeText(requireContext(), event.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showGenderPicker() {
        val genderOptions = arrayOf(
            getString(R.string.male),
            getString(R.string.female),
            getString(R.string.other),
            getString(R.string.prefer_not_to_say)
        )

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.select_gender))
            .setItems(genderOptions) { _, which ->
                viewModel.setGender(genderOptions[which])
            }
            .show()
    }

    private fun showHeightPicker() {
        val heightOptions = (140..210).map { it.toFloat() }.toTypedArray()
        val heightLabels = heightOptions.map { getString(R.string.height_format, it) }.toTypedArray()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.select_height))
            .setItems(heightLabels) { _, which ->
                viewModel.setHeight(heightOptions[which])
            }
            .show()
    }

    private fun showWeightPicker() {
        val weightOptions = (30..150).map { it.toFloat() }.toTypedArray()
        val weightLabels = weightOptions.map { getString(R.string.weight_format, it) }.toTypedArray()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.select_weight))
            .setItems(weightLabels) { _, which ->
                viewModel.setWeight(weightOptions[which])
            }
            .show()
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val selectedDate = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth)
                }
                val formattedDate = dateFormat.format(selectedDate.time)
                viewModel.setBirthday(formattedDate)
            },
            currentYear - 25, // Default to 25 years ago
            currentMonth,
            currentDay
        ).apply {
            // Set max date to today
            datePicker.maxDate = System.currentTimeMillis()
            // Set min date to 100 years ago
            datePicker.minDate = System.currentTimeMillis() - (100L * 365 * 24 * 60 * 60 * 1000)
        }.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}