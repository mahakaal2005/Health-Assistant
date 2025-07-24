# Source Tree

**Existing Project Structure:**
```
app/src/main/
├── java/com/example/health_assistant/
│   ├── auth/                    # Authentication flows
│   ├── core/                    # Core utilities and performance
│   ├── data/                    # Data layer (Room, Firebase, repositories)
│   ├── di/                      # Hilt dependency injection modules
│   ├── features/                # Feature modules (home, journal, prescriptions, etc.)
│   ├── main/                    # MainActivity
│   ├── utils/                   # Utility classes
│   └── widgets/                 # Custom UI widgets
├── res/
│   ├── drawable/                # Extensive drawable resources
│   ├── layout/                  # Fragment and activity layouts
│   ├── values/                  # Colors, themes, styles, strings
│   └── values-night/            # Dark theme resources
└── AndroidManifest.xml
```

**New File Organization:**
```
app/src/main/
├── java/com/example/health_assistant/
│   ├── core/
│   │   ├── design/              # New: Design system components
│   │   │   ├── HealthDesignSystem.kt
│   │   │   ├── components/
│   │   │   │   ├── HealthCardComponent.kt
│   │   │   │   ├── HealthButtonComponent.kt
│   │   │   │   └── HealthFormComponent.kt
│   │   │   └── tokens/
│   │   │       ├── HealthColors.kt
│   │   │       ├── HealthTypography.kt
│   │   │       └── HealthSpacing.kt
│   │   └── util/                # Existing utilities
│   ├── features/                # Existing feature modules
│   │   ├── home/
│   │   │   └── components/      # New: Feature-specific design system usage
│   │   ├── journal/
│   │   │   └── components/      # New: Standardized journal components
│   │   └── prescriptions/
│   │       └── components/      # New: Standardized prescription components
│   └── widgets/                 # Existing custom widgets
├── res/
│   ├── values/
│   │   ├── colors.xml           # Existing file with additions
│   │   ├── styles.xml           # Existing file with additions
│   │   ├── themes.xml           # Existing file with additions
│   │   └── design_tokens.xml    # New: Centralized design tokens
│   └── values-night/
│       ├── colors.xml           # Existing file with additions
│       └── themes.xml           # Existing file with additions
```

## Integration Guidelines

**File Naming:** Continue existing camelCase for Kotlin files, snake_case for resources, maintain Health prefix for design system components

**Folder Organization:** Add design system under core/ package to indicate foundational nature, feature-specific components under existing feature modules

**Import/Export Patterns:** Use existing package structure patterns, design system components accessible via `com.example.health_assistant.core.design` imports