package com.example.health_assistant.features.profile.di

import com.example.health_assistant.data.local.dao.ProfileImageDao
import com.example.health_assistant.features.profile.data.ProfileImageRepositoryImpl
import com.example.health_assistant.features.profile.domain.ProfileImageRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dependency injection module for profile image feature
 */
@Module
@InstallIn(SingletonComponent::class)
object ProfileModule {

    @Provides
    @Singleton
    fun provideProfileImageRepository(
        dao: ProfileImageDao
    ): ProfileImageRepository {
        return ProfileImageRepositoryImpl(dao)
    }
}