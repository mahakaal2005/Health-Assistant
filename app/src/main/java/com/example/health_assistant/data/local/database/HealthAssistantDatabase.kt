package com.example.health_assistant.data.local.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.example.health_assistant.data.local.dao.DiseaseCategoryDao
import com.example.health_assistant.data.local.dao.PrescriptionDao
import com.example.health_assistant.data.local.dao.ProfileImageDao
import com.example.health_assistant.data.local.entity.ProfileImageEntity
import com.example.health_assistant.data.local.entity.PrescriptionEntity
import com.example.health_assistant.data.model.DiseaseCategoryEntity
import com.example.health_assistant.features.journal.data.JournalEntryDao
import com.example.health_assistant.features.journal.data.JournalEntryEntity

/**
 * Main database for the Health Assistant application
 * Integrates all entities: prescriptions, profile images, disease categories, and journal entries
 */
@Database(
    entities = [
        PrescriptionEntity::class,
        DiseaseCategoryEntity::class,
        JournalEntryEntity::class,
        ProfileImageEntity::class
    ],
    version = 3, // Increment to include journal schema fixes
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class HealthAssistantDatabase : RoomDatabase() {

    abstract fun prescriptionDao(): PrescriptionDao
    abstract fun diseaseCategoryDao(): DiseaseCategoryDao
    abstract fun journalEntryDao(): JournalEntryDao
    abstract fun profileImageDao(): ProfileImageDao

    companion object {
        const val DATABASE_NAME = "health_assistant_database"

        @Volatile
        private var INSTANCE: HealthAssistantDatabase? = null

        fun getDatabase(context: Context): HealthAssistantDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HealthAssistantDatabase::class.java,
                    DATABASE_NAME
                )
                .fallbackToDestructiveMigration() // Allow schema recreation on conflicts
                .fallbackToDestructiveMigrationOnDowngrade() // Handle version downgrades
                .build()
                INSTANCE = instance
                instance
            }
        }

        /**
         * For testing purposes - creates an in-memory database
         */
        fun getInMemoryDatabase(context: Context): HealthAssistantDatabase {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                HealthAssistantDatabase::class.java
            ).build()
        }

        /**
         * Force close and reset database instance
         * Call this to clear cached schemas and force recreation
         */
        fun resetDatabase() {
            synchronized(this) {
                INSTANCE?.close()
                INSTANCE = null
            }
        }
    }
}