package com.example.health_assistant.data.model

import androidx.annotation.DrawableRes
import com.example.health_assistant.R

/**
 * Data model for disease categories used in prescription organization
 */
data class DiseaseCategory(
    val id: String,
    val name: String,
    val displayName: String,
    @DrawableRes val iconRes: Int?,
    val isCustom: Boolean = false
) {
    companion object {
        /**
         * Default disease categories available in the app
         */
        fun getDefaultCategories(): List<DiseaseCategory> {
            return listOf(
                DiseaseCategory(
                    id = "cardiology",
                    name = "CARDIOLOGY",
                    displayName = "Cardiology",
                    iconRes = R.drawable.ic_medical_category
                ),
                DiseaseCategory(
                    id = "diabetes",
                    name = "DIABETES",
                    displayName = "Diabetes",
                    iconRes = R.drawable.ic_medical_category
                ),
                DiseaseCategory(
                    id = "respiratory",
                    name = "RESPIRATORY",
                    displayName = "Respiratory",
                    iconRes = R.drawable.ic_medical_category
                ),
                DiseaseCategory(
                    id = "orthopedic",
                    name = "ORTHOPEDIC",
                    displayName = "Orthopedic",
                    iconRes = R.drawable.ic_medical_category
                ),
                DiseaseCategory(
                    id = "dermatology",
                    name = "DERMATOLOGY",
                    displayName = "Dermatology",
                    iconRes = R.drawable.ic_medical_category
                ),
                DiseaseCategory(
                    id = "neurology",
                    name = "NEUROLOGY",
                    displayName = "Neurology",
                    iconRes = R.drawable.ic_medical_category
                ),
                DiseaseCategory(
                    id = "general",
                    name = "GENERAL",
                    displayName = "General Medicine",
                    iconRes = R.drawable.ic_medical_category
                ),
                DiseaseCategory(
                    id = "other",
                    name = "OTHER",
                    displayName = "Other",
                    iconRes = R.drawable.ic_medical_category
                )
            )
        }

        /**
         * Find category by name
         */
        fun findByName(name: String): DiseaseCategory? {
            return getDefaultCategories().find { it.name == name }
        }

        /**
         * Find category by ID
         */
        fun findById(id: String): DiseaseCategory? {
            return getDefaultCategories().find { it.id == id }
        }
    }
}