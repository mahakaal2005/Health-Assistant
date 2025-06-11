package com.example.health_assistant.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.health_assistant.R
import com.example.health_assistant.auth.AuthActivity
import com.example.health_assistant.databinding.SplashActivityBinding

class SplashActivity : AppCompatActivity() {
    private lateinit var binding: SplashActivityBinding
    private val handler = Handler(Looper.getMainLooper())
    private val navigationRunnable = Runnable {
        try {
            // Check if activity is finishing before starting a new one
            if (!isFinishing && !isDestroyed) {
                val intent = Intent(this, AuthActivity::class.java)
                // Add flags to clear activity stack and start fresh
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                // Use a smooth animation transition
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                finish() // Close SplashActivity so it's not in the back stack
            }
        } catch (e: Exception) {
            // Log error (in a real app, use proper logging)
            e.printStackTrace()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SplashActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Hide the action bar if it's present
        supportActionBar?.hide()

        // Configure the Lottie animation
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

        // Post delayed navigation with handler
        handler.postDelayed(navigationRunnable, SPLASH_DELAY)
    }

    override fun onDestroy() {
        // Remove callbacks to prevent memory leaks and crashes
        handler.removeCallbacks(navigationRunnable)
        super.onDestroy()
    }

    companion object {
        private const val SPLASH_DELAY = 4000L // 4 seconds
    }
}