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
 * Provides repository implementation using existing journal infrastructure
 */
@Module
@InstallIn(SingletonComponent::class)
object ActivityCardModule {

    @Provides
    @Singleton
    fun provideActivityCardRepository(
        journalEntryDao: JournalEntryDao
    ): ActivityCardRepository {
        return ActivityCardRepositoryImpl(journalEntryDao)
    }
}