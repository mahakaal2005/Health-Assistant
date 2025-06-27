package com.example.health_assistant.di

import com.example.health_assistant.data.repository.impl.FirebaseAuthRepositoryImpl
import com.example.health_assistant.data.repository.impl.HealthRepositoryImpl
import com.example.health_assistant.data.repository.impl.UserProfileRepositoryImpl
import com.example.health_assistant.data.repository.interfaces.AuthRepository
import com.example.health_assistant.data.repository.interfaces.HealthRepository
import com.example.health_assistant.data.repository.interfaces.UserProfileRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing repository implementations
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(firebaseAuthRepositoryImpl: FirebaseAuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindHealthRepository(healthRepositoryImpl: HealthRepositoryImpl): HealthRepository

    @Binds
    @Singleton
    abstract fun bindUserProfileRepository(userProfileRepositoryImpl: UserProfileRepositoryImpl): UserProfileRepository
}