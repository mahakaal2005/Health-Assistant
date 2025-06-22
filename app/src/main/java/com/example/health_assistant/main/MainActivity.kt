package com.example.health_assistant.main

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupWithNavController
import com.example.health_assistant.R
import com.example.health_assistant.databinding.MainActivityBinding
import com.google.android.material.navigation.NavigationBarView

class MainActivity : AppCompatActivity() {
    private lateinit var binding: MainActivityBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration

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

        // Set up basic navigation first
        binding.bottomNav.setupWithNavController(navController)

        // Add custom listener to handle all tab navigation properly
        binding.bottomNav.setOnItemSelectedListener { item ->
            val currentDestId = navController.currentDestination?.id

            // Skip navigation if we're already on this tab (except for special cases)
            if (currentDestId == item.itemId) {
                return@setOnItemSelectedListener true
            }

            // For all navigation using bottom nav, clear backstack and navigate directly
            try {
                // Always pop to root first when switching tabs for consistent behavior
                navController.popBackStack(navController.graph.startDestinationId, false)
                navController.navigate(item.itemId)
                return@setOnItemSelectedListener true
            } catch (e: Exception) {
                e.printStackTrace()
                return@setOnItemSelectedListener false
            }
        }
    }

    // Handle Up navigation with NavController
    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}