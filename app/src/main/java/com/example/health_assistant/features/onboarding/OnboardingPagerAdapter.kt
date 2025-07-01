package com.example.health_assistant.features.onboarding

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.health_assistant.R
import com.example.health_assistant.databinding.OnboardingPageBinding

/**
 * Adapter for the onboarding ViewPager2 that displays onboarding content with Lottie animations
 */
class OnboardingPagerAdapter(
    private val onGetStartedClick: () -> Unit
) : RecyclerView.Adapter<OnboardingPagerAdapter.OnboardingPageViewHolder>() {

    // Define onboarding page content with updated titles and Lottie animations
    private val pages = listOf(
        OnboardingPage(
            lottieRawRes = R.raw.end_to_end,
            titleResId = R.string.feature_monitoring_title,
            descriptionResId = R.string.feature_monitoring_desc
        ),
        OnboardingPage(
            lottieRawRes = R.raw.ai_chatbot,
            titleResId = R.string.feature_chatbot_title,
            descriptionResId = R.string.feature_chatbot_desc
        ),
        OnboardingPage(
            lottieRawRes = R.raw.emergency_alerts,
            titleResId = R.string.feature_emergency_title,
            descriptionResId = R.string.feature_emergency_desc
        )
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnboardingPageViewHolder {
        val binding = OnboardingPageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return OnboardingPageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OnboardingPageViewHolder, position: Int) {
        holder.bind(pages[position])
    }

    override fun getItemCount(): Int = pages.size

    inner class OnboardingPageViewHolder(private val binding: OnboardingPageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(page: OnboardingPage) {
            binding.apply {
                // Set Lottie animation instead of image
                illustrationLottie.setAnimation(page.lottieRawRes)
                illustrationLottie.playAnimation()

                titleText.setText(page.titleResId)
                descriptionText.setText(page.descriptionResId)

                // Show "Get Started" button only on the last page
                val isLastPage = adapterPosition == itemCount - 1
                getStartedButton.visibility = if (isLastPage) View.VISIBLE else View.GONE

                // Set click listener for Get Started button
                if (isLastPage) {
                    getStartedButton.setOnClickListener {
                        onGetStartedClick()
                    }
                }
            }
        }
    }

    /**
     * Data class representing content for a single onboarding page with Lottie animation
     */
    data class OnboardingPage(
        val lottieRawRes: Int,
        val titleResId: Int,
        val descriptionResId: Int
    )
}