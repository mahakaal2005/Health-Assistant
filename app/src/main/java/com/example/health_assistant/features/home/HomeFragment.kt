package com.example.health_assistant.features.home

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.health_assistant.BuildConfig
import com.example.health_assistant.R
import com.example.health_assistant.auth.session.SessionManager
import com.example.health_assistant.core.performance.FragmentPerformanceManager
import com.example.health_assistant.core.util.Result
import com.example.health_assistant.data.repository.interfaces.UserProfileRepository
import com.example.health_assistant.data.repository.interfaces.HealthRepository
import com.example.health_assistant.databinding.FragmentHomeBinding
import com.example.health_assistant.features.health.model.HealthMetrics
import com.example.health_assistant.features.health.viewmodel.HealthMetricsViewModel
import com.example.health_assistant.features.home.adapters.WellnessTipsAdapter
import com.example.health_assistant.features.home.models.WellnessTip
import com.example.health_assistant.features.journal.workers.ActivityCardScheduler
import com.example.health_assistant.utils.HealthNotificationManager
import com.example.health_assistant.utils.ProfilePhotoManager
import com.example.health_assistant.utils.ChartManager
import com.example.health_assistant.data.models.DailyStepData
import com.example.health_assistant.data.models.WeeklyStepSummary
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.BarDataSet
import androidx.work.WorkManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.Random
import javax.inject.Inject
import java.time.LocalDate

/**
 * Premium Home Fragment featuring a modern interface with personalized greeting,
 * health summary, quick actions, and wellness insights.
 * Uses local device sensors for health data tracking.
 * Follows premium UI/UX design principles from top health apps.
 */
@AndroidEntryPoint
class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    // Add safe binding property for coroutines
    private val safeBinding get() = _binding

    @Inject
    lateinit var sessionManager: SessionManager

    // Inject ProfilePhotoManager using Hilt
    @Inject
    lateinit var profilePhotoManager: ProfilePhotoManager

    // Inject UserProfileRepository to get real-time display name
    @Inject
    lateinit var userProfileRepository: UserProfileRepository


    // NEW: Inject FragmentPerformanceManager for smooth transitions
    @Inject
    lateinit var performanceManager: FragmentPerformanceManager

    // NEW: Inject HealthNotificationManager for step notifications
    @Inject
    lateinit var notificationManager: HealthNotificationManager

    // CRITICAL FIX: Inject ActivityCardScheduler instead of manually creating it
    @Inject
    lateinit var activityCardScheduler: ActivityCardScheduler

    // Inject HealthRepository directly
    @Inject
    lateinit var healthRepository: HealthRepository

    private lateinit var wellnessTipsAdapter: WellnessTipsAdapter

    // View model for health metrics
    private val healthMetricsViewModel: HealthMetricsViewModel by viewModels()

    // Key for checking if this is the first time app is launched
    private val PREF_NAME = "HealthAssistantPrefs"
    private val KEY_FIRST_LAUNCH = "isFirstLaunch"

    // Animation properties
    private val animDuration = 1000L
    private val animDelay = 100L

    // Notification permission request code
    private val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * Clean up any duplicate activity cards for today
     */
    private fun cleanupDuplicateActivityCards() {
        lifecycleScope.launch {
            try {
                val userId = sessionManager.getCurrentUserId() ?: return@launch
                val today = LocalDate.now()
                
                val activityCardRepository = activityCardScheduler.getActivityCardRepository()
                val deletedCount = activityCardRepository.cleanupDuplicateActivityCards(today, userId)
                
                if (deletedCount > 0) {
                    Log.d("HomeFragment", "Cleaned up $deletedCount duplicate activity cards for today")
                }
            } catch (e: Exception) {
                Log.e("HomeFragment", "Error cleaning up duplicate activity cards", e)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Clean up any duplicate activity cards
        cleanupDuplicateActivityCards()
        
        // Check and request notification permissions
        checkNotificationPermissions()

        // NEW: Trigger daily data maintenance for weekly health management
        triggerDailyDataMaintenance()
        
        // OPTIMIZED: Use performance manager for smooth transitions
        performanceManager.lazyLoadUI(
            fragment = this,
            criticalViews = {
                // Load only essential UI elements immediately for fast transition
                setupGreetingSection()
                setupHealthSummary()
                setupQuickActions()
            },
            nonCriticalViews = {
                // Defer heavy operations to improve transition speed
                setupBackgroundEffects()
                setupContextualCard()
                setupWellnessInsights()
                setupCharts()
                loadProfilePhoto()
                loadUserProfileAndUpdateGreeting()
                setupHealthMetricsObservation()
                startDeviceSensorTracking()
                setupNotificationTestButtons() // Add test buttons for notifications
            },
            delayMs = 150L // Load non-critical views after transition completes
        )
    }


    override fun onResume() {
        super.onResume()
        // Refresh profile photo when returning to home (e.g., from EditProfileFragment)
        loadProfilePhoto()
        // Refresh greeting with updated display name
        loadUserProfileAndUpdateGreeting()
        // Reset and reload health metrics for current user
        resetHealthMetricsForCurrentUser()
        // CRITICAL FIX: Restart ring animations every time user enters home fragment
        restartRingAnimations()
    }

    /**
     * Reset and reload health metrics for the current user
     * This ensures proper user isolation when switching users
     */
    private fun resetHealthMetricsForCurrentUser() {
        try {
            // Reset the UI to default values first
            binding.tripleRingProgress.setStepsProgress(0, 9000)
            binding.tripleRingProgress.setCaloriesProgress(0, 300)
            binding.tripleRingProgress.setHeartPointsProgress(0, 50)

            // Reset text values
            binding.stepsValue.text = "0 / 9000 steps"
            binding.caloriesValue.text = "0 / 300 kcal"
            binding.heartPointsValue.text = "0 / 50 points"

            // Force refresh the metrics from the ViewModel
            healthMetricsViewModel.refreshMetrics()
            
            // Log the current user
            val currentUserId = sessionManager.getCurrentUserId() ?: "no user"
            Log.d("HomeFragment", "Reset health metrics for user: $currentUserId")
            
            // Refresh charts with user-specific data
            setupCharts()
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error resetting health metrics", e)
        }
    }

    override fun onPause() {
        super.onPause()
        // CRITICAL FIX: Stop ring animations when leaving fragment to prevent crashes
        stopRingAnimations()
    }

    /**
     * CRITICAL FIX: Restart ring animations when user enters home fragment
     */
    private fun restartRingAnimations() {
        try {
            // Safe binding access - only proceed if view exists
            safeBinding?.let { binding ->
                // STEP 1: Reset all rings to 0 immediately (no animation)
                binding.tripleRingProgress.setStepsProgress(0, 9000)
                binding.tripleRingProgress.setCaloriesProgress(0, 300)
                binding.tripleRingProgress.setHeartPointsProgress(0, 50)

                // STEP 2: Update text values to 0 for fresh start
                binding.stepsValue.text = "0 / 9000 steps"
                binding.caloriesValue.text = "0 / 300 kcal"
                binding.heartPointsValue.text = "0 / 50 points"

                // STEP 3: Start animation sequence with minimal delays for instant feel
                viewLifecycleOwner.lifecycleScope.launch {
                    // Minimal delay for ring reset
                    kotlinx.coroutines.delay(50)

                    // Trigger health metrics refresh to get real data
                    healthMetricsViewModel.refreshMetrics()

                    // Minimal wait for data loading - make it feel instant
                    kotlinx.coroutines.delay(100)

                    // Get health data or use sample data
                    val currentMetrics = healthMetricsViewModel.healthMetrics.value
                    if (currentMetrics != null) {
                        // Animate with real health data using faster sequence
                        animateRingsSequentially(
                            currentMetrics.steps.current,
                            currentMetrics.calories.current,
                            currentMetrics.heartPoints.current
                        )
                    } else {
                        // Animate with sample data using faster sequence
                        val sampleSteps = (9000 * 0.75).toInt() // 75% of daily goal
                        val sampleCalories = (300 * 0.6).toInt() // 60% of daily goal
                        val sampleHeartPoints = (50 * 0.8).toInt() // 80% of daily goal

                        animateRingsSequentially(sampleSteps, sampleCalories, sampleHeartPoints)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error restarting ring animations", e)
        }
    }

    /**
     * Animate rings one by one with faster delays for real-time feel
     */
    private fun animateRingsSequentially(steps: Int, calories: Int, heartPoints: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Safe binding check for all updates
                safeBinding?.let { binding ->
                    // Animate steps ring first with instant update
                    binding.tripleRingProgress.setStepsProgress(steps, 9000)
                    binding.stepsValue.text = "$steps / 9000 steps"

                    // Much shorter delay for snappy feel
                    kotlinx.coroutines.delay(150)

                    // Animate calories ring second
                    binding.tripleRingProgress.setCaloriesProgress(calories, 300)
                    binding.caloriesValue.text = "$calories / 300 kcal"

                    // Shorter delay for calories animation
                    kotlinx.coroutines.delay(150)

                    // Animate heart points ring last
                    binding.tripleRingProgress.setHeartPointsProgress(heartPoints, 50)
                    binding.heartPointsValue.text = "$heartPoints / 50 points"
                }
            } catch (e: Exception) {
                Log.e("HomeFragment", "Error in sequential ring animation", e)
            }
        }
    }

    /**
     * CRITICAL FIX: Stop ring animations when leaving fragment
     */
    private fun stopRingAnimations() {
        try {
            // Cancel any ongoing coroutines to prevent crashes
            // The TripleRingProgressView automatically cancels animations in onDetachedFromWindow()
            // Reset values to 0 for clean state when returning
            binding.tripleRingProgress.setStepsProgress(0, 9000)
            binding.tripleRingProgress.setCaloriesProgress(0, 300)
            binding.tripleRingProgress.setHeartPointsProgress(0, 50)

            // Reset text values too
            binding.stepsValue.text = "0 / 9000 steps"
            binding.caloriesValue.text = "0 / 300 kcal"
            binding.heartPointsValue.text = "0 / 50 points"
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error stopping ring animations", e)
        }
    }

    /**
     * Load and display user's profile photo using the shared ProfilePhotoManager
     */
    private fun loadProfilePhoto() {
        // Load profile photo using the shared manager
        profilePhotoManager.loadProfilePhoto(
            context = requireContext(),
            imageView = binding.userAvatar,
            lifecycleOwner = viewLifecycleOwner,
            enableFullScreenClick = true
        )
    }

    /**
     * Load user profile and update greeting with real display name
     */
    private fun loadUserProfileAndUpdateGreeting() {
        lifecycleScope.launch {
            try {
                val result = userProfileRepository.getUserProfile()
                when (result) {
                    is Result.Success -> {
                        val profile = result.data
                        updateGreetingWithProfile(profile?.displayName)
                    }
                    is Result.Error -> {
                        // Fallback to email-based name if profile fetch fails
                        updateGreetingWithFallback()
                    }
                    is Result.Loading -> {
                        // Show loading state or keep current greeting
                    }
                }
            } catch (e: Exception) {
                // Handle error gracefully
                updateGreetingWithFallback()
                android.util.Log.e("HomeFragment", "Error loading user profile for greeting", e)
            }
        }
    }

    /**
     * Update greeting text with user's display name (first name only)
     */
    private fun updateGreetingWithProfile(displayName: String?) {
        val firstName = when {
            !displayName.isNullOrBlank() -> {
                // Extract first name from display name
                displayName.trim().split(" ").firstOrNull()?.takeIf { it.isNotBlank() }
            }
            else -> null
        }

        val greetingName = firstName ?: run {
            // Fallback to email-derived name
            sessionManager.getCurrentUserEmail()?.let { email ->
                email.substringBefore("@").replaceFirstChar { char ->
                    if (char.isLowerCase()) char.titlecase() else char.toString()
                }
            } ?: "User"
        }

        // Update greeting text with time-based greeting and first name
        binding.greetingText.text = "${getTimeBasedGreeting()}, $greetingName"
    }

    /**
     * Fallback method to update greeting when profile is not available
     */
    private fun updateGreetingWithFallback() {
        val userEmail = sessionManager.getCurrentUserEmail()
        val userName = userEmail?.let {
            it.substringBefore("@").replaceFirstChar { char ->
                if (char.isLowerCase()) char.titlecase() else char.toString()
            }
        } ?: "User"

        // Create styled greeting with username formatting
        setStyledGreeting(getTimeBasedGreeting(), userName)
    }

    /**
     * Sets styled greeting text with regular greeting and bold/colored username
     */
    private fun setStyledGreeting(greeting: String, userName: String) {
        val fullText = "$greeting, $userName"
        val spannable = android.text.SpannableString(fullText)

        // Find the start index of the username
        val userNameStart = fullText.indexOf(userName)
        val userNameEnd = userNameStart + userName.length

        if (userNameStart >= 0) {
            // Apply bold style to username
            spannable.setSpan(
                android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                userNameStart,
                userNameEnd,
                android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            // Apply black color to username for consistency
            spannable.setSpan(
                android.text.style.ForegroundColorSpan(
                    ContextCompat.getColor(requireContext(), android.R.color.black)
                ),
                userNameStart,
                userNameEnd,
                android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        binding.greetingText.text = spannable
    }

    /**
     * Sets up personalized greeting with user name and time-based greeting
     * Initial setup - will be updated by loadUserProfileAndUpdateGreeting()
     */
    private fun setupGreetingSection() {
        // Set initial greeting (will be updated with real profile data)
        updateGreetingWithFallback()

        // Set current date in friendly format
        val dateFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
        binding.dateText.text = dateFormat.format(Date())

        // Set up avatar click interaction
        binding.userAvatar.setOnClickListener {
            // Apply a quick pulse animation
            val scaleX = ObjectAnimator.ofFloat(binding.userAvatar, "scaleX", 1f, 0.9f, 1f)
            val scaleY = ObjectAnimator.ofFloat(binding.userAvatar, "scaleY", 1f, 0.9f, 1f)
            scaleX.duration = 300
            scaleY.duration = 300
            scaleX.interpolator = OvershootInterpolator()
            scaleY.interpolator = OvershootInterpolator()
            scaleX.start()
            scaleY.start()

            Toast.makeText(context, "View Profile", Toast.LENGTH_SHORT).show()
            // Navigate to profile in a real implementation
        }
    }

    /**
     * Sets up subtle animation effects for background decorative elements
     */
    private fun setupBackgroundEffects() {
        // Subtle floating animation for decorative shapes
        animateDecorativeShape(binding.decorativeShape1, 20f, 6000L, 0L)
        animateDecorativeShape(binding.decorativeShape2, 15f, 7000L, 1000L)
    }

    /**
     * Sets up the contextual card with wellness tip or weather
     */
    private fun setupContextualCard() {
        // Always show wellness tip instead of time-based weather/tip
        binding.contextualTitle.text = "Wellness Tip"

        // Rotate through different wellness tips
        val wellnessTips = listOf(
            "Stay hydrated! Aim for 8 glasses of water today.",
            "Take a 5-minute break every hour to stretch and move.",
            "Practice deep breathing for 2 minutes to reduce stress.",
            "Get 7-9 hours of quality sleep for better health.",
            "Eat a colorful variety of fruits and vegetables daily.",
            "Take a short walk after meals to aid digestion.",
            "Practice gratitude by writing down 3 things you're thankful for.",
            "Limit screen time before bed for better sleep quality.",
            "Stay active with at least 30 minutes of exercise daily.",
            "Take time to connect with friends and family today."
        )

        // Select a random tip or use day-based rotation
        val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        val selectedTip = wellnessTips[dayOfYear % wellnessTips.size]
        binding.contextualContent.text = selectedTip

        // Add click interaction
        binding.contextualCard.setOnClickListener {
            Toast.makeText(context, "More wellness tips coming soon!", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Sets up health summary section with triple-ring progress view
     */
    private fun setupHealthSummary() {
        // Initialize the triple-ring progress view with initial/empty values
        binding.tripleRingProgress.setStepsProgress(0, 9000)
        binding.tripleRingProgress.setCaloriesProgress(0, 300)
        binding.tripleRingProgress.setHeartPointsProgress(0, 50)

        // Initialize text values in the legend
        binding.stepsValue.text = "0 / 9000 steps"
        binding.caloriesValue.text = "0 / 300 kcal"
        binding.heartPointsValue.text = "0 / 50 points"

        // NEW: Add click handler for health rings to trigger activity card generation
        binding.tripleRingProgress.setOnClickListener {
            Log.d("HomeFragment", "Health rings clicked - triggering activity card generation")
            animatePressEffect(binding.tripleRingProgress)
            triggerActivityCardGeneration()
        }

        // Also add click handler to the health summary card
        binding.healthSummaryCard.setOnClickListener {
            Log.d("HomeFragment", "Health summary card clicked - triggering activity card generation")
            animatePressEffect(binding.healthSummaryCard)
            triggerActivityCardGeneration()
        }

        // Add long press handler to reset health metrics (for testing)
        binding.healthSummaryCard.setOnLongClickListener {
            resetHealthMetrics()
            true
        }
    }

    /**
     * Reset health metrics for the current user
     * This is used for testing to ensure proper user isolation
     */
    private fun resetHealthMetrics() {
        lifecycleScope.launch {
            try {
                val userId = sessionManager.getCurrentUserId() ?: return@launch
                val result = healthRepository.resetUserStepCount(userId)
                
                when (result) {
                    is Result.Success -> {
                        // Reset UI
                        resetHealthMetricsForCurrentUser()
                        // Show success message
                        showSnackbar("Health metrics reset for current user")
                        Log.d("HomeFragment", "Health metrics reset for user $userId")
                    }
                    is Result.Error -> {
                        showSnackbar("Failed to reset health metrics")
                        Log.e("HomeFragment", "Error resetting health metrics: ${result.message}")
                    }
                    is Result.Loading -> {
                        // Do nothing
                    }
                }
            } catch (e: Exception) {
                Log.e("HomeFragment", "Exception resetting health metrics", e)
                showSnackbar("Error resetting health metrics")
            }
        }
    }

    /**
     * Updates the health metrics UI based on the ViewModel data
     */
    private fun updateHealthMetrics(metrics: HealthMetrics) {
        // Update triple-ring progress view
        binding.tripleRingProgress.setStepsProgress(
            metrics.steps.current,
            metrics.steps.target
        )

        binding.tripleRingProgress.setCaloriesProgress(
            metrics.calories.current,
            metrics.calories.target
        )

        binding.tripleRingProgress.setHeartPointsProgress(
            metrics.heartPoints.current,
            metrics.heartPoints.target
        )

        // Update text values in the legend
        binding.stepsValue.text = "${metrics.steps.current} / ${metrics.steps.target} steps"
        binding.caloriesValue.text = "${metrics.calories.current} / ${metrics.calories.target} kcal"
        binding.heartPointsValue.text = "${metrics.heartPoints.current} / ${metrics.heartPoints.target} points"
    }


    /**
     * Sets up pill-shaped quick action buttons with animations
     */
    private fun setupQuickActions() {
        // Health Check button
        binding.healthCheckButton.setOnClickListener {
            animatePillButton(binding.healthCheckButton)
            Toast.makeText(context, "Starting Health Check", Toast.LENGTH_SHORT).show()
        }

        // Prescriptions button
        binding.prescriptionsButton.setOnClickListener {
            animatePillButton(binding.prescriptionsButton)
            findNavController().navigate(R.id.action_homeFragment_to_prescriptionsFragment)
        }

        // AI Assistant button
        binding.aiAssistantButton.setOnClickListener {
            animatePillButton(binding.aiAssistantButton)
            Toast.makeText(context, "Launching AI Assistant", Toast.LENGTH_SHORT).show()
        }

        // Emergency button
        binding.emergencyButton.setOnClickListener {
            animatePillButton(binding.emergencyButton)
            Toast.makeText(context, "Emergency Contacts", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Sets up wellness insights recycler view
     */
    private fun setupWellnessInsights() {
        // Setup RecyclerView
        binding.insightsRecycler.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.HORIZONTAL,
            false
        )

        // Create wellness tips (in a real app, from a repository)
        val wellnessTips = createDynamicWellnessTips()

        wellnessTipsAdapter = WellnessTipsAdapter(wellnessTips) { tip ->
            // Handle tip click with animation
            Toast.makeText(requireContext(), "Insight: ${tip.title}", Toast.LENGTH_SHORT).show()
        }

        binding.insightsRecycler.adapter = wellnessTipsAdapter
    }

    private fun setupCharts() {
        setupStepsChart()
        setupCaloriesChart()
        setupHeartPointsChart()
    }

    /**
     * Sets up the steps chart with real data from HealthMetricsViewModel
     */
    private fun setupStepsChart() {
        try {
            Log.d("HomeFragment", "Setting up steps chart...")
            lifecycleScope.launch {
                val startOfWeek = getStartOfWeek()
                val weeklyDataResult = healthRepository.getWeeklyStepData(startOfWeek.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate())
                if (weeklyDataResult is Result.Success) {
                    ChartManager.setupChart(binding.stepsBarChart, weeklyDataResult.data, "steps")
                    updateStepsChartSummary(weeklyDataResult.data) // FIXED: Add weekly goal progress update
                }
            }
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error setting up steps chart", e)
        }
    }

    // FIXED: Add missing method to update steps weekly goal progress
    private fun updateStepsChartSummary(weeklyData: List<DailyStepData>) {
        val total = weeklyData.sumOf { it.steps }
        val avg = if (weeklyData.isNotEmpty()) total / weeklyData.size else 0
        binding.totalStepsValue.text = getString(R.string.total_steps_format, total)
        binding.dailyAverageValue.text = getString(R.string.daily_average_steps_format, avg)

        // FIXED: Update weekly goal progress for steps
        val weeklyGoal = 63000 // 7 days * 9000 steps per day
        val progressPercentage = if (weeklyGoal > 0) ((total.toFloat() / weeklyGoal) * 100).toInt() else 0
        binding.weeklyGoalProgress.progress = progressPercentage.coerceAtMost(100)
        binding.weeklyGoalText.text = "$total / $weeklyGoal steps (${progressPercentage}%)"
    }

    /**
     * Sets up the calories chart.
     */
    private fun setupCaloriesChart() {
        try {
            Log.d("HomeFragment", "Setting up calories chart...")
            lifecycleScope.launch {
                val startOfWeek = getStartOfWeek()
                val weeklyCaloriesData = healthRepository.getWeeklyCaloriesData(startOfWeek)
                ChartManager.setupChart(binding.caloriesChart, weeklyCaloriesData, "calories")
                updateCaloriesChartSummary(weeklyCaloriesData)
            }
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error setting up calories chart", e)
        }
    }

    private fun updateCaloriesChartSummary(weeklyData: List<DailyStepData>) {
        val total = weeklyData.sumOf { it.calories }
        val avg = if (weeklyData.isNotEmpty()) total / weeklyData.size else 0
        binding.totalCaloriesValue.text = getString(R.string.total_calories_format, total)
        binding.dailyCaloriesAverageValue.text = getString(R.string.daily_average_calories_format, avg)

        // FIXED: Update weekly goal progress for calories
        val weeklyGoal = 2100 // 7 days * 300 calories per day
        val progressPercentage = if (weeklyGoal > 0) ((total.toFloat() / weeklyGoal) * 100).toInt() else 0
        binding.caloriesWeeklyGoalProgress.progress = progressPercentage.coerceAtMost(100)
        binding.caloriesWeeklyGoalText.text = "$total / $weeklyGoal kcal (${progressPercentage}%)"
    }

    /**
     * Sets up the heart points chart with proper data handling
     */
    private fun setupHeartPointsChart() {
        try {
            Log.d("HomeFragment", "Setting up heart points chart...")
            lifecycleScope.launch {
                val startOfWeek = getStartOfWeek()

                // FIXED: Get heart points data and handle empty case
                val weeklyHeartPointsData = healthRepository.getWeeklyHeartPointsData(startOfWeek)

                Log.d("HomeFragment", "Heart points weekly data size: ${weeklyHeartPointsData.size}")
                weeklyHeartPointsData.forEach { data ->
                    Log.d("HomeFragment", "Heart points data: ${data.date} = ${data.heartPoints} points")
                }

                if (weeklyHeartPointsData.isEmpty()) {
                    Log.w("HomeFragment", "No heart points data found, using sample data")
                    val sampleData = generateSampleHeartPointsData()
                    ChartManager.setupChart(binding.heartPointsChart, sampleData, "heartPoints")
                    updateHeartPointsChartSummary(sampleData)
                } else {
                    ChartManager.setupChart(binding.heartPointsChart, weeklyHeartPointsData, "heartPoints")
                    updateHeartPointsChartSummary(weeklyHeartPointsData)
                }
            }
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error setting up heart points chart", e)
            // Fallback to sample data if there's an error
            val sampleData = generateSampleHeartPointsData()
            ChartManager.setupChart(binding.heartPointsChart, sampleData, "heartPoints")
            updateHeartPointsChartSummary(sampleData)
        }
    }

    /**
     * Generate sample heart points data including today's actual value
     */
    private fun generateSampleHeartPointsData(): List<DailyStepData> {
        val today = java.time.LocalDate.now()
        val startOfWeek = today.minusDays(today.dayOfWeek.value.toLong() - 1)

        // Get current heart points from health metrics
        val currentHeartPoints = healthMetricsViewModel.healthMetrics.value?.heartPoints?.current ?: 15

        return (0..6).map { dayOffset ->
            val date = startOfWeek.plusDays(dayOffset.toLong())
            val isToday = date.isEqual(today)
            val isFuture = date.isAfter(today)

            DailyStepData(
                date = date,
                steps = if (isFuture) 0 else if (isToday) (3000..8000).random() else (4000..12000).random(),
                goal = 9000,
                calories = if (isFuture) 0 else if (isToday) (150..400).random() else (200..500).random(),
                caloriesGoal = 300,
                heartPoints = if (isFuture) 0 else if (isToday) currentHeartPoints else (5..45).random(),
                heartPointsGoal = 50
            )
        }
    }

    /**
     * Observe real-time step data updates to refresh chart automatically
     */
    private fun observeRealTimeStepUpdates() {
        lifecycleScope.launch {
            // Observe health metrics changes in real-time
            healthMetricsViewModel.healthMetrics.observe(viewLifecycleOwner) { metrics ->
                metrics?.let { currentMetrics ->
                    // Update today's step data in the repository and refresh chart
                    val today = java.time.LocalDate.now()
                    val todayStepData = DailyStepData(
                        date = today,
                        steps = currentMetrics.steps.current,
                        goal = currentMetrics.steps.target
                    )

                    // Save updated step data and refresh chart
                    lifecycleScope.launch {
                        healthRepository.saveDailyStepData(todayStepData)
                        // Refresh the chart with updated data
                        setupStepsChart()
                    }
                }
            }
        }
    }

    /**
     * Update the chart with real step data
     */
    private fun updateStepsChart(weeklyData: List<DailyStepData>) {
        try {
            // Setup the chart using our StepsChartManager with real data
            ChartManager.setupChart(binding.stepsBarChart, weeklyData, "steps")

            // Update the summary data in the UI
            updateStepsSummaryUI(WeeklyStepSummary(weeklyData))

            Log.d("HomeFragment", "Steps chart updated with real data - Total steps: ${weeklyData.sumOf { it.steps }}")

        } catch (e: Exception) {
            Log.e("HomeFragment", "Error updating steps chart", e)
        }
    }

    /**
     * Fallback method using sample data
     */
    private fun setupChartWithSampleData() {
        try {
            Log.w("HomeFragment", "Using sample data for steps chart")

            // Generate sample weekly data for demonstration
            val weeklyData = ChartManager.generateSampleWeeklyData()

            // Setup the chart using our StepsChartManager
            ChartManager.setupChart(binding.stepsBarChart, weeklyData, "steps")

            // Update the summary data in the UI
            updateStepsSummaryUI(WeeklyStepSummary(weeklyData))

        } catch (e: Exception) {
            Log.e("HomeFragment", "Error setting up chart with sample data", e)
        }
    }

    /**
     * Updates the steps summary UI with weekly data
     */
    private fun updateStepsSummaryUI(summary: WeeklyStepSummary) {
        // Update total steps
        binding.totalStepsValue.text = "${formatNumber(summary.totalSteps)} steps"

        // Update daily average
        binding.dailyAverageValue.text = "${formatNumber(summary.dailyAverage)}/day"

        // Update weekly goal progress
        val progressPercentage = summary.weeklyGoalPercentage.toInt()
        binding.weeklyGoalProgress.progress = progressPercentage
        binding.weeklyGoalText.text = "${formatNumber(summary.totalSteps)} / ${formatNumber(summary.weeklyGoal)} steps ($progressPercentage%)"
    }

    /**
     * Format numbers for display (e.g., 1000 -> 1,000)
     */
    private fun formatNumber(number: Int): String {
        return java.text.NumberFormat.getNumberInstance(Locale.getDefault()).format(number)
    }

    /**
     * Runs entrance animations in sequence for visual delight
     */
    private fun runEntranceAnimations() {
        // Animate in sequence for a premium feel
        animateAlpha(binding.greetingText, 0f, 1f, animDuration, 0)
        animateAlpha(binding.dateText, 0f, 1f, animDuration, animDelay)
        animateAlpha(binding.userAvatar, 0f, 1f, animDuration, animDelay * 2)
        animateAlpha(binding.contextualCard, 0f, 1f, animDuration, animDelay * 3)

        // Health card rises up with a slight delay
        binding.healthSummaryCard.translationY = 50f
        binding.healthSummaryCard.alpha = 0f
        binding.healthSummaryCard.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(animDuration)
            .setStartDelay(animDelay * 4)
            .setInterpolator(OvershootInterpolator(0.7f))
            .withEndAction {
                // Animate health score after card appears
                animateHealthScore(85)
            }
            .start()

        // Animate section titles and lists with staggered timing
        animateAlpha(binding.quickActionsTitle, 0f, 1f, animDuration, animDelay * 6)
        animateAlpha(binding.quickActionsScroll, 0f, 1f, animDuration, animDelay * 7)
        animateAlpha(binding.insightsTitle, 0f, 1f, animDuration, animDelay * 8)
        animateAlpha(binding.insightsRecycler, 0f, 1f, animDuration, animDelay * 9)
    }

    /**
     * Returns an appropriate greeting based on time of day
     */
    private fun getTimeBasedGreeting(): String {
        return when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 0..4 -> "Good night"      // 12 AM - 4 AM
            in 5..11 -> "Good morning"   // 5 AM - 11 AM
            in 12..16 -> "Good afternoon" // 12 PM - 4 PM
            in 17..23 -> "Good evening"  // 5 PM - 11 PM
            else -> "Good morning"       // Fallback (should never happen)
        }
    }

    /**
     * Creates wellness tips that could vary based on user context
     */
    private fun createDynamicWellnessTips(): List<WellnessTip> {
        // In a real app, these would be personalized based on user data
        return listOf(
            WellnessTip(
                id = "1",
                title = "Sleep Quality Analysis",
                shortDescription = "Your sleep improved 15% this week",
                imageResId = R.drawable.premium_gradient_background
            ),
            WellnessTip(
                id = "2",
                title = "Hydration Reminder",
                shortDescription = "You're 2 glasses behind your goal today",
                imageResId = R.drawable.premium_gradient_background
            ),
            WellnessTip(
                id = "3",
                title = "Stress Management",
                shortDescription = "Try this 5-minute breathing exercise",
                imageResId = R.drawable.premium_gradient_background
            ),
            WellnessTip(
                id = "4",
                title = "Activity Goal Progress",
                shortDescription = "70% toward your weekly goal",
                imageResId = R.drawable.premium_gradient_background
            )
        )
    }

    // Animation Helpers

    /**
     * Animates a floating effect for decorative shapes
     */
    private fun animateDecorativeShape(view: View, distance: Float, duration: Long, delay: Long) {
        val startY = view.translationY
        val animator = ValueAnimator.ofFloat(startY, startY + distance, startY)
        animator.addUpdateListener { animation ->
            view.translationY = animation.animatedValue as Float
        }
        animator.duration = duration
        animator.repeatCount = ValueAnimator.INFINITE
        animator.repeatMode = ValueAnimator.REVERSE
        animator.startDelay = delay
        animator.start()
    }

    /**
     * Creates a press effect animation for cards
     */
    private fun animatePressEffect(view: View) {
        view.animate()
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(100)
            .withEndAction {
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(200)
                    .start()
            }
            .start()
    }

    /**
     * Animates pill buttons for feedback
     */
    private fun animatePillButton(button: View) {
        button.animate()
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(100)
            .withEndAction {
                button.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(200)
                    .start()
            }
            .start()
    }

    /**
     * Helper for fade animations
     */
    private fun animateAlpha(view: View, fromAlpha: Float, toAlpha: Float, duration: Long, delay: Long) {
        view.alpha = fromAlpha
        view.animate()
            .alpha(toAlpha)
            .setDuration(duration)
            .setStartDelay(delay)
            .start()
    }

    /**
     * Animates all three health metrics rings to show progress
     */
    private fun animateHealthScore(overallHealthScore: Int) {
        // Convert overall health score to individual metrics
        val stepsProgress = overallHealthScore * 90 / 100
        val caloriesProgress = overallHealthScore * 8 / 100
        val heartPointsProgress = overallHealthScore * 10 / 100

        // Update progress in the triple ring view
        binding.tripleRingProgress.setStepsProgress(stepsProgress, 9000)
        binding.tripleRingProgress.setCaloriesProgress(caloriesProgress, 300)
        binding.tripleRingProgress.setHeartPointsProgress(heartPointsProgress, 50)

        // Update the text displays
        updateMetricText(binding.stepsValue, stepsProgress, 9000, "steps")
        updateMetricText(binding.caloriesValue, caloriesProgress, 300, "kcal")
        updateMetricText(binding.heartPointsValue, heartPointsProgress, 50, "points")
    }

    /**
     * Helper method to update metric text with proper formatting
     */
    private fun updateMetricText(textView: android.widget.TextView, current: Int, target: Int, unit: String) {
        textView.text = getString(R.string.metric_format, current, target, unit)
    }

    /**
     * Starts tracking health metrics using device sensors
     */
    private fun startDeviceSensorTracking() {
        Toast.makeText(requireContext(), "Started tracking health metrics", Toast.LENGTH_SHORT).show()
    }

    /**
     * Sets up observation of health metrics from the ViewModel
     */
    private fun setupHealthMetricsObservation() {
        // CRITICAL FIX: Observe health metrics changes in real-time
        healthMetricsViewModel.healthMetrics.observe(viewLifecycleOwner) { metrics ->
            metrics?.let {
                updateHealthRingsWithRealData(it)
                checkAndGenerateActivityCard(it)
            }
        }

        // CRITICAL FIX: Observe real-time step count flow directly from sensor manager
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                healthRepository.getRealTimeStepFlow().collect { stepCount ->
                    // CRITICAL: Update UI on main thread immediately with safe binding check
                    viewLifecycleOwner.lifecycleScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                        try {
                            // Safe binding access - only update if view still exists
                            safeBinding?.let { binding ->
                                // Update step display immediately as user walks
                                updateMetricText(binding.stepsValue, stepCount, 9000, "steps")

                                // Update ring progress with animation
                                binding.tripleRingProgress.setStepsProgress(stepCount, 9000)

                                Log.d("HomeFragment", "Real-time step update: $stepCount")

                                // Refresh full metrics to get updated calories and heart points
                                healthMetricsViewModel.refreshMetrics()
                            }
                        } catch (e: Exception) {
                            Log.e("HomeFragment", "Error updating step UI", e)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("HomeFragment", "Error setting up real-time step flow", e)
            }
        }

        // OPTIMIZED: Set up ultra-fast refresh for maximum real-time feel
        viewLifecycleOwner.lifecycleScope.launch {
            while (viewLifecycleOwner.lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)) {
                try {
                    kotlinx.coroutines.delay(250) // Check every 250ms for ultra-responsive feel

                    // OPTIMIZED: Get current steps directly from health metrics
                    healthMetricsViewModel.refreshMetrics()
                    val currentMetrics = healthMetricsViewModel.healthMetrics.value
                    val currentSteps = currentMetrics?.steps?.current ?: 0

                    // Update UI on main thread with safe binding check - instant updates
                    viewLifecycleOwner.lifecycleScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                        safeBinding?.let { binding ->
                            // Instant UI updates without animation delays
                            updateMetricText(binding.stepsValue, currentSteps, 9000, "steps")
                            binding.tripleRingProgress.setStepsProgress(currentSteps, 9000)

                            // Also update calories and heart points in real-time
                            currentMetrics?.let { metrics ->
                                updateMetricText(binding.caloriesValue, metrics.calories.current, metrics.calories.target, "kcal")
                                updateMetricText(binding.heartPointsValue, metrics.heartPoints.current, metrics.heartPoints.target, "points")
                                binding.tripleRingProgress.setCaloriesProgress(metrics.calories.current, metrics.calories.target)
                                binding.tripleRingProgress.setHeartPointsProgress(metrics.heartPoints.current, metrics.heartPoints.target)
                            }

                            Log.d("HomeFragment", "Ultra-fast refresh - Steps: $currentSteps")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("HomeFragment", "Error in ultra-fast refresh", e)
                    break
                }
            }
        }
    }

    /**
     * Check and generate activity card based on health metrics
     */
    private fun checkAndGenerateActivityCard(metrics: HealthMetrics) {
        try {
            // Generate activity card with current metrics
            val stepCount = metrics.steps.current
            val caloriesBurned = metrics.calories.current
            val heartPoints = metrics.heartPoints.current

            // If metrics are significant, generate an activity card
            if (stepCount > 100 || caloriesBurned > 50 || heartPoints > 5) {
                Log.d("HomeFragment", "Generating activity card with metrics: Steps=$stepCount, Calories=$caloriesBurned, HeartPoints=$heartPoints")
                activityCardScheduler.forceGenerateCardForToday()
            }
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error generating activity card", e)
        }
    }

    /**
     * Get last recorded steps from SharedPreferences
     */
    private fun getLastRecordedSteps(): Int {
        val prefs = requireContext().getSharedPreferences("activity_card_prefs", Context.MODE_PRIVATE)
        return prefs.getInt("last_recorded_steps", 0)
    }

    /**
     * Save last recorded steps to SharedPreferences
     */
    private fun saveLastRecordedSteps(steps: Int) {
        val prefs = requireContext().getSharedPreferences("activity_card_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("last_recorded_steps", steps).apply()
    }

    /**
     * CRITICAL FIX: Update health rings with real data and proper animation
     */
    private fun updateHealthRingsWithRealData(metrics: HealthMetrics) {
        try {
            // Update text values immediately
            updateMetricText(binding.stepsValue, metrics.steps.current, metrics.steps.target, "steps")
            updateMetricText(binding.caloriesValue, metrics.calories.current, metrics.calories.target, "kcal")
            updateMetricText(binding.heartPointsValue, metrics.heartPoints.current, metrics.heartPoints.target, "points")

            // Update ring progress with smooth animation
            binding.tripleRingProgress.setStepsProgress(metrics.steps.current, metrics.steps.target)
            binding.tripleRingProgress.setCaloriesProgress(metrics.calories.current, metrics.calories.target)
            binding.tripleRingProgress.setHeartPointsProgress(metrics.heartPoints.current, metrics.heartPoints.target)

            // FIXED: Save heart points data to repository
            saveCurrentHeartPointsData(metrics)

        } catch (e: Exception) {
            Log.e("HomeFragment", "Error updating health rings", e)
        }
    }

    /**
     * Save current heart points data to repository
     */
    private fun saveCurrentHeartPointsData(metrics: HealthMetrics) {
        lifecycleScope.launch {
            try {
                val today = java.time.LocalDate.now()
                val todayData = DailyStepData(
                    date = today,
                    steps = metrics.steps.current,
                    goal = metrics.steps.target,
                    calories = metrics.calories.current,
                    caloriesGoal = metrics.calories.target,
                    heartPoints = metrics.heartPoints.current,
                    heartPointsGoal = metrics.heartPoints.target
                )

                // Save updated data to repository
                healthRepository.saveDailyStepData(todayData)

                // Log the saved heart points for debugging
                val userId = sessionManager.getCurrentUserId() ?: "no user"
                Log.d("HomeFragment", "Saved health metrics for user $userId - Steps: ${metrics.steps.current}, Calories: ${metrics.calories.current}, Heart Points: ${metrics.heartPoints.current}")

            } catch (e: Exception) {
                Log.e("HomeFragment", "Error saving heart points data", e)
            }
        }
    }

    /**
     * Get start of current week (Monday)
     */
    private fun getStartOfWeek(): Date {
        val calendar = Calendar.getInstance()
        calendar.firstDayOfWeek = Calendar.MONDAY
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.time
    }

    /**
     * NEW: Trigger daily data maintenance for weekly health management
     */
    private fun triggerDailyDataMaintenance() {
        lifecycleScope.launch {
            try {
                val userId = sessionManager.getCurrentUserEmail() ?: "default_user"
                healthRepository.performDailyDataMaintenance(userId)
                Log.d("HomeFragment", "Daily data maintenance completed")
            } catch (e: Exception) {
                Log.e("HomeFragment", "Error in daily data maintenance", e)
            }
        }
    }

    /**
     * Check and request notification permissions for Android 13+
     */
    private fun checkNotificationPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }
    }

    /**
     * TESTING: Add notification test buttons for development
     */
    private fun setupNotificationTestButtons() {
        // This is for testing notifications during development
        if (BuildConfig.DEBUG) {
            // Add test functionality if needed
            Log.d("HomeFragment", "Debug mode: Notification test buttons available")
        }
    }

    private fun updateHeartPointsChartSummary(weeklyData: List<DailyStepData>) {
        val total = weeklyData.sumOf { it.heartPoints }
        val avg = if (weeklyData.isNotEmpty()) total / weeklyData.size else 0
        binding.totalHeartPointsValue.text = getString(R.string.total_heart_points_format, total)
        binding.dailyHeartPointsAverageValue.text = getString(R.string.daily_average_heart_points_format, avg)

        // FIXED: Update weekly goal progress for heart points
        val weeklyGoal = 350 // 7 days * 50 heart points per day
        val progressPercentage = if (weeklyGoal > 0) ((total.toFloat() / weeklyGoal) * 100).toInt() else 0
        binding.heartPointsWeeklyGoalProgress.progress = progressPercentage.coerceAtMost(100)
        binding.heartPointsWeeklyGoalText.text = "$total / $weeklyGoal points (${progressPercentage}%)"
    }

    /**
     * Trigger activity card generation
     */
    private fun triggerActivityCardGeneration() {
        try {
            val metrics = healthMetricsViewModel.healthMetrics.value ?: return
            val stepCount = metrics.steps.current
            val caloriesBurned = metrics.calories.current
            val heartPoints = metrics.heartPoints.current
            
            // Get current user ID
            val userId = sessionManager.getCurrentUserId()
            if (userId.isNullOrEmpty()) {
                Log.w("HomeFragment", "No user logged in, using default user ID")
            }

            // Force generate an activity card with current metrics
            Log.d("HomeFragment", "Manually triggering activity card with metrics: Steps=$stepCount, Calories=$caloriesBurned, HeartPoints=$heartPoints for user $userId")
            activityCardScheduler.forceGenerateCardForToday()

            // Show success message
            showSnackbar("Activity card generated successfully")
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error triggering activity card", e)
            showSnackbar("Failed to generate activity card")
        }
    }

    /**
     * Show a snackbar message
     */
    private fun showSnackbar(message: String) {
        com.google.android.material.snackbar.Snackbar.make(
            binding.root,
            message,
            com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
        ).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}