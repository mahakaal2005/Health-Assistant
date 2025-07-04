package com.example.health_assistant.features.profile.data

import com.example.health_assistant.data.local.entity.ProfileImageEntity
import com.example.health_assistant.features.profile.domain.ProfileImage

/**
 * Extension functions to convert between ProfileImageEntity and ProfileImage domain model
 */

fun ProfileImageEntity.toDomain(): ProfileImage {
    return ProfileImage(
        id = id,
        imagePath = imagePath,
        fileName = fileName,
        fileSize = fileSize,
        mimeType = mimeType,
        dateCreated = dateCreated,
        dateModified = dateModified,
        isActive = isActive,
        width = width,
        height = height,
        description = description,
        compressionQuality = compressionQuality
    )
}

fun ProfileImage.toEntity(): ProfileImageEntity {
    return ProfileImageEntity(
        id = if (id == 0L) 0 else id, // Let Room auto-generate if 0
        imagePath = imagePath,
        fileName = fileName,
        fileSize = fileSize,
        mimeType = mimeType,
        dateCreated = dateCreated,
        dateModified = dateModified,
        isActive = isActive,
        width = width,
        height = height,
        description = description,
        compressionQuality = compressionQuality
    )
}

/**
 * Utility functions for list conversions
 */
fun List<ProfileImageEntity>.toDomainList(): List<ProfileImage> {
    return map { it.toDomain() }
}

fun List<ProfileImage>.toEntityList(): List<ProfileImageEntity> {
    return map { it.toEntity() }
}