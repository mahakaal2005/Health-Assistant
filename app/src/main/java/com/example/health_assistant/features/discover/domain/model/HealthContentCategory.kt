package com.example.health_assistant.features.discover.domain.model

/**
 * Enum representing different health content categories
 * Used for filtering and organizing content in the Discover section
 */
enum class HealthContentCategory(
    val key: String,
    val displayName: String,
    val description: String
) {
    GENERAL_HEALTH("general-health", "General Health", "Overall health and wellness topics"),
    NUTRITION("nutrition", "Nutrition", "Diet, nutrition, and healthy eating"),
    FITNESS("fitness", "Fitness", "Exercise, physical activity, and fitness"),
    MENTAL_HEALTH("mental-health", "Mental Health", "Mental wellness, stress management, and psychology"),
    PREVENTIVE_CARE("preventive-care", "Preventive Care", "Disease prevention and health screening"),
    CHRONIC_CONDITIONS("chronic-conditions", "Chronic Conditions", "Managing chronic diseases and conditions"),
    WOMEN_HEALTH("women-health", "Women's Health", "Health topics specific to women"),
    MEN_HEALTH("men-health", "Men's Health", "Health topics specific to men"),
    CHILD_HEALTH("child-health", "Child Health", "Pediatric health and child development"),
    SENIOR_HEALTH("senior-health", "Senior Health", "Health topics for older adults"),
    ALTERNATIVE_MEDICINE("alternative-medicine", "Alternative Medicine", "Complementary and alternative treatments"),
    RESEARCH("research", "Research", "Latest health research and medical studies");

    companion object {
        /**
         * Get category by key
         */
        fun fromKey(key: String): HealthContentCategory? {
            return values().find { it.key == key }
        }

        /**
         * Get all category keys
         */
        fun getAllKeys(): List<String> {
            return values().map { it.key }
        }

        /**
         * Get primary categories for main navigation
         */
        fun getPrimaryCategories(): List<HealthContentCategory> {
            return listOf(
                GENERAL_HEALTH,
                NUTRITION,
                FITNESS,
                MENTAL_HEALTH,
                PREVENTIVE_CARE,
                CHRONIC_CONDITIONS
            )
        }
    }
}