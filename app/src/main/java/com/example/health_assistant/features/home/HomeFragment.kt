package com.example.health_assistant.features.home

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.health_assistant.R
import com.example.health_assistant.auth.session.SessionManager
import com.example.health_assistant.databinding.FragmentHomeBinding
import com.example.health_assistant.features.home.adapters.WellnessTipsAdapter
import com.example.health_assistant.features.home.models.WellnessTip
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.Random

/**
 * Premium Home Fragment featuring a modern interface with personalized greeting,
 * health summary, quick actions, and wellness insights.
 * Follows premium UI/UX design principles from top health apps.
 */
class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var sessionManager: SessionManager
    private lateinit var wellnessTipsAdapter: WellnessTipsAdapter

    // Animation properties
    private val animDuration = 1000L
    private val animDelay = 100L

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        sessionManager = SessionManager(requireContext())
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

        // Run entrance animations
        runEntranceAnimations()
    }

    /**
     * Sets up personalized greeting with user name and time-based greeting
     */
    private fun setupGreetingSection() {
        // Set user name if available with proper capitalization
        val userEmail = sessionManager.getUserEmail()
        userEmail?.let {
            val userName = it.substringBefore("@").replaceFirstChar { char ->
                if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString()
            }
            binding.greetingText.text = "${getTimeBasedGreeting()}, $userName"
        }

        // Set current date in friendly format
        val dateFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
        binding.dateText.text = dateFormat.format(Date())

        // Set up avatar click interaction
        binding.avatarContainer.setOnClickListener {
            // Apply a quick pulse animation
            val scaleX = ObjectAnimator.ofFloat(binding.avatarContainer, "scaleX", 1f, 0.9f, 1f)
            val scaleY = ObjectAnimator.ofFloat(binding.avatarContainer, "scaleY", 1f, 0.9f, 1f)
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
        // In a real implementation, this would be dynamic based on time, weather API, etc.
        val isMorning = Calendar.getInstance().get(Calendar.HOUR_OF_DAY) < 12

        if (isMorning) {
            // Weather card in morning
            binding.contextualIcon.setImageResource(android.R.drawable.ic_menu_compass)
            binding.contextualIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.colorPrimaryGradientEnd))
            binding.contextualTitle.text = "Today's Weather"
            binding.contextualContent.text = "Sunny • 72°F • Perfect for a walk outside!"
        } else {
            // Wellness tip in afternoon/evening
            binding.contextualIcon.setImageResource(android.R.drawable.ic_menu_info_details)
            binding.contextualIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.colorPrimaryGradientStart))
            binding.contextualTitle.text = "Wellness Tip"
            binding.contextualContent.text = "Take 5 minutes to meditate before bed for better sleep quality."
        }

        // Add click interaction
        binding.contextualCard.setOnClickListener {
            Toast.makeText(context, "More information", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Sets up health summary section with animated progress indicator
     */
    private fun setupHealthSummary() {
        // In a real app, these values would come from health data repositories
        val healthScore = 85
        val steps = 7548
        val heartRate = 72

        // Set initial values
        binding.healthScoreValue.text = healthScore.toString()
        binding.stepsValue.text = steps.toString().chunked(1).joinToString(",")
        binding.heartRateValue.text = "$heartRate bpm"

        // Set up click handlers with haptic feedback and animations
        binding.stepsCard.setOnClickListener {
            animatePressEffect(binding.stepsCard)
            Toast.makeText(context, "View step details", Toast.LENGTH_SHORT).show()
        }

        binding.heartRateCard.setOnClickListener {
            animatePressEffect(binding.heartRateCard)
            Toast.makeText(context, "View heart rate history", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Animates the health score progress with a smooth circular animation
     */
    private fun animateHealthScore(targetValue: Int) {
        val animator = ObjectAnimator.ofInt(binding.healthScoreProgress, "progress", 0, targetValue)
        animator.duration = 1500
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.start()
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
            Toast.makeText(context, "Opening Prescriptions", Toast.LENGTH_SHORT).show()
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
        animateAlpha(binding.avatarContainer, 0f, 1f, animDuration, animDelay * 2)
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
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..21 -> "Good evening"
            else -> "Hello"
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}