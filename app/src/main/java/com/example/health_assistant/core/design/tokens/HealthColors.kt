package com.example.health_assistant.core.design.tokens

import androidx.annotation.ColorRes
import com.example.health_assistant.R

/**
 * Health Design System Color Tokens
 * 
 * Provides semantic color tokens that consolidate existing health-themed colors
 * into a consistent, maintainable system. All colors support both light and dark themes.
 * 
 * Usage:
 * - Primary actions: HealthColors.Primary
 * - Secondary elements: HealthColors.Secondary  
 * - Surface backgrounds: HealthColors.Surface
 * - Text hierarchy: HealthColors.Text
 */
object HealthColors {
    
    /**
     * Primary color tokens - Main health green theme
     */
    object Primary {
        @ColorRes val default = R.color.health_primary        // #4CAF50
        @ColorRes val variant = R.color.colorPrimaryVariant   // #388E3C  
        @ColorRes val container = R.color.primary_container   // Light container
        @ColorRes val onPrimary = R.color.colorOnPrimary      // White text on primary
    }
    
    /**
     * Secondary color tokens - Accent green colors
     */
    object Secondary {
        @ColorRes val default = R.color.health_accent         // #81C784
        @ColorRes val variant = R.color.colorSecondaryVariant // #66BB6A
        @ColorRes val container = R.color.secondary_container // Light container
        @ColorRes val onSecondary = R.color.colorOnSecondary  // Text on secondary
    }
    
    /**
     * Surface color tokens - Background and card surfaces
     */
    object Surface {
        @ColorRes val primary = R.color.surface_primary       // Main surface
        @ColorRes val elevated = R.color.surface_elevated     // Elevated cards
        @ColorRes val health = R.color.surface_health         // Health-themed surface
        @ColorRes val variant = R.color.surface_variant       // Alternative surface
        @ColorRes val onSurface = R.color.colorOnSurface      // Text on surface
    }
    
    /**
     * Background color tokens
     */
    object Background {
        @ColorRes val primary = R.color.background_primary    // Main background
        @ColorRes val secondary = R.color.background_secondary // Secondary background
        @ColorRes val onBackground = R.color.colorOnBackground // Text on background
    }
    
    /**
     * Text color tokens - Hierarchical text colors
     */
    object Text {
        @ColorRes val primary = R.color.text_primary          // Primary text
        @ColorRes val secondary = R.color.text_secondary      // Secondary text
        @ColorRes val tertiary = R.color.textTertiary         // Tertiary text
        @ColorRes val disabled = R.color.textDisabled         // Disabled text
        @ColorRes val onGreen = R.color.text_on_green         // White text on green
    }
    
    /**
     * Card color tokens - Standardized card backgrounds
     */
    object Card {
        @ColorRes val background = R.color.cardBackground     // Standard card
        @ColorRes val elevated = R.color.card_background_elevated // Elevated card
        @ColorRes val alternative = R.color.card_background_alt   // Alternative card
        @ColorRes val translucent = R.color.card_background_translucent // Glass effect
    }
    
    /**
     * Semantic color tokens - Status and feedback colors
     */
    object Semantic {
        @ColorRes val success = R.color.success               // Success state
        @ColorRes val warning = R.color.warning               // Warning state  
        @ColorRes val error = R.color.error                   // Error state
        @ColorRes val onError = R.color.colorOnError          // Text on error
    }
    
    /**
     * Health status color tokens - Health-specific status indicators
     */
    object HealthStatus {
        @ColorRes val excellent = R.color.healthExcellent     // Excellent health
        @ColorRes val good = R.color.healthGood               // Good health
        @ColorRes val warning = R.color.healthWarning         // Health warning
        @ColorRes val poor = R.color.healthPoor               // Poor health
    }
    
    /**
     * Interactive color tokens - Buttons, inputs, and interactive elements
     */
    object Interactive {
        @ColorRes val ripple = R.color.ripple_color           // Ripple effect
        @ColorRes val rippleLight = R.color.rippleColorLight  // Light ripple
        @ColorRes val outline = R.color.outline_variant       // Outline color
        @ColorRes val divider = R.color.divider               // Divider lines
    }
    
    /**
     * Legacy color mappings for backward compatibility
     * These maintain existing color references while providing semantic meaning
     */
    object Legacy {
        @ColorRes val healthPrimary = R.color.health_primary
        @ColorRes val healthLight = R.color.health_light
        @ColorRes val healthAccent = R.color.health_accent
        @ColorRes val healthDark = R.color.health_dark
        @ColorRes val primaryColor = R.color.primary_color
        @ColorRes val accentColor = R.color.accent_color
    }
}