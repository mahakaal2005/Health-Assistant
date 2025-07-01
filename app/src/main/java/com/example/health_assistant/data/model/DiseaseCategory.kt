package com.example.health_assistant.data.model

import com.example.health_assistant.data.local.entity.DiseaseCategoryEntity

/**
 * Domain model for disease categories
 */
data class DiseaseCategory(
    val id: String,
    val displayName: String,
    val description: String? = null,
    val iconRes: Int? = null
) {
    companion object {
        /**
         * Get the default disease categories
         */
        fun getDefaultCategories(): List<DiseaseCategory> {
            return listOf(
                DiseaseCategory("general", "General"),
                DiseaseCategory("cardiology", "Cardiology"),
                DiseaseCategory("dermatology", "Dermatology"),
                DiseaseCategory("endocrinology", "Endocrinology"),
                DiseaseCategory("gastroenterology", "Gastroenterology"),
                DiseaseCategory("neurology", "Neurology"),
                DiseaseCategory("orthopedics", "Orthopedics"),
                DiseaseCategory("pediatrics", "Pediatrics"),
                DiseaseCategory("psychiatry", "Psychiatry"),
                DiseaseCategory("radiology", "Radiology")
            )
        }
    }

    /**
     * Convert to database entity
     */
    fun toEntity(): DiseaseCategoryEntity {
        return DiseaseCategoryEntity(
            id = id,
            displayName = displayName,
            description = description,
            iconRes = iconRes
        )
    }
}

/**
 * Extension function to convert entity back to domain model
 */
fun DiseaseCategoryEntity.toDiseaseCategory(): DiseaseCategory {
    return DiseaseCategory(
        id = id,
        displayName = displayName,
        description = description,
        iconRes = iconRes
    )
}