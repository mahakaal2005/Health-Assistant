package com.example.health_assistant.features.journal


/**
 * Filter type enumeration for journal entries
 */
enum class JournalFilterType {
    ALL,
    NOTES,
    HEALTH,
    ACTIVITY,
    MOOD;
    
    fun getEntryTypes(): List<String> {
        return when (this) {
            ALL -> listOf("note", "mood", "health", "activity", "workout", "measurement", "heart_rate", "blood_pressure", "weight")
            NOTES -> listOf("note")
            HEALTH -> listOf("health", "heart_rate", "blood_pressure", "weight", "measurement")
            ACTIVITY -> listOf("activity", "workout")
            MOOD -> listOf("mood")
        }
    }
}

/**
 * Filter chip data class
 */
data class FilterChip(
    val type: JournalFilterType,
    val label: String,
    val isSelected: Boolean = false
)