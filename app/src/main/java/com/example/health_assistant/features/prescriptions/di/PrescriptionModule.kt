package com.example.health_assistant.features.prescriptions.di

import com.example.health_assistant.auth.session.SessionManager
import com.example.health_assistant.data.local.dao.PrescriptionDao
import com.example.health_assistant.features.prescriptions.data.PrescriptionRepositoryImpl
import com.example.health_assistant.features.prescriptions.domain.PrescriptionRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dependency injection module for prescription feature
 */
@Module
@InstallIn(SingletonComponent::class)
object PrescriptionModule {

    @Provides
    @Singleton
    fun providePrescriptionRepository(
        dao: PrescriptionDao,
        sessionManager: SessionManager
    ): PrescriptionRepository {
        return PrescriptionRepositoryImpl(dao, sessionManager)
    }
}