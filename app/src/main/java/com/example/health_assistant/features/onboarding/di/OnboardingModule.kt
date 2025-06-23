package com.example.health_assistant.features.onboarding.di

import com.example.health_assistant.features.onboarding.OnboardingPreferencesRepository
import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing onboarding-related dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object OnboardingModule {

    @Provides
    @Singleton
    fun provideOnboardingPreferencesRepository(
        @ApplicationContext context: Context
    ): OnboardingPreferencesRepository {
        return OnboardingPreferencesRepository(context)
    }
}