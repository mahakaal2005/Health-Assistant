package com.example.health_assistant.di

import android.content.Context
import androidx.room.Room
import com.example.health_assistant.data.local.dao.DiseaseCategoryDao
import com.example.health_assistant.data.local.dao.PrescriptionDao
import com.example.health_assistant.data.local.dao.ProfileImageDao
import com.example.health_assistant.data.local.database.HealthAssistantDatabase
import com.example.health_assistant.features.discover.data.DiscoverDao
import com.example.health_assistant.features.discover.data.AnalyticsDao
import com.example.health_assistant.features.journal.data.JournalEntryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dependency injection module for database components
 * Provides Room database and all DAO instances
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideHealthAssistantDatabase(
        @ApplicationContext context: Context
    ): HealthAssistantDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            HealthAssistantDatabase::class.java,
            HealthAssistantDatabase.DATABASE_NAME
        )
        .fallbackToDestructiveMigration() // For development - remove in production
        .build()
    }

    @Provides
    fun providePrescriptionDao(database: HealthAssistantDatabase): PrescriptionDao {
        return database.prescriptionDao()
    }

    @Provides
    fun provideDiseaseCategoryDao(database: HealthAssistantDatabase): DiseaseCategoryDao {
        return database.diseaseCategoryDao()
    }

    @Provides
    fun provideJournalEntryDao(database: HealthAssistantDatabase): JournalEntryDao {
        return database.journalEntryDao()
    }

    @Provides
    fun provideProfileImageDao(database: HealthAssistantDatabase): ProfileImageDao {
        return database.profileImageDao()
    }

    @Provides
    fun provideDiscoverDao(database: HealthAssistantDatabase): DiscoverDao {
        return database.discoverDao()
    }

    @Provides
    fun provideAnalyticsDao(database: HealthAssistantDatabase): AnalyticsDao {
        return database.analyticsDao()
    }
}