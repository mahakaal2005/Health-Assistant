package com.example.health_assistant.features.profile.data

import com.example.health_assistant.data.local.dao.ProfileImageDao
import com.example.health_assistant.features.profile.domain.ProfileImage
import com.example.health_assistant.features.profile.domain.ProfileImageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of ProfileImageRepository
 * Handles data operations and converts between entity and domain models
 */
@Singleton
class ProfileImageRepositoryImpl @Inject constructor(
    private val dao: ProfileImageDao
) : ProfileImageRepository {

    override fun getAllProfileImages(): Flow<List<ProfileImage>> {
        return dao.getAllProfileImages().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getCurrentProfileImage(): Flow<ProfileImage?> {
        return dao.getCurrentProfileImageFlow().map { entity ->
            entity?.toDomain()
        }
    }

    override suspend fun getCurrentProfileImageSync(): ProfileImage? {
        return dao.getCurrentProfileImage()?.toDomain()
    }

    override suspend fun getProfileImageById(id: Long): ProfileImage? {
        return dao.getProfileImageById(id)?.toDomain()
    }

    override suspend fun insertProfileImage(profileImage: ProfileImage): Long {
        return dao.insertProfileImage(profileImage.toEntity())
    }

    override suspend fun updateProfileImage(profileImage: ProfileImage) {
        dao.updateProfileImage(profileImage.toEntity())
    }

    override suspend fun deleteProfileImage(profileImage: ProfileImage) {
        dao.deleteProfileImage(profileImage.toEntity())
    }

    override suspend fun deleteProfileImageById(id: Long) {
        dao.deleteProfileImageById(id)
    }

    override suspend fun setActiveProfileImage(id: Long) {
        // First deactivate all profile images
        dao.deactivateAllProfileImages()
        // Then activate the selected one
        dao.setActiveProfileImage(id)
    }

    override suspend fun deleteInactiveProfileImages() {
        dao.deleteInactiveProfileImages()
    }

    override suspend fun getProfileImageCount(): Int {
        return dao.getProfileImageCount()
    }
}