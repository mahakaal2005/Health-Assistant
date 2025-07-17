package com.example.health_assistant.data.local.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import com.example.health_assistant.data.local.dao.DiseaseCategoryDao
import com.example.health_assistant.data.local.dao.PrescriptionDao
import com.example.health_assistant.data.local.dao.ProfileImageDao
import com.example.health_assistant.data.local.entity.ProfileImageEntity
import com.example.health_assistant.data.local.entity.PrescriptionEntity
import com.example.health_assistant.data.model.DiseaseCategoryEntity
import com.example.health_assistant.features.journal.data.ActivityCardDao
import com.example.health_assistant.features.journal.data.JournalEntryDao
import com.example.health_assistant.features.journal.data.JournalEntryEntity
import com.example.health_assistant.features.journal.domain.ActivityCard

/**
 * Main database for the Health Assistant application
 * Integrates all entities: prescriptions, profile images, disease categories, journal entries, and activity cards
 */
@Database(
    entities = [
        PrescriptionEntity::class,
        DiseaseCategoryEntity::class,
        JournalEntryEntity::class,
        ProfileImageEntity::class,
        ActivityCard::class
    ],
    version = 5, // Increment to include userId fields
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class HealthAssistantDatabase : RoomDatabase() {

    abstract fun prescriptionDao(): PrescriptionDao
    abstract fun diseaseCategoryDao(): DiseaseCategoryDao
    abstract fun journalEntryDao(): JournalEntryDao
    abstract fun profileImageDao(): ProfileImageDao
    abstract fun activityCardDao(): ActivityCardDao

    companion object {
        const val DATABASE_NAME = "health_assistant_database"

        @Volatile
        private var INSTANCE: HealthAssistantDatabase? = null

        /**
         * Migration from version 4 to 5:
         * - Add userId column to prescriptions table
         * - Add userId column to journal_entries table
         * - Add userId column to activity_cards table
         */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add userId column to prescriptions table
                database.execSQL("ALTER TABLE prescriptions ADD COLUMN userId TEXT NOT NULL DEFAULT ''")
                database.execSQL("CREATE INDEX index_prescriptions_userId ON prescriptions(userId)")

                // Add userId column to journal_entries table
                database.execSQL("ALTER TABLE journal_entries ADD COLUMN userId TEXT NOT NULL DEFAULT ''")
                database.execSQL("CREATE INDEX index_journal_entries_userId ON journal_entries(userId)")
                database.execSQL("CREATE INDEX index_journal_entries_type ON journal_entries(type)")

                // Add userId column to activity_cards table
                database.execSQL("ALTER TABLE activity_cards ADD COLUMN userId TEXT NOT NULL DEFAULT ''")
                database.execSQL("CREATE INDEX index_activity_cards_userId ON activity_cards(userId)")
                database.execSQL("CREATE INDEX index_activity_cards_date ON activity_cards(date)")
            }
        }

        fun getDatabase(context: Context): HealthAssistantDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HealthAssistantDatabase::class.java,
                    DATABASE_NAME
                )
                .fallbackToDestructiveMigration() // Allow schema recreation on conflicts
                .fallbackToDestructiveMigrationOnDowngrade() // Handle version downgrades
                .addMigrations(MIGRATION_4_5) // Add migration from version 4 to 5
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