# Health Assistant Design System Documentation

## Overview

The Health Assistant Design System provides a comprehensive set of design tokens, components, and guidelines to ensure visual consistency across the entire application. Built on Material 3 foundation with health-focused branding.

## Design System Structure

```
app/src/main/java/com/example/health_assistant/core/design/
├── HealthDesignSystem.kt          # Main design system entry point
├── tokens/
│   ├── HealthColors.kt           # Color semantic tokens
│   ├── HealthTypography.kt       # Typography hierarchy tokens
│   └── HealthSpacing.kt          # Spacing and sizing tokens
└── components/                   # Reusable component implementations
```

## Color System

### Usage Guidelines

The color system provides semantic tokens that automatically adapt to light and dark themes:

```kotlin
// Kotlin usage
import com.example.health_assistant.core.design.tokens.HealthColors

// Primary colors for main actions
HealthColors.Primary.default        // Main health green (#4CAF50)
HealthColors.Primary.variant        // Darker green for emphasis
HealthColors.Primary.container      // Light container background

// Secondary colors for accents
HealthColors.Secondary.default      // Accent green (#81C784)
HealthColors.Secondary.variant      // Medium green variant

// Surface colors for backgrounds
HealthColors.Surface.primary        // Main surface background
HealthColors.Surface.elevated       // Elevated card surfaces
HealthColors.Surface.health         // Health-themed surface

// Text colors with hierarchy
HealthColors.Text.primary           // Primary text (high contrast)
HealthColors.Text.secondary         // Secondary text (medium contrast)
HealthColors.Text.tertiary          // Tertiary text (low contrast)
```

### XML Resource Usage

```xml
<!-- Direct color token usage -->
<TextView
    android:textColor="@color/ds_text_primary"
    android:background="@color/ds_surface_primary" />

<!-- Card with health theme -->
<androidx.cardview.widget.CardView
    android:background="@color/ds_card_background"
    app:cardElevation="@dimen/ds_elevation_medium" />
```

### Color Categories

- **Primary**: Main health green theme (#4CAF50) for primary actions
- **Secondary**: Accent green colors (#81C784) for secondary elements
- **Surface**: Background colors for cards and surfaces
- **Text**: Hierarchical text colors (primary, secondary, tertiary)
- **Semantic**: Status colors (success, warning, error)
- **Health Status**: Health-specific indicators (excellent, good, warning, poor)

## Typography System

### Usage Guidelines

Typography tokens provide consistent text hierarchy across the application:

```kotlin
// Kotlin usage
import com.example.health_assistant.core.design.tokens.HealthTypography

// Headlines for major sections
HealthTypography.Headline.large     // 28sp, bold - Main page titles
HealthTypography.Headline.medium    // 24sp, medium - Section headers
HealthTypography.Headline.small     // 20sp, medium - Subsection headers

// Titles for cards and components
HealthTypography.Title.large        // 18sp, bold - Card titles
HealthTypography.Title.medium       // 16sp, bold - Component titles
HealthTypography.Title.small        // 14sp, bold - Small titles

// Body text for content
HealthTypography.Body.large         // 16sp, regular - Main content
HealthTypography.Body.medium        // 14sp, regular - Secondary content
HealthTypography.Body.small         // 12sp, regular - Small content
```

### XML Style Usage

```xml
<!-- Headline text -->
<TextView
    android:textAppearance="@style/TextAppearance.HealthDS.Headline.Large"
    android:text="Health Dashboard" />

<!-- Card title -->
<TextView
    android:textAppearance="@style/TextAppearance.HealthDS.Title.Medium"
    android:text="Today's Activity" />

<!-- Body content -->
<TextView
    android:textAppearance="@style/TextAppearance.HealthDS.Body.Large"
    android:text="Your health metrics for today..." />
```

### Typography Hierarchy

- **Display**: 36sp, 32sp - Hero text and major displays
- **Headline**: 28sp, 24sp, 20sp - Page and section headers
- **Title**: 18sp, 16sp, 14sp - Card and component titles
- **Body**: 16sp, 14sp, 12sp - Content text with proper line spacing
- **Label**: 14sp, 12sp, 10sp - Form labels and captions

## Spacing System

### Usage Guidelines

All spacing follows an 8dp grid system for consistency:

```kotlin
// Kotlin usage
import com.example.health_assistant.core.design.tokens.HealthSpacing

// Padding tokens
HealthSpacing.Padding.standard      // 16dp - Standard component padding
HealthSpacing.Padding.large         // 20dp - Large component padding
HealthSpacing.Padding.xl            // 24dp - Extra large padding

// Margin tokens
HealthSpacing.Margin.standard       // 16dp - Standard component margins
HealthSpacing.Margin.small          // 8dp - Small spacing between elements

// Component sizing
HealthSpacing.Component.buttonHeight     // 56dp - Standard button height
HealthSpacing.Component.cardRadius       // 12dp - Card corner radius
HealthSpacing.Component.touchTarget      // 48dp - Minimum touch target
```

### XML Dimension Usage

```xml
<!-- Standard card layout -->
<androidx.cardview.widget.CardView
    android:layout_margin="@dimen/ds_margin_small"
    android:padding="@dimen/ds_padding_standard"
    app:cardCornerRadius="@dimen/ds_component_card_radius"
    app:cardElevation="@dimen/ds_elevation_medium" />

<!-- Button with proper sizing -->
<Button
    android:layout_height="@dimen/ds_component_button_height"
    android:paddingHorizontal="@dimen/ds_padding_xl"
    android:paddingVertical="@dimen/ds_padding_medium" />
```

### Spacing Categories

- **Base Units**: 8dp, 4dp, 2dp - Foundation spacing units
- **Padding**: 0dp to 32dp - Internal component spacing
- **Margins**: 0dp to 32dp - External component spacing
- **Component Sizing**: Standard dimensions for buttons, inputs, icons
- **Layout Spacing**: Screen padding, section spacing, card spacing
- **Elevation**: Shadow and depth values (0dp to 16dp)

## Component Styles

### Button Components

```xml
<!-- Primary button -->
<Button
    style="@style/Widget.HealthDS.Button.Primary"
    android:text="Save Changes" />

<!-- Secondary button -->
<Button
    style="@style/Widget.HealthDS.Button.Secondary"
    android:text="Cancel" />

<!-- Text button -->
<Button
    style="@style/Widget.HealthDS.Button.Text"
    android:text="Learn More" />
```

### Card Components

```xml
<!-- Standard card -->
<androidx.cardview.widget.CardView
    style="@style/Widget.HealthDS.CardView"
    android:layout_width="match_parent"
    android:layout_height="wrap_content">
    <!-- Card content -->
</androidx.cardview.widget.CardView>

<!-- Elevated card -->
<androidx.cardview.widget.CardView
    style="@style/Widget.HealthDS.CardView.Elevated">
    <!-- Elevated card content -->
</androidx.cardview.widget.CardView>

<!-- Health-themed card -->
<androidx.cardview.widget.CardView
    style="@style/Widget.HealthDS.CardView.Health">
    <!-- Health-specific content -->
</androidx.cardview.widget.CardView>
```

### Text Input Components

```xml
<!-- Standard text input -->
<com.google.android.material.textfield.TextInputLayout
    style="@style/Widget.HealthDS.TextInputLayout"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:hint="Enter your name">
    
    <com.google.android.material.textfield.TextInputEditText
        style="@style/Widget.HealthDS.TextInputEditText"
        android:layout_width="match_parent"
        android:layout_height="wrap_content" />
        
</com.google.android.material.textfield.TextInputLayout>
```

## Theme Integration

The design system integrates with Material 3 themes:

```xml
<!-- Base theme with design system integration -->
<style name="Base.Theme.Health_Assistant" parent="Theme.Material3.DayNight.NoActionBar">
    <!-- Design system colors automatically applied -->
    <item name="colorPrimary">@color/ds_primary</item>
    <item name="colorSecondary">@color/ds_secondary</item>
    <item name="colorSurface">@color/ds_surface_primary</item>
    
    <!-- Design system typography -->
    <item name="textAppearanceHeadline1">@style/TextAppearance.HealthDS.Headline.Large</item>
    <item name="textAppearanceBody1">@style/TextAppearance.HealthDS.Body.Large</item>
    
    <!-- Design system components -->
    <item name="materialButtonStyle">@style/Widget.HealthDS.Button.Primary</item>
    <item name="materialCardViewStyle">@style/Widget.HealthDS.CardView</item>
</style>
```

## Best Practices

### Color Usage
- Use semantic tokens instead of direct color values
- Ensure proper contrast ratios for accessibility
- Test both light and dark themes
- Use health-themed colors for health-related content

### Typography Usage
- Follow the established hierarchy (headline > title > body > caption)
- Use appropriate line spacing for readability
- Maintain consistent font weights across similar content types
- Test text scaling for accessibility

### Spacing Usage
- Follow the 8dp grid system consistently
- Use standard padding/margin tokens instead of custom values
- Ensure minimum touch targets (48dp) for interactive elements
- Maintain consistent spacing between related elements

### Component Usage
- Use design system component styles instead of custom styling
- Extend existing components rather than creating new ones
- Maintain consistent elevation and corner radius patterns
- Test component behavior across different screen sizes

## Migration Guide

### From Legacy Colors
```kotlin
// Old approach
R.color.health_primary

// New approach
HealthColors.Primary.default
```

### From Legacy Typography
```xml
<!-- Old approach -->
<TextView android:textAppearance="@style/TextAppearance.Health.Headline1" />

<!-- New approach -->
<TextView android:textAppearance="@style/TextAppearance.HealthDS.Headline.Large" />
```

### From Legacy Spacing
```xml
<!-- Old approach -->
<View android:padding="16dp" />

<!-- New approach -->
<View android:padding="@dimen/ds_padding_standard" />
```

## Accessibility Compliance

The design system ensures WCAG 2.1 AA compliance:

- **Color Contrast**: All color combinations meet minimum contrast ratios
- **Touch Targets**: Minimum 48dp touch targets for interactive elements
- **Text Scaling**: Typography scales properly with system font size settings
- **Focus Indicators**: Clear focus states for keyboard navigation
- **Screen Reader Support**: Proper semantic markup and content descriptions

## Future Enhancements

- Animation tokens for consistent motion design
- Icon system integration with standardized sizing
- Component variants for specialized use cases
- Advanced theming support for customization
- Performance optimizations for large-scale usage

## Support

For questions or issues with the design system:
1. Check this documentation for usage guidelines
2. Review existing component implementations
3. Test changes in both light and dark themes
4. Ensure accessibility compliance before implementation