package com.example.health_assistant.di

import com.example.health_assistant.data.repository.impl.FirebaseAuthRepository
import com.example.health_assistant.data.repository.impl.HealthRepositoryImpl
import com.example.health_assistant.data.repository.impl.RoomPrescriptionRepositoryImpl
import com.example.health_assistant.data.repository.impl.UserProfileRepositoryImpl
import com.example.health_assistant.data.repository.interfaces.AuthRepository
import com.example.health_assistant.data.repository.interfaces.HealthRepository
import com.example.health_assistant.data.repository.interfaces.PrescriptionRepository
import com.example.health_assistant.data.repository.interfaces.UserProfileRepository
import com.example.health_assistant.features.discover.data.SimpleDiscoverRepositoryImpl
import com.example.health_assistant.features.discover.domain.repository.DiscoverRepository
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
    abstract fun bindAuthRepository(firebaseAuthRepository: FirebaseAuthRepository): AuthRepository

    @Binds
    @Singleton
    abstract fun bindHealthRepository(healthRepositoryImpl: HealthRepositoryImpl): HealthRepository

    @Binds
    @Singleton
    abstract fun bindUserProfileRepository(userProfileRepositoryImpl: UserProfileRepositoryImpl): UserProfileRepository

    @Binds
    @Singleton
    abstract fun bindPrescriptionRepository(
        roomPrescriptionRepositoryImpl: RoomPrescriptionRepositoryImpl
    ): PrescriptionRepository

    @Binds
    @Singleton
    abstract fun bindDiscoverRepository(
        simpleDiscoverRepositoryImpl: SimpleDiscoverRepositoryImpl
    ): DiscoverRepository
}