package com.example.health_assistant.main

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupWithNavController
import com.example.health_assistant.R
import com.example.health_assistant.databinding.MainActivityBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: MainActivityBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration
    private val TAG = "MainActivity"

    companion object {
        private const val EXTRA_SKIP_ACCOUNT_DECISION = "skip_account_decision"

        // Static method to launch MainActivity and skip any account decision screens
        fun startWithHomeFragment(context: Context) {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra(EXTRA_SKIP_ACCOUNT_DECISION, true)
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        binding = MainActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Enable full screen experience after view is set
        setupFullScreen()

        // Update window insets handling to work with hidden status bar and display cutout
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val displayCutout = insets.getInsets(WindowInsetsCompat.Type.displayCutout())

            // Apply proper padding: display cutout top + left/right system bars + bottom navigation
            v.setPadding(
                systemBars.left,
                displayCutout.top, // Use display cutout top padding to avoid camera notch
                systemBars.right,
                systemBars.bottom
            )
            insets
        }

        initializeNavigation()
    }

    private fun initializeNavigation() {
        try {
            // Set up the NavHostFragment and NavController for navigation
            val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragment_container) as NavHostFragment
            navController = navHostFragment.navController

            // Set up top-level destinations (no back button shown for these destinations)
            appBarConfiguration = AppBarConfiguration(
                setOf(
                    R.id.homeFragment,
                    R.id.discoverFragment,
                    R.id.journalFragment,
                    R.id.profileFragment
                )
            )

            // Connect bottom navigation with navigation controller
            // This automatically handles navigation and syncing
            binding.bottomNav.setupWithNavController(navController)

            // Add destination change listener for UI visibility control
            navController.addOnDestinationChangedListener { _, destination, _ ->
                when (destination.id) {
                    R.id.homeFragment,
                    R.id.discoverFragment,
                    R.id.journalFragment,
                    R.id.profileFragment -> {
                        binding.bottomNav.visibility = View.VISIBLE
                    }
                    else -> {
                        binding.bottomNav.visibility = View.GONE
                    }
                }
            }

            Log.d(TAG, "Navigation initialized successfully")

        } catch (e: Exception) {
            Log.e(TAG, "Error initializing navigation: ${e.message}")
            e.printStackTrace()
        }
    }

    // Handle Up navigation with NavController
    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    // Setup full screen mode by hiding the status bar
    private fun setupFullScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // For Android 11 and above - modern WindowInsetsController approach
            val controller: WindowInsetsController = window.insetsController ?: return
            controller.hide(WindowInsets.Type.statusBars())
            controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            // For Android 10 and below - use system UI visibility flags
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
        }

        // Additional window optimizations for enhanced full screen experience
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // Enable drawing behind display cutouts for Android 9+
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
    }
}