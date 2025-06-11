package com.example.health_assistant.auth

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentContainerView
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import com.example.health_assistant.R
import com.example.health_assistant.databinding.AuthActivityBinding

class AuthActivity : AppCompatActivity() {
    private lateinit var binding: AuthActivityBinding
    private var navController: NavController? = null
    private val TAG = "AuthActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            // Initialize binding first
            binding = AuthActivityBinding.inflate(layoutInflater)
            setContentView(binding.root)

            // Decide on UI approach - either hide action bar for immersive UI or keep it for navigation
            // Option 1: Hide action bar (recommended for modern UI)
            supportActionBar?.hide()

            // Initialize navigation with safer approach
            initializeNavigation()

        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate: ${e.message}", e)
        }
    }

    private fun initializeNavigation() {
        try {
            // Find the NavHostFragment safely
            val navHostFragmentId = R.id.nav_host_fragment
            val navHostFragment = supportFragmentManager.findFragmentById(navHostFragmentId)

            if (navHostFragment is NavHostFragment) {
                // Standard navigation setup
                navController = navHostFragment.navController

                // Only setup ActionBar navigation if we're using the action bar
                if (supportActionBar?.isShowing == true) {
                    val appBarConfiguration = AppBarConfiguration(
                        setOf(R.id.onboardingFragment, R.id.accountDecisionFragment)
                    )
                    NavigationUI.setupActionBarWithNavController(this, navController!!, appBarConfiguration)
                }
            } else {
                // Fallback method if the NavHostFragment isn't found as expected
                val fragmentContainerView = findViewById<FragmentContainerView>(navHostFragmentId)
                if (fragmentContainerView != null) {
                    navController = Navigation.findNavController(fragmentContainerView)
                } else {
                    Log.e(TAG, "Navigation container view not found")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing navigation: ${e.message}", e)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        // Add null safety
        return navController?.navigateUp() ?: false || super.onSupportNavigateUp()
    }
}