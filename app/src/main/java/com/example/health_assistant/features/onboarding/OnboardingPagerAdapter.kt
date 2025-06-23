package com.example.health_assistant.features.onboarding

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.health_assistant.R
import com.example.health_assistant.databinding.OnboardingPageBinding

/**
 * Adapter for the onboarding ViewPager2 that displays onboarding content
 */
class OnboardingPagerAdapter : RecyclerView.Adapter<OnboardingPagerAdapter.OnboardingPageViewHolder>() {

    // Define onboarding page content
    private val pages = listOf(
        OnboardingPage(
            imageResId = R.drawable.premium_gradient_background, // Replace with actual illustration
            titleResId = R.string.feature_monitoring_title,
            descriptionResId = R.string.feature_monitoring_desc
        ),
        OnboardingPage(
            imageResId = R.drawable.premium_gradient_background, // Replace with actual illustration
            titleResId = R.string.feature_chatbot_title,
            descriptionResId = R.string.feature_chatbot_desc
        ),
        OnboardingPage(
            imageResId = R.drawable.premium_gradient_background, // Replace with actual illustration
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
                illustrationImage.setImageResource(page.imageResId)
                titleText.setText(page.titleResId)
                descriptionText.setText(page.descriptionResId)
            }
        }
    }

    /**
     * Data class representing content for a single onboarding page
     */
    data class OnboardingPage(
        val imageResId: Int,
        val titleResId: Int,
        val descriptionResId: Int
    )
}