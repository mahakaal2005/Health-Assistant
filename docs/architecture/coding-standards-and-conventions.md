# Coding Standards and Conventions

## Existing Standards Compliance

**Code Style:** Kotlin coding conventions with existing project patterns, Material 3 component usage, MVVM architecture adherence

**Linting Rules:** Continue existing Detekt configuration (version 1.23.6), maintain current code quality standards

**Testing Patterns:** JUnit 5 with MockK for unit tests, Espresso for UI tests, existing test structure under `src/test/` and `src/androidTest/`

**Documentation Style:** KDoc for Kotlin classes, inline comments for complex UI logic, README updates for design system usage

## Enhancement-Specific Standards

- **Design System Naming**: All design system components prefixed with "Health" (HealthCard, HealthButton, etc.)
- **Resource Naming**: Design system resources use "health_" prefix for consistency (health_card_style, health_button_primary)
- **Component Documentation**: Each design system component includes usage examples and integration guidelines

## Critical Integration Rules

- **Existing API Compatibility**: UI changes must not affect existing API call patterns or data handling logic
- **Database Integration**: No modifications to Room entities or Firebase document structures
- **Error Handling**: Maintain existing error handling patterns, extend for design system component errors
- **Logging Consistency**: Use existing logging patterns, add design system component usage tracking if needed