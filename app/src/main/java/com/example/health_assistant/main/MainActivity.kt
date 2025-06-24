package com.example.health_assistant.main

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
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

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initializeNavigation()
    }

    private fun initializeNavigation() {
        try {
            // Set up the NavHostFragment and NavController for navigation
            val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragment_container) as NavHostFragment
            navController = navHostFragment.navController

            // Get the navigation graph and explicitly set home as the start destination
            val navInflater = navController.navInflater
            val graph = navInflater.inflate(R.navigation.nav_main)
            graph.setStartDestination(R.id.homeFragment)
            navController.graph = graph

            // Check if we need to force navigation to home fragment
            val skipAccountDecision = intent.getBooleanExtra(EXTRA_SKIP_ACCOUNT_DECISION, false)
            if (skipAccountDecision) {
                Log.d(TAG, "Skipping account decision, navigating directly to home")
                // Clear any existing back stack and navigate to home
                navController.navigate(R.id.homeFragment) {
                    popUpTo(navController.graph.startDestinationId) {
                        inclusive = true
                    }
                }
            }

            // Set up top-level destinations (no back button shown for these destinations)
            appBarConfiguration = AppBarConfiguration(
                setOf(
                    R.id.homeFragment,
                    R.id.discoverFragment,  // Changed from settingsFragment to match the bottom nav menu
                    R.id.journalFragment,
                    R.id.profileFragment
                )
            )

            // First, directly connect bottom navigation with navigation controller
            binding.bottomNav.setupWithNavController(navController)

            // Then, add a destination change listener to sync the UI
            navController.addOnDestinationChangedListener { _, destination, _ ->
                try {
                    // Update bottom nav selection based on current destination
                    when (destination.id) {
                        R.id.homeFragment,
                        R.id.discoverFragment,
                        R.id.journalFragment,
                        R.id.profileFragment -> {
                            binding.bottomNav.visibility = View.VISIBLE

                            // Only update the selected item if it doesn't match current destination
                            if (binding.bottomNav.selectedItemId != destination.id) {
                                binding.bottomNav.menu.findItem(destination.id)?.isChecked = true
                            }
                        }
                        else -> {
                            // For other destinations outside the main tabs, keep the bottom nav visible
                            binding.bottomNav.visibility = View.VISIBLE
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in destination changed listener: ${e.message}")
                }
            }

            // Add custom listener to handle all tab navigation properly
            binding.bottomNav.setOnItemSelectedListener { item ->
                val currentDestId = navController.currentDestination?.id

                // Skip navigation if we're already on this tab
                if (currentDestId == item.itemId) {
                    return@setOnItemSelectedListener true
                }

                try {
                    when (item.itemId) {
                        R.id.homeFragment,
                        R.id.discoverFragment,
                        R.id.journalFragment,
                        R.id.profileFragment -> {
                            // Clear back stack before navigating
                            navController.popBackStack(navController.graph.startDestinationId, false)
                            navController.navigate(item.itemId)
                        }
                    }
                    return@setOnItemSelectedListener true
                } catch (e: Exception) {
                    Log.e(TAG, "Navigation error: ${e.message}")
                    e.printStackTrace()
                    return@setOnItemSelectedListener false
                }
            }

            // Make sure the imports at the top of the file include:
            // import android.view.View

            // Explicitly set the selected item to home for first launch
            if (navController.currentDestination?.id == R.id.homeFragment) {
                binding.bottomNav.selectedItemId = R.id.homeFragment
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error initializing navigation: ${e.message}")
            e.printStackTrace()
        }
    }

    // Handle Up navigation with NavController
    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}