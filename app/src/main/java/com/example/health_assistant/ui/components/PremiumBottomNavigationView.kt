package com.example.health_assistant.ui.components

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.example.health_assistant.R
import com.google.android.material.floatingactionbutton.FloatingActionButton

/**
 * Custom 5-Tab Bottom Navigation with integrated center FAB
 * Layout: [Home] [Discover] [FAB] [Journal] [Profile]
 * Features light green background like discover cards
 */
class PremiumBottomNavigationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    companion object {
        private const val TAG = "PremiumBottomNav"
    }

    private lateinit var customBottomNavContainer: LinearLayout
    private lateinit var centerFab: FloatingActionButton
    private var pillHighlight: PillHighlightView? = null
    private var currentSelectedItemId = -1
    private var performanceMonitor: PerformanceMonitor? = null
    private var animationController: BottomNavAnimationController? = null
    private var accessibilityHelper: AccessibilityNavigationHelper? = null
    private var config: PremiumBottomNavConfig? = null
    
    // Icon resource mappings for filled/outlined states
    private val iconMappings = mapOf(
        R.id.homeFragment to Pair(R.drawable.ic_home_filled, R.drawable.ic_home_outlined),
        R.id.discoverFragment to Pair(R.drawable.ic_browse_filled, R.drawable.ic_browse_outlined),
        R.id.journalFragment to Pair(R.drawable.ic_diary_filled, R.drawable.ic_diary_outlined),
        R.id.profileFragment to Pair(R.drawable.ic_profile_filled, R.drawable.ic_profile_outlined)
    )

    init {
        Log.d(TAG, "Initializing Custom 5-Tab Bottom Navigation")
        
        try {
            // Create custom bottom navigation container
            createCustomBottomNavigation()
            
            // Initialize optional components safely
            initializeOptionalComponents()
            
            Log.i(TAG, "Custom 5-Tab Bottom Navigation initialization complete")
            
        } catch (e: Exception) {
            Log.e(TAG, "Critical initialization failure", e)
            createFallbackView(context, attrs, defStyleAttr)
        }
    }
    
    private fun createCustomBottomNavigation() {
        // Create main container with light green background
        customBottomNavContainer = LinearLayout(context).apply {
            layoutParams = LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            
            // Apply subtle background with better contrast
            val roundedBackground = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = 24.dpToPx().toFloat() // More rounded for modern look
                setColor(ContextCompat.getColor(context, R.color.white))
                setStroke(1.dpToPx(), ContextCompat.getColor(context, R.color.health_light))
            }
            background = roundedBackground
            elevation = 8f
            
            // Add padding for better spacing with 5 elements
            setPadding(12.dpToPx(), 16.dpToPx(), 12.dpToPx(), 16.dpToPx())
        }
        
        // Create navigation tabs with proper spacing
        createNavigationTabs()
        
        // Add container to main view
        addView(customBottomNavContainer)
        
        // Add pill highlight overlay
        createPillHighlight()
        
        Log.d(TAG, "Custom bottom navigation container created")
    }    

    private fun createNavigationTabs() {
        // Left group (Home + Discover) - closer together
        val leftGroup = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                2f // Weight for left side
            )
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        
        // Add Home tab
        leftGroup.addView(createTabView(R.id.homeFragment, R.drawable.ic_home_filled, R.drawable.ic_home_outlined, "Home"))
        // Add Discover tab
        leftGroup.addView(createTabView(R.id.discoverFragment, R.drawable.ic_browse_filled, R.drawable.ic_browse_outlined, "Discover"))
        
        customBottomNavContainer.addView(leftGroup)
        
        // Center FAB with proper spacing
        createCenterFab()
        
        // Right group (Journal + Profile) - closer together
        val rightGroup = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                2f // Weight for right side
            )
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        
        // Add Journal tab
        rightGroup.addView(createTabView(R.id.journalFragment, R.drawable.ic_diary_filled, R.drawable.ic_diary_outlined, "Journal"))
        // Add Profile tab
        rightGroup.addView(createTabView(R.id.profileFragment, R.drawable.ic_profile_filled, R.drawable.ic_profile_outlined, "Profile"))
        
        customBottomNavContainer.addView(rightGroup)
    }
    
    private fun createTabView(id: Int, filledIcon: Int, outlinedIcon: Int, title: String): LinearLayout {
        return LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(8.dpToPx(), 8.dpToPx(), 8.dpToPx(), 8.dpToPx())
            
            // Make clickable
            isClickable = true
            isFocusable = true
            
            // Add ripple effect
            val rippleDrawable = android.graphics.drawable.RippleDrawable(
                ContextCompat.getColorStateList(context, R.color.rippleColorLight) ?: ColorStateList.valueOf(ContextCompat.getColor(context, R.color.rippleColorLight)),
                null,
                null
            )
            background = rippleDrawable
            
            // Create icon
            val iconView = android.widget.ImageView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    24.dpToPx(),
                    24.dpToPx()
                )
                setImageResource(outlinedIcon) // Start with outlined
                imageTintList = ContextCompat.getColorStateList(context, R.color.text_tertiary_dark)
                scaleType = android.widget.ImageView.ScaleType.CENTER_INSIDE
            }
            
            // Create label
            val labelView = android.widget.TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 4.dpToPx()
                }
                text = title
                textSize = 12f
                setTextColor(ContextCompat.getColor(context, R.color.text_tertiary_dark))
                gravity = Gravity.CENTER
            }
            
            addView(iconView)
            addView(labelView)
            
            // Set click listener
            setOnClickListener {
                selectTab(id, this, iconView, labelView, filledIcon, outlinedIcon)
            }
            
            // Store references for later use
            tag = id
        }
    }
    
    private fun createCenterFab() {
        centerFab = FloatingActionButton(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                56.dpToPx(),
                56.dpToPx()
            ).apply {
                setMargins(24.dpToPx(), 0, 24.dpToPx(), 0)
            }
            
            // Style the FAB
            backgroundTintList = ContextCompat.getColorStateList(context, R.color.health_primary)
            setImageResource(R.drawable.ic_ai_chatbot)
            imageTintList = ContextCompat.getColorStateList(context, R.color.white)
            elevation = 6f
            compatElevation = 6f
            
            contentDescription = "Open AI Health Assistant"
            
            // Add click listener
            setOnClickListener {
                // Handle FAB click
                Log.d(TAG, "Center FAB clicked")
                onFabClickListener?.invoke()
            }
        }
        
        customBottomNavContainer.addView(centerFab)
    }
    
    private fun createPillHighlight() {
        try {
            pillHighlight = PillHighlightView(context).apply {
                layoutParams = LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                // Position behind the tabs but above the background
                elevation = 2f
            }
            
            // Add pill highlight as an overlay
            addView(pillHighlight, 0) // Add at index 0 so it's behind the navigation container
            
            Log.d(TAG, "Pill highlight overlay created")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to create pill highlight overlay", e)
        }
    }
    
    private fun selectTab(id: Int, tabView: LinearLayout, iconView: android.widget.ImageView, 
                         labelView: android.widget.TextView, filledIcon: Int, outlinedIcon: Int) {
        // Update current selection
        currentSelectedItemId = id
        
        // Reset all tabs to unselected state
        resetAllTabs()
        
        // Update selected tab appearance
        iconView.imageTintList = ContextCompat.getColorStateList(context, R.color.health_primary)
        labelView.setTextColor(ContextCompat.getColor(context, R.color.health_primary))
        iconView.setImageResource(filledIcon)
        
        // Update pill highlight position
        updatePillHighlightForTab(id)
        
        // Trigger navigation callback
        onTabSelectedListener?.invoke(id)
        
        Log.d(TAG, "Tab selected: $id")
    }
    
    private fun updatePillHighlightForTab(tabId: Int) {
        pillHighlight?.let { pill ->
            val tabIndex = when (tabId) {
                R.id.homeFragment -> 0
                R.id.discoverFragment -> 1
                R.id.journalFragment -> 2
                R.id.profileFragment -> 3
                else -> return
            }
            
            // Update pill position with animation
            pill.updatePillPosition(tabIndex, animate = true)
            Log.d(TAG, "Pill highlight updated for tab index: $tabIndex")
        }
    }
    
    private fun resetAllTabs() {
        resetTabGroup(customBottomNavContainer.getChildAt(0) as LinearLayout) // Left group
        resetTabGroup(customBottomNavContainer.getChildAt(2) as LinearLayout) // Right group (index 2 because FAB is at index 1)
    }
    
    private fun resetTabGroup(group: LinearLayout) {
        for (i in 0 until group.childCount) {
            val tabView = group.getChildAt(i) as LinearLayout
            val iconView = tabView.getChildAt(0) as android.widget.ImageView
            val labelView = tabView.getChildAt(1) as android.widget.TextView
            
            // Reset to unselected state
            iconView.imageTintList = ContextCompat.getColorStateList(context, R.color.text_tertiary_dark)
            labelView.setTextColor(ContextCompat.getColor(context, R.color.text_tertiary_dark))
            
            // Update icon to outlined version
            val outlinedIcon = when (tabView.tag as Int) {
                R.id.homeFragment -> R.drawable.ic_home_outlined
                R.id.discoverFragment -> R.drawable.ic_browse_outlined
                R.id.journalFragment -> R.drawable.ic_diary_outlined
                R.id.profileFragment -> R.drawable.ic_profile_outlined
                else -> continue
            }
            iconView.setImageResource(outlinedIcon)
        }
    }
    
    // Navigation callbacks
    private var onTabSelectedListener: ((Int) -> Unit)? = null
    private var onFabClickListener: (() -> Unit)? = null
    
    fun setOnTabSelectedListener(listener: (Int) -> Unit) {
        onTabSelectedListener = listener
    }
    
    fun setOnFabClickListener(listener: () -> Unit) {
        onFabClickListener = listener
    }
    
    // Compatibility method for MainActivity
    fun setOnItemSelectedListener(listener: (android.view.MenuItem) -> Boolean): Boolean {
        setOnTabSelectedListener { itemId ->
            // Create a mock MenuItem for compatibility
            val mockMenuItem = object : android.view.MenuItem {
                override fun getItemId(): Int = itemId
                override fun getGroupId(): Int = 0
                override fun getOrder(): Int = 0
                override fun setTitle(title: CharSequence?): android.view.MenuItem = this
                override fun setTitle(title: Int): android.view.MenuItem = this
                override fun getTitle(): CharSequence = ""
                override fun setTitleCondensed(title: CharSequence?): android.view.MenuItem = this
                override fun getTitleCondensed(): CharSequence = ""
                override fun setIcon(icon: android.graphics.drawable.Drawable?): android.view.MenuItem = this
                override fun setIcon(iconRes: Int): android.view.MenuItem = this
                override fun getIcon(): android.graphics.drawable.Drawable? = null
                override fun setIntent(intent: android.content.Intent?): android.view.MenuItem = this
                override fun getIntent(): android.content.Intent? = null
                override fun setShortcut(numericChar: Char, alphaChar: Char): android.view.MenuItem = this
                override fun setNumericShortcut(numericChar: Char): android.view.MenuItem = this
                override fun getNumericShortcut(): Char = 0.toChar()
                override fun setAlphabeticShortcut(alphaChar: Char): android.view.MenuItem = this
                override fun getAlphabeticShortcut(): Char = 0.toChar()
                override fun setCheckable(checkable: Boolean): android.view.MenuItem = this
                override fun isCheckable(): Boolean = false
                override fun setChecked(checked: Boolean): android.view.MenuItem = this
                override fun isChecked(): Boolean = false
                override fun setVisible(visible: Boolean): android.view.MenuItem = this
                override fun isVisible(): Boolean = true
                override fun setEnabled(enabled: Boolean): android.view.MenuItem = this
                override fun isEnabled(): Boolean = true
                override fun hasSubMenu(): Boolean = false
                override fun getSubMenu(): android.view.SubMenu? = null
                override fun setOnMenuItemClickListener(menuItemClickListener: android.view.MenuItem.OnMenuItemClickListener?): android.view.MenuItem = this
                override fun getMenuInfo(): android.view.ContextMenu.ContextMenuInfo? = null
                override fun setShowAsAction(actionEnum: Int) {}
                override fun setShowAsActionFlags(actionEnum: Int): android.view.MenuItem = this
                override fun setActionView(view: android.view.View?): android.view.MenuItem = this
                override fun setActionView(resId: Int): android.view.MenuItem = this
                override fun getActionView(): android.view.View? = null
                override fun setActionProvider(actionProvider: android.view.ActionProvider?): android.view.MenuItem = this
                override fun getActionProvider(): android.view.ActionProvider? = null
                override fun expandActionView(): Boolean = false
                override fun collapseActionView(): Boolean = false
                override fun isActionViewExpanded(): Boolean = false
                override fun setOnActionExpandListener(listener: android.view.MenuItem.OnActionExpandListener?): android.view.MenuItem = this
                override fun setContentDescription(contentDescription: CharSequence?): android.view.MenuItem = this
                override fun getContentDescription(): CharSequence? = null
                override fun setTooltipText(tooltipText: CharSequence?): android.view.MenuItem = this
                override fun getTooltipText(): CharSequence? = null
                override fun setIconTintList(tint: android.content.res.ColorStateList?): android.view.MenuItem = this
                override fun getIconTintList(): android.content.res.ColorStateList? = null
                override fun setIconTintMode(tintMode: android.graphics.PorterDuff.Mode?): android.view.MenuItem = this
                override fun getIconTintMode(): android.graphics.PorterDuff.Mode? = null
                override fun setNumericShortcut(numericChar: Char, numericModifiers: Int): android.view.MenuItem = this
                override fun getNumericModifiers(): Int = 0
                override fun setAlphabeticShortcut(alphaChar: Char, alphaModifiers: Int): android.view.MenuItem = this
                override fun getAlphabeticModifiers(): Int = 0
            }
            
            listener(mockMenuItem)
        }
        return true
    }
    
    // Extension function for dp to px conversion
    private fun Int.dpToPx(): Int {
        return (this * context.resources.displayMetrics.density).toInt()
    }
    
    // Compatibility methods for MainActivity integration
    fun setupWithNavController(navController: androidx.navigation.NavController) {
        // Set up navigation listener for custom tabs
        setOnTabSelectedListener { itemId ->
            try {
                navController.navigate(itemId)
                Log.d(TAG, "Navigated to fragment: $itemId")
            } catch (e: Exception) {
                Log.w(TAG, "Navigation failed for item: $itemId", e)
            }
        }
        
        // Set initial selection to home
        post {
            selectInitialTab()
        }
        
        Log.d(TAG, "Custom navigation setup with NavController complete")
    }
    
    private fun selectInitialTab() {
        // Find and select the home tab by default
        val leftGroup = customBottomNavContainer.getChildAt(0) as LinearLayout
        val homeTab = leftGroup.getChildAt(0) as LinearLayout
        val iconView = homeTab.getChildAt(0) as android.widget.ImageView
        val labelView = homeTab.getChildAt(1) as android.widget.TextView
        selectTab(R.id.homeFragment, homeTab, iconView, labelView, R.drawable.ic_home_filled, R.drawable.ic_home_outlined)
    }
    
    fun setSelectedItemId(itemId: Int) {
        // Find and select the specified tab
        findAndSelectTab(itemId)
    }
    
    private fun findAndSelectTab(itemId: Int) {
        // Check left group
        val leftGroup = customBottomNavContainer.getChildAt(0) as LinearLayout
        for (i in 0 until leftGroup.childCount) {
            val tabView = leftGroup.getChildAt(i) as LinearLayout
            if (tabView.tag == itemId) {
                val iconView = tabView.getChildAt(0) as android.widget.ImageView
                val labelView = tabView.getChildAt(1) as android.widget.TextView
                val filledIcon = iconMappings[itemId]?.first ?: return
                selectTab(itemId, tabView, iconView, labelView, filledIcon, iconMappings[itemId]?.second ?: return)
                return
            }
        }
        
        // Check right group
        val rightGroup = customBottomNavContainer.getChildAt(2) as LinearLayout
        for (i in 0 until rightGroup.childCount) {
            val tabView = rightGroup.getChildAt(i) as LinearLayout
            if (tabView.tag == itemId) {
                val iconView = tabView.getChildAt(0) as android.widget.ImageView
                val labelView = tabView.getChildAt(1) as android.widget.TextView
                val filledIcon = iconMappings[itemId]?.first ?: return
                selectTab(itemId, tabView, iconView, labelView, filledIcon, iconMappings[itemId]?.second ?: return)
                return
            }
        }
    }
    
    fun getSelectedItemId(): Int = currentSelectedItemId
    
    fun getIntegratedFab(): FloatingActionButton? {
        return centerFab
    }
    
    // Compatibility method for accessibility helper
    fun getBottomNavigationView(): com.google.android.material.bottomnavigation.BottomNavigationView? {
        // This custom view doesn't have a standard BottomNavigationView
        // Return null and let the calling code handle it gracefully
        Log.w(TAG, "getBottomNavigationView() called on custom navigation view - returning null")
        return null
    }
    
    private fun initializeOptionalComponents() {
        try {
            // Initialize configuration system
            config = PremiumBottomNavConfig.getInstance(context)
            Log.d(TAG, "Configuration system initialized")
        } catch (e: Exception) {
            Log.w(TAG, "Configuration system failed to initialize", e)
        }
        
        try {
            // Initialize performance monitoring
            performanceMonitor = PerformanceMonitor()
            Log.d(TAG, "Performance monitor initialized")
        } catch (e: Exception) {
            Log.w(TAG, "Performance monitor failed to initialize", e)
        }
        
        try {
            // Initialize accessibility features
            accessibilityHelper = AccessibilityNavigationHelper(context, this)
            Log.d(TAG, "Accessibility helper initialized")
        } catch (e: Exception) {
            Log.w(TAG, "Accessibility helper failed to initialize", e)
        }
    }
    
    private fun createFallbackView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        Log.w(TAG, "Creating fallback view")
        
        // Create a simple container as fallback
        val fallbackContainer = LinearLayout(context).apply {
            layoutParams = LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setBackgroundColor(ContextCompat.getColor(context, R.color.health_light))
            setPadding(16.dpToPx(), 12.dpToPx(), 16.dpToPx(), 12.dpToPx())
        }
        
        addView(fallbackContainer)
        customBottomNavContainer = fallbackContainer
        
        Log.d(TAG, "Fallback view created")
    }
    
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        performanceMonitor?.stopMonitoring()
        animationController?.cleanup()
        accessibilityHelper?.cleanup()
        
        Log.d(TAG, "PremiumBottomNavigationView detached from window")
    }
}