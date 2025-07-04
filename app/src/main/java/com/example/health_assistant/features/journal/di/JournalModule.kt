package com.example.health_assistant.features.journal.di

import com.example.health_assistant.features.journal.data.JournalEntryDao
import com.example.health_assistant.features.journal.data.JournalRepositoryImpl
import com.example.health_assistant.features.journal.domain.JournalRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dependency injection module for journal feature
 */
@Module
@InstallIn(SingletonComponent::class)
object JournalModule {

    @Provides
    @Singleton
    fun provideJournalRepository(
        dao: JournalEntryDao
    ): JournalRepository {
        return JournalRepositoryImpl(dao)
    }
}