package com.example.health_assistant.features.home

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.health_assistant.R
import com.example.health_assistant.auth.session.SessionManager
import com.example.health_assistant.core.util.Result
import com.example.health_assistant.data.repository.interfaces.UserProfileRepository
import com.example.health_assistant.databinding.FragmentHomeBinding
import com.example.health_assistant.features.health.model.HealthMetrics
import com.example.health_assistant.features.health.viewmodel.HealthMetricsViewModel
import com.example.health_assistant.features.home.adapters.WellnessTipsAdapter
import com.example.health_assistant.features.home.models.WellnessTip
import com.example.health_assistant.utils.ProfilePhotoManager
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

    private lateinit var wellnessTipsAdapter: WellnessTipsAdapter

    // View model for health metrics
    private val healthMetricsViewModel: HealthMetricsViewModel by viewModels()

    // Key for checking if this is the first time app is launched
    private val PREF_NAME = "HealthAssistantPrefs"
    private val KEY_FIRST_LAUNCH = "isFirstLaunch"

    // Animation properties
    private val animDuration = 1000L
    private val animDelay = 100L

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

        // Initialize UI elements with animation sequence
        setupGreetingSection()
        setupBackgroundEffects()
        setupContextualCard()
        setupHealthSummary()
        setupQuickActions()
        setupWellnessInsights()
        loadProfilePhoto()

        // Load user profile and update greeting in real-time
        loadUserProfileAndUpdateGreeting()

        // Observe health metrics data
        healthMetricsViewModel.healthMetrics.observe(viewLifecycleOwner, Observer { metrics ->
            // Update UI with health metrics - handle nullable metrics
            metrics?.let {
                updateHealthMetrics(it)
            }
        })

        // Check SharedPreferences to determine if animations should run
        val sharedPreferences = requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val isFirstLaunch = sharedPreferences.getBoolean(KEY_FIRST_LAUNCH, true)

        if (isFirstLaunch) {
            // Run entrance animations for first launch only
            runEntranceAnimations()

            // Update SharedPreferences to indicate that the app has been launched at least once
            sharedPreferences.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply()
        } else {
            // Skip entrance animations on subsequent launches
            binding.greetingText.alpha = 1f
            binding.dateText.alpha = 1f
            binding.userAvatar.alpha = 1f
            binding.contextualCard.alpha = 1f
            binding.healthSummaryCard.alpha = 1f
            binding.quickActionsTitle.alpha = 1f
            binding.quickActionsScroll.alpha = 1f
            binding.insightsTitle.alpha = 1f
            binding.insightsRecycler.alpha = 1f

            // Still animate the health score progress bar for visual appeal
            // We do this separately from entrance animations for subsequent visits
            view.post {
                animateHealthScore(85) // Using 85 as the value, replace with actual data source
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh profile photo when returning to home (e.g., from EditProfileFragment)
        loadProfilePhoto()
        // Refresh greeting with updated display name
        loadUserProfileAndUpdateGreeting()
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
            sessionManager.getUserEmail()?.let { email ->
                email.substringBefore("@").replaceFirstChar { char ->
                    if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString()
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
        val userEmail = sessionManager.getUserEmail()
        val userName = userEmail?.let {
            it.substringBefore("@").replaceFirstChar { char ->
                if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString()
            }
        } ?: "User"

        binding.greetingText.text = "${getTimeBasedGreeting()}, $userName"
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}