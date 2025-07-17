package com.example.health_assistant.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "disease_categories")
data class DiseaseCategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String?,
    val iconResId: Int?,
    val isActive: Boolean = true
)

// Domain model
data class DiseaseCategory(
    val id: Long = 0,
    val name: String,
    val description: String?,
    val iconResId: Int?,
    val isActive: Boolean = true
) {
    companion object {
        fun getDefaultCategories(): List<DiseaseCategory> {
            return listOf(
                DiseaseCategory(
                    id = 1L,
                    name = "Cardiology",
                    description = "Heart and cardiovascular conditions",
                    iconResId = null,
                    isActive = true
                ),
                DiseaseCategory(
                    id = 2L,
                    name = "Endocrinology",
                    description = "Diabetes and hormonal disorders",
                    iconResId = null,
                    isActive = true
                ),
                DiseaseCategory(
                    id = 3L,
                    name = "General",
                    description = "General medical conditions",
                    iconResId = null,
                    isActive = true
                ),
                DiseaseCategory(
                    id = 4L,
                    name = "Neurology",
                    description = "Brain and nervous system conditions",
                    iconResId = null,
                    isActive = true
                ),
                DiseaseCategory(
                    id = 5L,
                    name = "Orthopedics",
                    description = "Bone and joint conditions",
                    iconResId = null,
                    isActive = true
                ),
                DiseaseCategory(
                    id = 6L,
                    name = "Other",
                    description = "Other medical conditions and custom categories",
                    iconResId = null,
                    isActive = true
                )
            )
        }
    }
}

// Extension functions for mapping
fun DiseaseCategoryEntity.toDiseaseCategory(): DiseaseCategory {
    return DiseaseCategory(
        id = id,
        name = name,
        description = description,
        iconResId = iconResId,
        isActive = isActive
    )
}

fun DiseaseCategory.toDiseaseCategoryEntity(): DiseaseCategoryEntity {
    return DiseaseCategoryEntity(
        id = id,
        name = name,
        description = description,
        iconResId = iconResId,
        isActive = isActive
    )
}