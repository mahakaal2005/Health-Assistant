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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
        // CRITICAL FIX: Restart ring animations every time user enters home fragment
        restartRingAnimations()
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
            // STEP 1: Reset all rings to 0 immediately (no animation)
            binding.tripleRingProgress.setStepsProgress(0, 9000)
            binding.tripleRingProgress.setCaloriesProgress(0, 300)
            binding.tripleRingProgress.setHeartPointsProgress(0, 50)

            // STEP 2: Update text values to 0 for fresh start
            binding.stepsValue.text = "0 / 9000 steps"
            binding.caloriesValue.text = "0 / 300 kcal"
            binding.heartPointsValue.text = "0 / 50 points"

            // STEP 3: Start animation sequence with proper delays
            lifecycleScope.launch {
                // Wait longer to ensure rings are fully reset
                kotlinx.coroutines.delay(200)

                // Trigger health metrics refresh to get real data
                healthMetricsViewModel.refreshMetrics()

                // Wait for data loading
                kotlinx.coroutines.delay(400)

                // Get health data or use sample data
                val currentMetrics = healthMetricsViewModel.healthMetrics.value
                if (currentMetrics != null) {
                    // Animate with real health data using staggered sequence
                    animateRingsSequentially(
                        currentMetrics.steps.current,
                        currentMetrics.calories.current,
                        currentMetrics.heartPoints.current
                    )
                } else {
                    // Animate with sample data using staggered sequence
                    val sampleSteps = (9000 * 0.75).toInt() // 75% of daily goal
                    val sampleCalories = (300 * 0.6).toInt() // 60% of daily goal
                    val sampleHeartPoints = (50 * 0.8).toInt() // 80% of daily goal

                    animateRingsSequentially(sampleSteps, sampleCalories, sampleHeartPoints)
                }
            }
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error restarting ring animations", e)
        }
    }

    /**
     * Animate rings one by one with visible delays for smooth effect
     */
    private fun animateRingsSequentially(steps: Int, calories: Int, heartPoints: Int) {
        lifecycleScope.launch {
            try {
                // Animate steps ring first
                binding.tripleRingProgress.setStepsProgress(steps, 9000)
                binding.stepsValue.text = "$steps / 9000 steps"

                // Wait for steps animation to be visible
                kotlinx.coroutines.delay(300)

                // Animate calories ring second
                binding.tripleRingProgress.setCaloriesProgress(calories, 300)
                binding.caloriesValue.text = "$calories / 300 kcal"

                // Wait for calories animation to be visible
                kotlinx.coroutines.delay(300)

                // Animate heart points ring last
                binding.tripleRingProgress.setHeartPointsProgress(heartPoints, 50)
                binding.heartPointsValue.text = "$heartPoints / 50 points"

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
     * Sets up the heart points chart.
     */
    private fun setupHeartPointsChart() {
        try {
            Log.d("HomeFragment", "Setting up heart points chart...")
            lifecycleScope.launch {
                val startOfWeek = getStartOfWeek()
                val weeklyHeartPointsData = healthRepository.getWeeklyHeartPointsData(startOfWeek)
                ChartManager.setupChart(binding.heartPointsChart, weeklyHeartPointsData, "heartPoints")
                updateHeartPointsChartSummary(weeklyHeartPointsData)
            }
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error setting up heart points chart", e)
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
        // This is a simple example - in a real app this would use real health data
        val stepsProgress = overallHealthScore * 90 / 100  // 90% of health score converted to steps
        val caloriesProgress = overallHealthScore * 8 / 100 // 8% of health score converted to calories
        val heartPointsProgress = overallHealthScore * 10 / 100 // 10% of health score converted to heart points

        // Update progress in the triple ring view
        binding.tripleRingProgress.setStepsProgress(stepsProgress, 9000)
        binding.tripleRingProgress.setCaloriesProgress(caloriesProgress, 300)
        binding.tripleRingProgress.setHeartPointsProgress(heartPointsProgress, 50)

        // Update the text displays
        binding.stepsValue.text = "$stepsProgress / 9000 steps"
        binding.caloriesValue.text = "$caloriesProgress / 300 kcal"
        binding.heartPointsValue.text = "$heartPointsProgress / 50 points"
    }

    /**
     * Starts tracking health metrics using device sensors
     */
    private fun startDeviceSensorTracking() {
        // In a real app, start the necessary sensors to track health metrics
        // For example, step counter, heart rate monitor, etc.
        Toast.makeText(context, "Started tracking health metrics", Toast.LENGTH_SHORT).show()
    }

    /**
     * Sets up observation of health metrics from the ViewModel
     */
    private fun setupHealthMetricsObservation() {
        // Observe health metrics LiveData from the ViewModel
        healthMetricsViewModel.healthMetrics.observe(viewLifecycleOwner, Observer { metrics ->
            // Handle nullable metrics - only update UI if metrics are not null
            metrics?.let {
                updateHealthMetrics(it)
            }
        })

        // Observe error state
        healthMetricsViewModel.error.observe(viewLifecycleOwner, Observer { error ->
            error?.let {
                Log.e("HomeFragment", "Health metrics error: $it")
                Toast.makeText(context, "Health tracking error: $it", Toast.LENGTH_SHORT).show()
                healthMetricsViewModel.clearError()
            }
        })

        // Trigger initial refresh of health metrics using the correct method
        healthMetricsViewModel.refreshMetrics()

        /**
         * Add observer for real-time step data updates to refresh chart automatically
         */
        observeRealTimeStepUpdates()
    }


    /**
     * Check and request notification permissions for Android 13+
     */
    private fun checkNotificationPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permission already granted
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    // Show explanation to user
                    showNotificationPermissionRationale()
                }
                else -> {
                    // Request permission
                    requestPermissions(
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                        NOTIFICATION_PERMISSION_REQUEST_CODE
                    )
                }
            }
        }
    }

    /**
     * Show rationale for notification permission
     */
    private fun showNotificationPermissionRationale() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Enable Notifications")
            .setMessage("Get notified about your step progress and health achievements to stay motivated!")
            .setPositiveButton("Enable") { _, _ ->
                requestPermissions(
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
            }
            .setNegativeButton("Maybe Later") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            NOTIFICATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(context, "Notifications enabled! You'll get step progress updates.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Notifications disabled. You can enable them later in settings.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * Setup test buttons for notification functionality (for testing purposes)
     */
    private fun setupNotificationTestButtons() {
        // Add a simple click handler for testing activity card generation
        // The health rings will trigger activity card generation when clicked
        Log.d("HomeFragment", "Notification test setup complete - health rings clickable for testing")
    }

    /**
     * Trigger activity card generation using properly injected Hilt scheduler
     */
    private fun triggerActivityCardGeneration() {
        try {
            Log.d("HomeFragment", "🎯 TRIGGERING ACTIVITY CARD GENERATION")

            // CRITICAL FIX: Use Hilt-injected scheduler directly without WorkManager cache clearing
            // This ensures the HiltWorkerFactory is used properly
            activityCardScheduler.forceGenerateCardForToday()

            Log.d("HomeFragment", "✅ Activity card generation triggered successfully")

            // Show user feedback
            Snackbar.make(
                binding.root,
                "Generating activity card... Check Journal in a moment",
                Snackbar.LENGTH_LONG
            ).show()

        } catch (e: Exception) {
            Log.e("HomeFragment", "❌ Error triggering activity card generation", e)
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }


    /**
     * Test method - manually trigger activity card generation
     */
    private fun testGenerateActivityCard() {
        triggerActivityCardGeneration()
    }

    // NEW: Setup and update methods for calories and heart points charts

    private fun getStartOfWeek(): Date {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.time
    }

    /**
     * Trigger daily data maintenance for weekly health management
     */
    private fun triggerDailyDataMaintenance() {
        lifecycleScope.launch {
            try {
                Log.d("HomeFragment", "🛠️ TRIGGERING DAILY DATA MAINTENANCE")

                // Get current user ID from session
                val userId = sessionManager.getCurrentUserEmail() ?: "default_user"

                // Perform daily data maintenance through HealthRepository
                val maintenanceResult = healthRepository.performDailyDataMaintenance(userId)

                when (maintenanceResult) {
                    is Result.Success -> {
                        Log.d("HomeFragment", "✅ Daily data maintenance completed successfully")

                        // Refresh health metrics after maintenance
                        healthMetricsViewModel.refreshMetrics()

                        // Optionally show a subtle notification to user
                        // Toast.makeText(context, "Health data updated", Toast.LENGTH_SHORT).show()
                    }
                    is Result.Error -> {
                        Log.w("HomeFragment", "⚠️ Daily data maintenance encountered issues: ${maintenanceResult.message}")
                        // Don't show error to user as this is background maintenance
                    }
                    else -> {
                        Log.d("HomeFragment", "🔄 Daily data maintenance in progress")
                    }
                }

                Log.d("HomeFragment", "🏁 Daily data maintenance process completed")

            } catch (e: Exception) {
                Log.e("HomeFragment", "❌ Error triggering daily data maintenance", e)
                // Fail silently for background maintenance
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}