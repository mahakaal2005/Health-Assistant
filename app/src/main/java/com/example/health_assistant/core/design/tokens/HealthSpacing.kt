package com.example.health_assistant.core.design.tokens

import androidx.annotation.DimenRes
import com.example.health_assistant.R

/**
 * Health Design System Spacing Tokens
 * 
 * Provides consistent spacing system with standardized dimensions for padding,
 * margins, and component sizing based on 8dp grid system.
 * 
 * Usage:
 * - Padding: HealthSpacing.Padding
 * - Margins: HealthSpacing.Margin  
 * - Component sizing: HealthSpacing.Component
 * - Layout spacing: HealthSpacing.Layout
 */
object HealthSpacing {
    
    /**
     * Base spacing unit (8dp) - All spacing should be multiples of this
     */
    object Base {
        @DimenRes val unit = R.dimen.ds_spacing_unit              // 8dp
        @DimenRes val half = R.dimen.ds_spacing_half              // 4dp
        @DimenRes val quarter = R.dimen.ds_spacing_quarter        // 2dp
    }
    
    /**
     * Padding tokens - Internal component spacing
     */
    object Padding {
        @DimenRes val none = R.dimen.ds_padding_none              // 0dp
        @DimenRes val xs = R.dimen.ds_padding_xs                  // 4dp
        @DimenRes val small = R.dimen.ds_padding_small            // 8dp
        @DimenRes val medium = R.dimen.ds_padding_medium          // 12dp
        @DimenRes val standard = R.dimen.ds_padding_standard      // 16dp
        @DimenRes val large = R.dimen.ds_padding_large            // 20dp
        @DimenRes val xl = R.dimen.ds_padding_xl                  // 24dp
        @DimenRes val xxl = R.dimen.ds_padding_xxl                // 32dp
    }
    
    /**
     * Margin tokens - External component spacing
     */
    object Margin {
        @DimenRes val none = R.dimen.ds_margin_none               // 0dp
        @DimenRes val xs = R.dimen.ds_margin_xs                   // 4dp
        @DimenRes val small = R.dimen.ds_margin_small             // 8dp
        @DimenRes val medium = R.dimen.ds_margin_medium           // 12dp
        @DimenRes val standard = R.dimen.ds_margin_standard       // 16dp
        @DimenRes val large = R.dimen.ds_margin_large             // 20dp
        @DimenRes val xl = R.dimen.ds_margin_xl                   // 24dp
        @DimenRes val xxl = R.dimen.ds_margin_xxl                 // 32dp
    }
    
    /**
     * Component sizing tokens - Standard component dimensions
     */
    object Component {
        @DimenRes val buttonHeight = R.dimen.ds_component_button_height        // 56dp
        @DimenRes val buttonHeightSmall = R.dimen.ds_component_button_height_small // 40dp
        @DimenRes val inputHeight = R.dimen.ds_component_input_height          // 56dp
        @DimenRes val cardRadius = R.dimen.ds_component_card_radius            // 12dp
        @DimenRes val cardRadiusSmall = R.dimen.ds_component_card_radius_small // 8dp
        @DimenRes val iconSize = R.dimen.ds_component_icon_size                // 24dp
        @DimenRes val iconSizeSmall = R.dimen.ds_component_icon_size_small     // 16dp
        @DimenRes val iconSizeLarge = R.dimen.ds_component_icon_size_large     // 32dp
        @DimenRes val touchTarget = R.dimen.ds_component_touch_target          // 48dp
    }
    
    /**
     * Layout spacing tokens - Screen and section spacing
     */
    object Layout {
        @DimenRes val screenPadding = R.dimen.ds_layout_screen_padding         // 16dp
        @DimenRes val sectionSpacing = R.dimen.ds_layout_section_spacing       // 24dp
        @DimenRes val cardSpacing = R.dimen.ds_layout_card_spacing             // 12dp
        @DimenRes val listItemSpacing = R.dimen.ds_layout_list_item_spacing    // 8dp
        @DimenRes val dividerHeight = R.dimen.ds_layout_divider_height         // 1dp
    }
    
    /**
     * Elevation tokens - Shadow and depth
     */
    object Elevation {
        @DimenRes val none = R.dimen.ds_elevation_none                         // 0dp
        @DimenRes val low = R.dimen.ds_elevation_low                           // 2dp
        @DimenRes val medium = R.dimen.ds_elevation_medium                     // 4dp
        @DimenRes val high = R.dimen.ds_elevation_high                         // 8dp
        @DimenRes val highest = R.dimen.ds_elevation_highest                   // 16dp
    }
    
    /**
     * Legacy spacing mappings for backward compatibility
     * These maintain existing dimension references while providing semantic meaning
     */
    object Legacy {
        // Common spacing values that exist in the current system
        @DimenRes val standardPadding = R.dimen.ds_padding_standard    // 16dp
        @DimenRes val largePadding = R.dimen.ds_padding_xl             // 24dp
        @DimenRes val smallPadding = R.dimen.ds_padding_small          // 8dp
        @DimenRes val cardCornerRadius = R.dimen.ds_component_card_radius // 12dp
        @DimenRes val buttonMinHeight = R.dimen.ds_component_button_height // 56dp
    }
}