package com.example.health_assistant.core.design.tokens

import androidx.annotation.StyleRes
import com.example.health_assistant.R

/**
 * Health Design System Typography Tokens
 * 
 * Provides consistent typography hierarchy using existing TextAppearance.Health styles
 * with standardized text sizes, weights, and spacing for optimal readability.
 * 
 * Usage:
 * - Headlines: HealthTypography.Headline
 * - Titles: HealthTypography.Title
 * - Body text: HealthTypography.Body
 * - Captions: HealthTypography.Caption
 */
object HealthTypography {
    
    /**
     * Headline typography tokens - Large display text
     */
    object Headline {
        @StyleRes val large = R.style.TextAppearance_Health_Headline1      // 32sp, medium weight
        @StyleRes val medium = R.style.TextAppearance_Health_Headline2     // 24sp, medium weight
        @StyleRes val small = R.style.TextAppearance_HealthAssistant_Title // 18sp, medium weight
    }
    
    /**
     * Title typography tokens - Section and card titles
     */
    object Title {
        @StyleRes val large = R.style.TextAppearance_HealthAssistant_SectionTitle  // 18sp, bold
        @StyleRes val medium = R.style.TextAppearance_HealthAssistant_CardTitle    // 16sp, bold
        @StyleRes val small = R.style.TextAppearance_HealthAssistant_Subtitle      // 14sp, medium
    }
    
    /**
     * Body typography tokens - Main content text
     */
    object Body {
        @StyleRes val large = R.style.TextAppearance_Health_Body1          // 16sp, regular
        @StyleRes val medium = R.style.TextAppearance_Health_Body2         // 14sp, regular
        @StyleRes val small = R.style.TextAppearance_HealthAssistant_Body  // 14sp, regular
    }
    
    /**
     * Caption typography tokens - Small descriptive text
     */
    object Caption {
        @StyleRes val default = R.style.TextAppearance_HealthAssistant_Caption    // 12sp, regular
        @StyleRes val tip = R.style.TextAppearance_HealthAssistant_TipText        // 14sp, secondary color
    }
    
    /**
     * Special typography tokens - Unique text treatments
     */
    object Special {
        @StyleRes val greeting = R.style.TextAppearance_HealthAssistant_Greeting  // 28sp, bold
        @StyleRes val accent = R.style.TextAppearance_Health_Accent               // 16sp, health green
        @StyleRes val date = R.style.TextAppearance_HealthAssistant_Date          // 20sp, white text
    }
    
    /**
     * Legacy typography mappings for backward compatibility
     * These maintain existing style references while providing semantic meaning
     */
    object Legacy {
        @StyleRes val healthHeadline1 = R.style.TextAppearance_Health_Headline1
        @StyleRes val healthHeadline2 = R.style.TextAppearance_Health_Headline2
        @StyleRes val healthAccent = R.style.TextAppearance_Health_Accent
        @StyleRes val healthBody1 = R.style.TextAppearance_Health_Body1
        @StyleRes val healthBody2 = R.style.TextAppearance_Health_Body2
        @StyleRes val healthAssistantTitle = R.style.TextAppearance_HealthAssistant_Title
        @StyleRes val healthAssistantSubtitle = R.style.TextAppearance_HealthAssistant_Subtitle
        @StyleRes val healthAssistantBody = R.style.TextAppearance_HealthAssistant_Body
        @StyleRes val healthAssistantCaption = R.style.TextAppearance_HealthAssistant_Caption
    }
}