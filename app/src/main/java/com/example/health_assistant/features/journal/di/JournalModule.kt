package com.example.health_assistant.features.journal.di

import com.example.health_assistant.auth.session.SessionManager
import com.example.health_assistant.features.journal.data.JournalEntryDao
import com.example.health_assistant.features.journal.data.JournalRepositoryImpl
import com.example.health_assistant.features.journal.domain.JournalRepository
import com.example.health_assistant.features.journal.domain.JournalUseCases
import com.example.health_assistant.features.journal.domain.AddEntryUseCase
import com.example.health_assistant.features.journal.domain.DeleteEntryUseCase
import com.example.health_assistant.features.journal.domain.GetAllEntriesUseCase
import com.example.health_assistant.features.journal.domain.GetEntriesByDateRangeUseCase
import com.example.health_assistant.features.journal.domain.GetEntriesByTypeUseCase
import com.example.health_assistant.features.journal.domain.GetEntryByIdUseCase
import com.example.health_assistant.features.journal.domain.GetRecentEntriesUseCase
import com.example.health_assistant.features.journal.domain.UpdateEntryUseCase
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
        dao: JournalEntryDao,
        sessionManager: SessionManager
    ): JournalRepository {
        return JournalRepositoryImpl(dao, sessionManager)
    }

    @Provides
    @Singleton
    fun provideJournalUseCases(repository: JournalRepository): JournalUseCases {
        return JournalUseCases(
            getAllEntries = GetAllEntriesUseCase(repository),
            getEntriesByType = GetEntriesByTypeUseCase(repository),
            getRecentEntries = GetRecentEntriesUseCase(repository),
            addEntry = AddEntryUseCase(repository),
            updateEntry = UpdateEntryUseCase(repository),
            deleteEntry = DeleteEntryUseCase(repository),
            getEntryById = GetEntryByIdUseCase(repository),
            getEntriesByDateRange = GetEntriesByDateRangeUseCase(repository)
        )
    }
}