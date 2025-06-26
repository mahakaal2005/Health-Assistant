package com.example.health_assistant.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.health_assistant.R
import com.example.health_assistant.auth.AuthActivity
import com.example.health_assistant.auth.session.SessionManager
import com.example.health_assistant.databinding.SplashActivityBinding
import com.example.health_assistant.main.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {
    private lateinit var binding: SplashActivityBinding

    @Inject
    lateinit var sessionManager: SessionManager

    private val handler = Handler(Looper.getMainLooper())
    private var isNavigationScheduled = false
    private val TAG = "SplashActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SplashActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Hide the action bar if it's present
        supportActionBar?.hide()

        // Configure the Lottie animation
        setupAnimation()

        // Start session check and navigation logic
        scheduleNavigation()
    }

    private fun setupAnimation() {
        with(binding.animationView) {
            // Use the resource ID directly instead of string path
            setAnimation(R.raw.health_animation)

            // Calculate the speed needed for animation to complete exactly in SPLASH_DELAY ms
            addAnimatorUpdateListener {
                // This will be triggered when animation loads and we can get its duration
                if (duration > 0) {
                    // Only adjust speed if we haven't already
                    if (speed == 1f) {
                        val animationSpeed = duration / SPLASH_DELAY.toFloat()
                        speed = animationSpeed
                        removeAllUpdateListeners() // No need to keep checking
                    }
                }
            }

            // Ensure we play the animation exactly once
            repeatCount = 0

            // Start the animation
            playAnimation()
        }
    }

    private fun scheduleNavigation() {
        if (isNavigationScheduled) return
        isNavigationScheduled = true

        // Check session state asynchronously
        lifecycleScope.launch {
            try {
                val isLoggedIn = sessionManager.isLoggedInAsync()

                // Wait for minimum splash duration to complete
                handler.postDelayed({
                    navigateToNextActivity(isLoggedIn)
                }, SPLASH_DELAY)

            } catch (e: Exception) {
                Log.e(TAG, "Error checking session state: ${e.message}")
                // On error, default to auth flow
                handler.postDelayed({
                    navigateToNextActivity(false)
                }, SPLASH_DELAY)
            }
        }
    }

    private fun navigateToNextActivity(isLoggedIn: Boolean) {
        try {
            // Check if activity is finishing before starting a new one
            if (isFinishing || isDestroyed) return

            val intent = if (isLoggedIn) {
                Log.d(TAG, "User is logged in, navigating to MainActivity")
                Intent(this, MainActivity::class.java)
            } else {
                Log.d(TAG, "User is not logged in, navigating to AuthActivity")
                Intent(this, AuthActivity::class.java)
            }

            // Add flags to clear activity stack and start fresh
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)

            // Use a smooth animation transition
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish() // Close SplashActivity so it's not in the back stack

        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to next activity: ${e.message}")
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        // Remove callbacks to prevent memory leaks and crashes
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    companion object {
        private const val SPLASH_DELAY = 4000L // 4 seconds
    }
}