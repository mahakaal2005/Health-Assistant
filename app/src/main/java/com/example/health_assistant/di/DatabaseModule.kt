package com.example.health_assistant.di

import android.content.Context
import androidx.room.Room
import com.example.health_assistant.data.local.dao.DiseaseCategoryDao
import com.example.health_assistant.data.local.dao.ProfileImageDao
import com.example.health_assistant.data.local.dao.PrescriptionDao
import com.example.health_assistant.data.local.database.HealthAssistantDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing Room database dependencies
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
            "health_assistant_database"
        )
        .addMigrations(HealthAssistantDatabase.MIGRATION_1_2)
        .build()
    }

    @Provides
    fun provideProfileImageDao(database: HealthAssistantDatabase): ProfileImageDao {
        return database.profileImageDao()
    }

    @Provides
    fun providePrescriptionDao(database: HealthAssistantDatabase): PrescriptionDao {
        return database.prescriptionDao()
    }

    @Provides
    fun provideDiseaseCategoryDao(database: HealthAssistantDatabase): DiseaseCategoryDao {
        return database.diseaseCategoryDao()
    }
}