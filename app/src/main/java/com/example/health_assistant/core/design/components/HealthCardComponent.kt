package com.example.health_assistant.core.design.components

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.example.health_assistant.R
import com.example.health_assistant.core.design.tokens.HealthColors
import com.example.health_assistant.core.design.tokens.HealthSpacing

/**
 * Unified Health Card Component
 * 
 * Standardized card layout for all health-related content across the application.
 * Replaces existing card_background_elevated, card_background_alt, and glassmorphism_card_background usage.
 * 
 * Features:
 * - Three style variants: Primary, Secondary, Elevated
 * - Consistent corner radius (12dp)
 * - Standardized elevation patterns
 * - Accessibility support with proper focus states
 * - Material 3 foundation with health-focused styling
 * 
 * Usage:
 * ```
 * val card = HealthCardComponent(context)
 * card.setCardType(HealthCardType.PRIMARY)
 * card.setContentPadding(HealthSpacing.Padding.standard)
 * ```
 */
class HealthCardComponent @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : CardView(context, attrs, defStyleAttr) {

    /**
     * Card type variants with specific styling characteristics
     */
    enum class HealthCardType(
        @ColorRes val backgroundColorRes: Int,
        @DimenRes val elevationRes: Int,
        val description: String
    ) {
        /**
         * Primary card variant - Main content cards
         * Used for: Health metrics, prescription cards, main content
         */
        PRIMARY(
            backgroundColorRes = R.color.ds_surface_primary,
            elevationRes = R.dimen.ds_elevation_low,
            description = "Primary content cards with low elevation"
        ),
        
        /**
         * Secondary card variant - Supporting content cards  
         * Used for: Article previews, secondary information, content discovery
         */
        SECONDARY(
            backgroundColorRes = R.color.ds_surface_secondary,
            elevationRes = R.dimen.ds_elevation_medium,
            description = "Secondary content cards with medium elevation"
        ),
        
        /**
         * Elevated card variant - Important or interactive cards
         * Used for: Important notifications, interactive elements, highlighted content
         */
        ELEVATED(
            backgroundColorRes = R.color.ds_surface_primary,
            elevationRes = R.dimen.ds_elevation_high,
            description = "Elevated cards for important or interactive content"
        )
    }

    private var currentCardType: HealthCardType = HealthCardType.PRIMARY

    init {
        // Initialize with default styling
        setupDefaultStyling()
        
        // Apply custom attributes if provided
        attrs?.let { attributeSet ->
            val typedArray = context.obtainStyledAttributes(
                attributeSet,
                R.styleable.HealthCardComponent,
                defStyleAttr,
                0
            )
            
            try {
                val cardTypeOrdinal = typedArray.getInt(
                    R.styleable.HealthCardComponent_healthCardType,
                    HealthCardType.PRIMARY.ordinal
                )
                // Improved bounds checking for enum values
                val cardType = if (cardTypeOrdinal in HealthCardType.values().indices) {
                    HealthCardType.values()[cardTypeOrdinal]
                } else {
                    HealthCardType.PRIMARY // Fallback to safe default
                }
                setCardType(cardType)
                
                val customPadding = typedArray.getDimensionPixelSize(
                    R.styleable.HealthCardComponent_healthCardPadding,
                    -1
                )
                if (customPadding > 0) { // Ensure positive padding values only
                    setContentPadding(customPadding, customPadding, customPadding, customPadding)
                }
            } finally {
                typedArray.recycle()
            }
        }
    }

    /**
     * Set up default styling that applies to all card variants
     */
    private fun setupDefaultStyling() {
        // Standardized corner radius (12dp)
        radius = resources.getDimension(HealthSpacing.Component.cardRadius)
        
        // Default content padding (16dp)
        val defaultPadding = resources.getDimensionPixelSize(HealthSpacing.Padding.standard)
        setContentPadding(defaultPadding, defaultPadding, defaultPadding, defaultPadding)
        
        // Enable accessibility
        isFocusable = true
        isFocusableInTouchMode = false
        
        // Set up focus state styling
        setupAccessibilitySupport()
    }

    /**
     * Set the card type and apply corresponding styling
     * @param cardType The card type to apply - must be a valid HealthCardType
     */
    fun setCardType(cardType: HealthCardType) {
        if (currentCardType != cardType) { // Only update if different to avoid unnecessary work
            currentCardType = cardType
            applyCardTypeStyling()
        }
    }

    /**
     * Get the current card type
     */
    fun getCardType(): HealthCardType = currentCardType

    /**
     * Apply styling based on the current card type
     */
    private fun applyCardTypeStyling() {
        // Set background color
        setCardBackgroundColor(ContextCompat.getColor(context, currentCardType.backgroundColorRes))
        
        // Set elevation
        cardElevation = resources.getDimension(currentCardType.elevationRes)
        
        // Update content description for accessibility
        contentDescription = "${currentCardType.description} card"
    }

    /**
     * Set up accessibility support with proper focus states and screen reader compatibility
     */
    private fun setupAccessibilitySupport() {
        // Ensure minimum touch target size (48dp)
        minimumHeight = resources.getDimensionPixelSize(HealthSpacing.Component.touchTarget)
        
        // Set up focus state styling with proper state management
        setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                // Apply focus outline by adjusting card elevation and background
                val focusElevationIncrease = resources.getDimension(R.dimen.ds_elevation_low)
                cardElevation = cardElevation + focusElevationIncrease
                alpha = 0.9f
                // Announce focus change for accessibility
                announceForAccessibility("Card focused")
            } else {
                // Reset to original styling
                applyCardTypeStyling()
                alpha = 1.0f
            }
        }
    }

    /**
     * Convenience method to set content padding using design system tokens
     */
    fun setContentPadding(@DimenRes paddingRes: Int) {
        val padding = resources.getDimensionPixelSize(paddingRes)
        setContentPadding(padding, padding, padding, padding)
    }

    /**
     * Factory methods for creating specific card types
     */
    companion object {
        /**
         * Create a primary card for main content
         */
        fun createPrimaryCard(context: Context): HealthCardComponent {
            return HealthCardComponent(context).apply {
                setCardType(HealthCardType.PRIMARY)
            }
        }

        /**
         * Create a secondary card for supporting content
         */
        fun createSecondaryCard(context: Context): HealthCardComponent {
            return HealthCardComponent(context).apply {
                setCardType(HealthCardType.SECONDARY)
            }
        }

        /**
         * Create an elevated card for important content
         */
        fun createElevatedCard(context: Context): HealthCardComponent {
            return HealthCardComponent(context).apply {
                setCardType(HealthCardType.ELEVATED)
            }
        }
    }
}