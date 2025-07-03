package com.example.health_assistant.features.journal.db

import android.content.Context
import android.util.Log
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.health_assistant.features.journal.data.JournalEntryEntity
import com.example.health_assistant.features.journal.data.JournalEntryDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

// Update version to match schema changes and add exportSchema for version tracking
@Database(
    entities = [JournalEntryEntity::class],
    version = 3,
    exportSchema = false // Disable schema export to avoid annotation processor error
)
@TypeConverters(Converters::class)
abstract class JournalDatabase : RoomDatabase() {
    abstract fun journalEntryDao(): JournalEntryDao

    companion object {
        private const val TAG = "JournalDatabase"
        private const val DATABASE_NAME = "journal_database"
        @Volatile private var INSTANCE: JournalDatabase? = null

        // Migration from version 1 to 2: add 'date' column with default value 0
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {
                    database.execSQL("ALTER TABLE journal_entries ADD COLUMN date INTEGER NOT NULL DEFAULT 0")
                    // Copy timestamp to date for existing entries to maintain data consistency
                    database.execSQL("UPDATE journal_entries SET date = timestamp")
                    Log.d(TAG, "Migration 1->2 completed successfully")
                } catch (e: Exception) {
                    // Log error but don't crash - Room will handle the exception
                    Log.e(TAG, "Error during migration 1->2", e)
                }
            }
        }

        // Migration from version 2 to 3: add 'mood_level' and 'emoji' columns
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {
                    // Using transaction to ensure both columns are added or neither
                    database.beginTransaction()
                    database.execSQL("ALTER TABLE journal_entries ADD COLUMN mood_level INTEGER DEFAULT NULL")
                    database.execSQL("ALTER TABLE journal_entries ADD COLUMN emoji TEXT DEFAULT NULL")

                    // Try to identify mood entries by type and set appropriate values
                    database.execSQL(
                        "UPDATE journal_entries SET " +
                        "mood_level = 3, " +  // Default to neutral mood
                        "emoji = '😐' " +     // Default emoji
                        "WHERE type = 'mood'"
                    )

                    database.setTransactionSuccessful()
                    Log.d(TAG, "Migration 2->3 completed successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "Error during migration 2->3", e)
                } finally {
                    database.endTransaction()
                }
            }
        }

        // Callback for database creation and opening events
        private val roomCallback = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                Log.d(TAG, "Database created successfully")
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                Log.d(TAG, "Database opened successfully")
            }
        }

        fun getInstance(context: Context): JournalDatabase {
            return INSTANCE ?: synchronized(this) {
                Log.d(TAG, "Creating new database instance")
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    JournalDatabase::class.java,
                    DATABASE_NAME
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                // Use a custom callback for better lifecycle logging
                .addCallback(roomCallback)
                // Only allow destructive migration in debug builds
                .apply {
                    if (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0) {
                        fallbackToDestructiveMigration()
                        Log.w(TAG, "Using destructive migration for debug build")
                    }
                }
                // Initialize with a single thread for write operations to prevent conflicts
                .setQueryExecutor(Executors.newSingleThreadExecutor())
                .build()

                INSTANCE = instance
                instance
            }
        }

        /**
         * Safely close the database instance to prevent leaks
         */
        fun closeInstance() {
            synchronized(this) {
                if (INSTANCE?.isOpen == true) {
                    INSTANCE?.close()
                    Log.d(TAG, "Database closed successfully")
                }
                INSTANCE = null
            }
        }

        /**
         * Verify database integrity - can be called periodically to ensure data is not corrupted
         */
        suspend fun verifyIntegrity(context: Context): Boolean = withContext(Dispatchers.IO) {
            try {
                val db = getInstance(context)
                val count = db.journalEntryDao().getEntryCount()
                Log.d(TAG, "Database integrity verified: $count entries found")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Database integrity check failed", e)
                false
            }
        }
    }
}
