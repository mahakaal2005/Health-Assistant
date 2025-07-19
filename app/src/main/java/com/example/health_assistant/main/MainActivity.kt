package com.example.health_assistant.main

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import com.example.health_assistant.R
import com.example.health_assistant.core.performance.FragmentPerformanceManager
import com.example.health_assistant.databinding.MainActivityBinding
import com.example.health_assistant.data.sync.ProfileSyncManager
import com.example.health_assistant.data.health.EnhancedHealthTracker
import com.example.health_assistant.features.discover.navigation.DiscoverDeepLinkHandler
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: MainActivityBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration
    private val TAG = "MainActivity"

    // Inject ProfileSyncManager for automatic Firestore synchronization
    @Inject
    lateinit var profileSyncManager: ProfileSyncManager

    // NEW: Inject FragmentPerformanceManager for ultra-smooth navigation
    @Inject
    lateinit var performanceManager: FragmentPerformanceManager

    // CRITICAL FIX: Inject EnhancedHealthTracker to ensure immediate sensor initialization
    @Inject
    lateinit var enhancedHealthTracker: EnhancedHealthTracker

    // Inject ActivityCardScheduler for manual card generation testing
    @Inject
    lateinit var activityCardScheduler: com.example.health_assistant.features.journal.workers.ActivityCardScheduler

    // Inject deep link handler for Discover content
    @Inject
    lateinit var discoverDeepLinkHandler: DiscoverDeepLinkHandler

    // CRITICAL FIX: Runtime permission handling for sensor access
    private val sensorPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val bodySensorsGranted = permissions[Manifest.permission.BODY_SENSORS] == true
        val activityRecognitionGranted = permissions[Manifest.permission.ACTIVITY_RECOGNITION] == true

        Log.d(TAG, "Sensor permissions result - Body Sensors: $bodySensorsGranted, Activity Recognition: $activityRecognitionGranted")

        if (bodySensorsGranted || activityRecognitionGranted) {
            // At least one permission granted, try to initialize health tracking
            initializeHealthTracking()
        } else {
            Log.w(TAG, "Sensor permissions denied - step tracking will not work")
        }
    }

    // NEW: Track navigation state for proper backstack management
    private var isHomeDestination = true
    private var lastNavigationTime = 0L
    private val NAVIGATION_THROTTLE_MS = 300L

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

        // Start automatic profile synchronization with Firestore
        initializeProfileSync()

        // CRITICAL FIX: Initialize health tracking immediately
        initializeHealthTracking()
        
        // Check for missing activity cards on app start
        activityCardScheduler.checkForMissingActivityCards()

        // NEW: Setup modern back button handling
        setupBackButtonHandler()

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
        
        // Handle deep links if this activity was started by one
        handleDeepLinkIfPresent()
    }

    private fun initializeNavigation() {
        try {
            // Set up the NavHostFragment and NavController for navigation
            val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragment_container) as NavHostFragment
            navController = navHostFragment.navController

            // NEW: Optimize navigation controller for ultra-smooth transitions
            performanceManager.optimizeNavController(navController)

            // Set up top-level destinations (no back button shown for these destinations)
            appBarConfiguration = AppBarConfiguration(
                setOf(
                    R.id.homeFragment,
                    R.id.discoverFragment,
                    R.id.journalFragment,
                    R.id.profileFragment
                )
            )

            // NEW: Enhanced bottom navigation with navigation throttling
            setupOptimizedBottomNavigation()

            Log.d(TAG, "Navigation initialized successfully")

        } catch (e: Exception) {
            Log.e(TAG, "Error initializing navigation: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * NEW: Setup optimized bottom navigation with proper backstack management
     * Home is the central hub - all other fragments clear backstack when navigating
     */
    private fun setupOptimizedBottomNavigation() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            // Throttle navigation to prevent rapid clicks
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastNavigationTime < NAVIGATION_THROTTLE_MS) {
                return@setOnItemSelectedListener false
            }
            lastNavigationTime = currentTime

            try {
                when (item.itemId) {
                    R.id.homeFragment -> {
                        navigateToHome()
                        true
                    }
                    R.id.discoverFragment -> {
                        navigateToDestination(R.id.discoverFragment)
                        true
                    }
                    R.id.journalFragment -> {
                        navigateToDestination(R.id.journalFragment)
                        true
                    }
                    R.id.profileFragment -> {
                        navigateToDestination(R.id.profileFragment)
                        true
                    }
                    else -> false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Navigation error: ${e.message}")
                false
            }
        }

        // Add destination change listener for proper state tracking
        navController.addOnDestinationChangedListener { _, destination, _ ->
            isHomeDestination = destination.id == R.id.homeFragment

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
    }

    /**
     * Navigate to home fragment - this is the central hub
     */
    private fun navigateToHome() {
        if (navController.currentDestination?.id != R.id.homeFragment) {
            navController.popBackStack(R.id.homeFragment, false)
        }
    }

    /**
     * Navigate to any destination with proper backstack management
     * Ensures home is always in the backstack as the base
     */
    private fun navigateToDestination(destinationId: Int) {
        if (navController.currentDestination?.id != destinationId) {
            // Clear backstack and navigate, ensuring home is the base
            navController.popBackStack(R.id.homeFragment, false)
            if (destinationId != R.id.homeFragment) {
                navController.navigate(destinationId)
            }
        }
    }

    // Handle Up navigation with NavController
    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    // Setup full screen mode by hiding the status bar AND action bar
    private fun setupFullScreen() {
        // CRITICAL FIX: Hide the action bar completely
        supportActionBar?.hide()

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

    // Initialize profile synchronization with Firestore
    private fun initializeProfileSync() {
        try {
            // Start automatic profile synchronization monitoring
            profileSyncManager.startSyncMonitoring(this)
            Log.d(TAG, "Profile sync manager initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing profile sync: ${e.message}")
            // App should continue even if sync fails
        }
    }

    /**
     * CRITICAL FIX: Initialize health tracking and request necessary permissions
     */
    private fun initializeHealthTracking() {
        try {
            // First check and request permissions if needed
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val activityRecognitionPermission = Manifest.permission.ACTIVITY_RECOGNITION
                val bodySensorsPermission = Manifest.permission.BODY_SENSORS

                val permissionsToRequest = mutableListOf<String>()

                // Check each permission individually
                if (ContextCompat.checkSelfPermission(this, activityRecognitionPermission) != PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(activityRecognitionPermission)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH &&
                    ContextCompat.checkSelfPermission(this, bodySensorsPermission) != PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(bodySensorsPermission)
                }

                // Request permissions if needed
                if (permissionsToRequest.isNotEmpty()) {
                    sensorPermissionLauncher.launch(permissionsToRequest.toTypedArray())
                } else {
                    // Permissions already granted, initialize health tracking
                    enhancedHealthTracker.initialize()
                    Log.d(TAG, "Health tracking initialized with existing permissions")
                }
            } else {
                // Older Android versions don't need runtime permissions for sensors
                enhancedHealthTracker.initialize()
                Log.d(TAG, "Health tracking initialized (pre-Android 10)")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing health tracking: ${e.message}")
        }
    }
    
    /**
     * Force generate activity cards for testing purposes
     * This can be called from a debug menu or developer settings
     */
    private fun forceGenerateActivityCards() {
        try {
            // Force generate today's card
            activityCardScheduler.forceGenerateCardForToday()
            
            // Check for any missing cards
            activityCardScheduler.checkForMissingActivityCards()
            
            Log.d(TAG, "Manually triggered activity card generation")
        } catch (e: Exception) {
            Log.e(TAG, "Error forcing activity card generation: ${e.message}")
        }
    }

    // CRITICAL FIX: Check if sensor permissions are granted
    private fun checkSensorPermissions(): Boolean {
        val bodySensorsGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.BODY_SENSORS
        ) == PackageManager.PERMISSION_GRANTED

        val activityRecognitionGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACTIVITY_RECOGNITION
        ) == PackageManager.PERMISSION_GRANTED

        Log.d(TAG, "Current permissions - Body Sensors: $bodySensorsGranted, Activity Recognition: $activityRecognitionGranted")

        // We need at least one of these permissions for step tracking
        return bodySensorsGranted || activityRecognitionGranted
    }

    // CRITICAL FIX: Request sensor permissions for step tracking
    private fun requestSensorPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        // Add BODY_SENSORS permission (required for step sensors)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS)
            != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.BODY_SENSORS)
        }

        // Add ACTIVITY_RECOGNITION permission (alternative for step tracking)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)
            != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACTIVITY_RECOGNITION)
        }

        if (permissionsToRequest.isNotEmpty()) {
            Log.d(TAG, "Requesting permissions: ${permissionsToRequest.joinToString()}")
            sensorPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    /**
     * NEW: Modern back button handling using OnBackPressedCallback
     */
    private fun setupBackButtonHandler() {
        onBackPressedDispatcher.addCallback(this) {
            handleBackNavigation()
        }
    }

    /**
     * Handle back button press with custom logic - UPDATED for modern Android
     */
    private fun handleBackNavigation() {
        when {
            // If we're on home fragment, exit the app
            isHomeDestination -> {
                finish()
            }
            // If we're on any other main fragment, go back to home
            navController.currentDestination?.id in setOf(
                R.id.discoverFragment,
                R.id.journalFragment,
                R.id.profileFragment
            ) -> {
                navigateToHome()
                binding.bottomNav.selectedItemId = R.id.homeFragment
            }
            // For other fragments (like prescriptions), use normal back navigation
            else -> {
                if (!navController.popBackStack()) {
                    // If can't pop, go to home
                    navigateToHome()
                    binding.bottomNav.selectedItemId = R.id.homeFragment
                }
            }
        }
    }

    /**
     * Handle deep links if present in the intent
     */
    private fun handleDeepLinkIfPresent() {
        try {
            if (::navController.isInitialized && intent != null) {
                val handled = discoverDeepLinkHandler.handleDeepLink(intent, navController)
                if (handled) {
                    Log.d(TAG, "Deep link handled successfully")
                } else {
                    Log.d(TAG, "No deep link to handle or not a Discover deep link")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling deep link: ${e.message}")
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Handle deep links when app is already running
        setIntent(intent)
        if (::navController.isInitialized) {
            discoverDeepLinkHandler.handleDeepLink(intent, navController)
        }
    }
}