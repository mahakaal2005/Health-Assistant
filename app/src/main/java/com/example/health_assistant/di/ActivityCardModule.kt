package com.example.health_assistant.di

import com.example.health_assistant.features.journal.data.ActivityCardRepositoryImpl
import com.example.health_assistant.features.journal.data.JournalEntryDao
import com.example.health_assistant.features.journal.domain.ActivityCardRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dependency injection module for Activity Card feature
 * Uses existing journal system to maintain compatibility
 */
@Module
@InstallIn(SingletonComponent::class)
object ActivityCardModule {

    @Provides
    @Singleton
    fun provideActivityCardRepository(
        journalDao: JournalEntryDao
    ): ActivityCardRepository {
        return ActivityCardRepositoryImpl(journalDao)
    }
}