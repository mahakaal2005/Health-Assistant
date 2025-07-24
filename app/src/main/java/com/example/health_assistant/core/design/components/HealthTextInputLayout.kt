package com.example.health_assistant.core.design.components

import android.content.Context
import android.util.AttributeSet
import androidx.core.content.ContextCompat
import com.example.health_assistant.R
import com.example.health_assistant.core.design.tokens.HealthColors
import com.example.health_assistant.core.design.tokens.HealthTypography
import com.google.android.material.textfield.TextInputLayout

/**
 * Health Text Input Layout Component
 * 
 * Standardized TextInputLayout styling and validation patterns across all forms.
 * Replaces inconsistent form styling in auth, profile, and journal screens.
 * 
 * Features:
 * - Consistent styling using HealthColors and HealthTypography tokens
 * - Standardized validation error styling
 * - Accessibility support with proper content descriptions
 * - Material 3 foundation with health-focused styling
 * 
 * Usage:
 * ```
 * <com.example.health_assistant.core.design.components.HealthTextInputLayout
 *     android:layout_width="match_parent"
 *     android:layout_height="wrap_content"
 *     app:healthInputStyle="primary"
 *     android:hint="Enter text">
 *     
 *     <com.google.android.material.textfield.TextInputEditText
 *         android:layout_width="match_parent"
 *         android:layout_height="wrap_content" />
 *         
 * </com.example.health_assistant.core.design.components.HealthTextInputLayout>
 * ```
 */
class HealthTextInputLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = com.google.android.material.R.attr.textInputStyle
) : TextInputLayout(context, attrs, defStyleAttr) {

    /**
     * Input style variants for different use cases
     */
    enum class HealthInputStyle(
        val description: String
    ) {
        /**
         * Primary input style - Main form inputs
         */
        PRIMARY("Primary form input with standard styling"),
        
        /**
         * Secondary input style - Supporting form inputs
         */
        SECONDARY("Secondary form input with subtle styling"),
        
        /**
         * Search input style - Search and filter inputs
         */
        SEARCH("Search input with search-specific styling")
    }

    private var currentInputStyle: HealthInputStyle = HealthInputStyle.PRIMARY

    init {
        // Apply default styling
        setupDefaultStyling()
        
        // Apply custom attributes if provided
        attrs?.let { attributeSet ->
            val typedArray = context.obtainStyledAttributes(
                attributeSet,
                R.styleable.HealthTextInputLayout,
                defStyleAttr,
                0
            )
            
            try {
                val inputStyleOrdinal = typedArray.getInt(
                    R.styleable.HealthTextInputLayout_healthInputStyle,
                    HealthInputStyle.PRIMARY.ordinal
                )
                val inputStyle = if (inputStyleOrdinal in HealthInputStyle.values().indices) {
                    HealthInputStyle.values()[inputStyleOrdinal]
                } else {
                    HealthInputStyle.PRIMARY
                }
                setInputStyle(inputStyle)
            } finally {
                typedArray.recycle()
            }
        }
    }

    /**
     * Set up default styling that applies to all input variants
     */
    private fun setupDefaultStyling() {
        // Set corner radius for consistent appearance
        val cornerRadius = resources.getDimension(R.dimen.ds_component_card_radius_small)
        setBoxCornerRadii(cornerRadius, cornerRadius, cornerRadius, cornerRadius)
        
        // Set box background mode to outline for consistent appearance
        boxBackgroundMode = BOX_BACKGROUND_OUTLINE
        
        // Apply consistent typography for hints and labels
        setHintTextAppearance(HealthTypography.Body.medium)
        setHelperTextTextAppearance(HealthTypography.Caption.default)
        setErrorTextAppearance(HealthTypography.Caption.default)
        
        // Set minimum height for accessibility
        minimumHeight = resources.getDimensionPixelSize(R.dimen.ds_component_input_height)
        
        // Enable accessibility
        setupAccessibilitySupport()
    }

    /**
     * Set the input style and apply corresponding styling
     */
    fun setInputStyle(inputStyle: HealthInputStyle) {
        if (currentInputStyle != inputStyle) {
            currentInputStyle = inputStyle
            applyInputStyling()
        }
    }

    /**
     * Get the current input style
     */
    fun getInputStyle(): HealthInputStyle = currentInputStyle

    /**
     * Apply styling based on the current input style
     */
    private fun applyInputStyling() {
        when (currentInputStyle) {
            HealthInputStyle.PRIMARY -> {
                // Primary input styling
                boxStrokeColor = ContextCompat.getColor(context, HealthColors.Primary.default)
                setBoxStrokeColorStateList(ContextCompat.getColorStateList(context, R.color.health_input_stroke_primary))
                hintTextColor = ContextCompat.getColorStateList(context, HealthColors.Text.secondary)
                setBoxBackgroundColorStateList(ContextCompat.getColorStateList(context, HealthColors.Surface.primary))
            }
            HealthInputStyle.SECONDARY -> {
                // Secondary input styling
                boxStrokeColor = ContextCompat.getColor(context, HealthColors.Secondary.default)
                setBoxStrokeColorStateList(ContextCompat.getColorStateList(context, R.color.health_input_stroke_secondary))
                hintTextColor = ContextCompat.getColorStateList(context, HealthColors.Text.tertiary)
                setBoxBackgroundColorStateList(ContextCompat.getColorStateList(context, HealthColors.Surface.variant))
            }
            HealthInputStyle.SEARCH -> {
                // Search input styling
                boxStrokeColor = ContextCompat.getColor(context, HealthColors.Interactive.outline)
                setBoxStrokeColorStateList(ContextCompat.getColorStateList(context, R.color.health_input_stroke_search))
                hintTextColor = ContextCompat.getColorStateList(context, HealthColors.Text.tertiary)
                setBoxBackgroundColorStateList(ContextCompat.getColorStateList(context, HealthColors.Surface.elevated))
                
                // Add search-specific styling
                startIconDrawable = ContextCompat.getDrawable(context, R.drawable.ic_search)
                setStartIconTintList(ContextCompat.getColorStateList(context, HealthColors.Text.tertiary))
            }
        }
        
        // Update content description for accessibility
        contentDescription = "${currentInputStyle.description} input field"
    }

    /**
     * Set up accessibility support
     */
    private fun setupAccessibilitySupport() {
        // Ensure proper focus handling
        isFocusable = true
        isFocusableInTouchMode = false // Let the EditText handle touch focus
        
        // Set up error announcement for screen readers
        setErrorIconOnClickListener {
            error?.let { errorText ->
                announceForAccessibility("Error: $errorText")
            }
        }
    }

    /**
     * Enhanced error handling with consistent styling
     */
    override fun setError(errorText: CharSequence?) {
        super.setError(errorText)
        
        if (errorText != null) {
            applyErrorStyling()
            announceErrorForAccessibility(errorText.toString())
        } else {
            clearErrorStyling()
        }
    }

    /**
     * Apply error styling consistently
     */
    private fun applyErrorStyling() {
        boxStrokeColor = ContextCompat.getColor(context, HealthColors.Semantic.error)
        setErrorTextColor(ContextCompat.getColorStateList(context, HealthColors.Semantic.error))
        setErrorIconTintList(ContextCompat.getColorStateList(context, HealthColors.Semantic.error))
    }

    /**
     * Clear error styling and reset to normal state
     */
    private fun clearErrorStyling() {
        applyInputStyling()
    }

    /**
     * Announce error for accessibility with proper formatting
     */
    private fun announceErrorForAccessibility(errorText: String) {
        announceForAccessibility("Error: $errorText")
    }

    /**
     * Convenience method to set helper text with consistent styling
     */
    fun setHelperTextWithStyle(helperText: CharSequence?) {
        helperText?.let {
            this.helperText = it
            setHelperTextColor(ContextCompat.getColorStateList(context, HealthColors.Text.tertiary))
        }
    }

    /**
     * Factory methods for creating specific input types
     */
    companion object {
        /**
         * Create a primary input for main form fields
         */
        fun createPrimaryInput(context: Context): HealthTextInputLayout {
            return HealthTextInputLayout(context).apply {
                setInputStyle(HealthInputStyle.PRIMARY)
            }
        }

        /**
         * Create a secondary input for supporting form fields
         */
        fun createSecondaryInput(context: Context): HealthTextInputLayout {
            return HealthTextInputLayout(context).apply {
                setInputStyle(HealthInputStyle.SECONDARY)
            }
        }

        /**
         * Create a search input for search and filter functionality
         */
        fun createSearchInput(context: Context): HealthTextInputLayout {
            return HealthTextInputLayout(context).apply {
                setInputStyle(HealthInputStyle.SEARCH)
            }
        }
    }
}