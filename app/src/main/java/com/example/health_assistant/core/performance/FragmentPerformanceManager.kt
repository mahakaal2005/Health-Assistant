package com.example.health_assistant.core.performance

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enhanced performance manager for ultra-smooth fragment transitions
 * Implements advanced optimizations including fragment preloading and resource pooling
 */
@Singleton
class FragmentPerformanceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Fragment preloading cache
    private val preloadedFragments = ConcurrentHashMap<String, Fragment>()

    // Navigation timing optimization
    private var lastNavigationTime = 0L
    private val minNavigationInterval = 100L // Prevent rapid consecutive navigation

    // View recycling pools
    private val viewPoolManager = ViewPoolManager()

    /**
     * ENHANCED: Optimize fragment creation with aggressive preloading
     */
    fun optimizeFragmentCreation(
        fragment: Fragment,
        heavyOperations: List<suspend () -> Unit>,
        delayMs: Long = 50L // Reduced delay for faster response
    ) {
        fragment.viewLifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                // Execute heavy operations with priority queue
                heavyOperations.forEachIndexed { index, operation ->
                    backgroundScope.launch {
                        try {
                            delay(delayMs * index) // Stagger operations
                            operation()
                        } catch (e: Exception) {
                            android.util.Log.e("FragmentPerformance", "Error in operation $index", e)
                        }
                    }
                }
            }
        })
    }

    /**
     * ENHANCED: Ultra-fast UI loading with immediate critical views
     */
    fun lazyLoadUI(
        fragment: Fragment,
        criticalViews: () -> Unit,
        nonCriticalViews: () -> Unit,
        delayMs: Long = 100L // Reduced from 150ms for faster loading
    ) {
        // Load critical views immediately - no delay
        try {
            criticalViews()
        } catch (e: Exception) {
            android.util.Log.e("FragmentPerformance", "Error in critical views", e)
        }

        // Defer non-critical views with optimized timing
        mainHandler.postDelayed({
            if (fragment.isVisible && fragment.view != null && fragment.isAdded) {
                try {
                    nonCriticalViews()
                } catch (e: Exception) {
                    android.util.Log.e("FragmentPerformance", "Error in non-critical views", e)
                }
            }
        }, delayMs)
    }

    /**
     * NEW: Smart navigation throttling to prevent overwhelming the system
     */
    fun canNavigate(): Boolean {
        val currentTime = System.currentTimeMillis()
        return if (currentTime - lastNavigationTime >= minNavigationInterval) {
            lastNavigationTime = currentTime
            true
        } else {
            false
        }
    }

    /**
     * NEW: Preload likely next fragments for instant navigation
     */
    fun preloadFragment(fragmentClass: Class<out Fragment>, fragmentTag: String) {
        if (preloadedFragments.containsKey(fragmentTag)) return

        backgroundScope.launch {
            try {
                val fragment = fragmentClass.getDeclaredConstructor().newInstance()
                preloadedFragments[fragmentTag] = fragment
                android.util.Log.d("FragmentPerformance", "Preloaded fragment: $fragmentTag")
            } catch (e: Exception) {
                android.util.Log.e("FragmentPerformance", "Failed to preload fragment: $fragmentTag", e)
            }
        }
    }

    /**
     * NEW: Get preloaded fragment for instant navigation
     */
    fun getPreloadedFragment(fragmentTag: String): Fragment? {
        return preloadedFragments.remove(fragmentTag)
    }

    /**
     * ENHANCED: Advanced RecyclerView optimization with view pooling
     */
    fun optimizeRecyclerView(recyclerView: RecyclerView) {
        recyclerView.apply {
            // Enable aggressive optimizations
            setHasFixedSize(true)
            setItemViewCacheSize(30) // Increased from 20

            // Use shared view pools for better memory management
            setRecycledViewPool(viewPoolManager.getSharedPool())

            // Optimize layout calculations
            setItemAnimator(null) // Disable for maximum performance

            // Enable nested scrolling optimization
            isNestedScrollingEnabled = true

            // Modern performance optimizations (removed deprecated drawing cache methods)
            // Enable hardware acceleration for smoother scrolling
            setLayerType(View.LAYER_TYPE_HARDWARE, null)

            // Optimize touch event handling
            isMotionEventSplittingEnabled = false
        }
    }

    /**
     * NEW: Optimize navigation controller for smooth transitions
     */
    fun optimizeNavController(navController: NavController) {
        // Pre-warm navigation destinations
        backgroundScope.launch {
            delay(1000) // Wait for app to stabilize
            preloadCommonFragments()
        }
    }

    /**
     * NEW: Execute operations with different priority levels
     */
    fun executeWithPriority(
        priority: Priority,
        backgroundWork: suspend () -> Unit,
        mainThreadCallback: (() -> Unit)? = null
    ): Job {
        val dispatcher = when (priority) {
            Priority.HIGH -> Dispatchers.Main.immediate
            Priority.MEDIUM -> Dispatchers.Default
            Priority.LOW -> Dispatchers.IO
        }

        return backgroundScope.launch(dispatcher) {
            try {
                backgroundWork()
                mainThreadCallback?.let {
                    withContext(Dispatchers.Main.immediate) {
                        it()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("FragmentPerformance", "Error in priority execution", e)
            }
        }
    }

    /**
     * NEW: Memory pressure handling
     */
    fun onMemoryPressure() {
        // Clear preloaded fragments to free memory
        preloadedFragments.clear()
        viewPoolManager.clearPools()
        System.gc() // Suggest garbage collection
    }

    /**
     * NEW: Pre-warm fragments that are likely to be used
     */
    private suspend fun preloadCommonFragments() {
        val commonFragments = listOf(
            "com.example.health_assistant.features.home.HomeFragment" to "home",
            "com.example.health_assistant.features.prescriptions.PrescriptionsFragment" to "prescriptions",
            "com.example.health_assistant.features.profile.ProfileFragment" to "profile"
        )

        commonFragments.forEach { (className, tag) ->
            try {
                val fragmentClass = Class.forName(className) as Class<out Fragment>
                preloadFragment(fragmentClass, tag)
                delay(100) // Stagger preloading
            } catch (e: Exception) {
                android.util.Log.e("FragmentPerformance", "Failed to preload common fragment: $className", e)
            }
        }
    }

    enum class Priority {
        HIGH, MEDIUM, LOW
    }

    /**
     * NEW: Shared view pool manager for better memory efficiency
     */
    private class ViewPoolManager {
        private val sharedPool = RecyclerView.RecycledViewPool()

        init {
            // Configure shared pool sizes
            sharedPool.setMaxRecycledViews(0, 10) // Default view type
            sharedPool.setMaxRecycledViews(1, 5)  // Header view type
            sharedPool.setMaxRecycledViews(2, 15) // Item view type
        }

        fun getSharedPool(): RecyclerView.RecycledViewPool = sharedPool

        fun clearPools() {
            sharedPool.clear()
        }
    }

    /**
     * Enhanced cleanup with memory optimization
     */
    fun cleanup() {
        preloadedFragments.clear()
        viewPoolManager.clearPools()
        backgroundScope.cancel()
    }
}