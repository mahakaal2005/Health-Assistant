package com.example.health_assistant.data.local.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import com.example.health_assistant.data.local.dao.ProfileImageDao
import com.example.health_assistant.data.local.dao.PrescriptionDao
import com.example.health_assistant.data.local.dao.DiseaseCategoryDao
import com.example.health_assistant.data.local.entity.ProfileImageEntity
import com.example.health_assistant.data.local.entity.PrescriptionEntity
import com.example.health_assistant.data.local.entity.DiseaseCategoryEntity

/**
 * Room Database for local data storage
 * Version 2: Added prescriptions and disease categories support
 */
@Database(
    entities = [
        ProfileImageEntity::class,
        PrescriptionEntity::class,
        DiseaseCategoryEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class HealthAssistantDatabase : RoomDatabase() {

    abstract fun profileImageDao(): ProfileImageDao
    abstract fun prescriptionDao(): PrescriptionDao
    abstract fun diseaseCategoryDao(): DiseaseCategoryDao

    companion object {
        @Volatile
        private var INSTANCE: HealthAssistantDatabase? = null

        /**
         * Migration from version 1 to version 2
         * Adds disease categories and prescriptions tables
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create disease_categories table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `disease_categories` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `displayName` TEXT NOT NULL,
                        `iconResName` TEXT,
                        `isCustom` INTEGER NOT NULL DEFAULT 0,
                        `isActive` INTEGER NOT NULL DEFAULT 1,
                        `sortOrder` INTEGER NOT NULL DEFAULT 0,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())

                // Create prescriptions table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `prescriptions` (
                        `id` TEXT NOT NULL,
                        `userId` TEXT NOT NULL,
                        `imageUri` TEXT NOT NULL,
                        `localImagePath` TEXT NOT NULL,
                        `doctorName` TEXT NOT NULL,
                        `categoryId` TEXT NOT NULL,
                        `notes` TEXT,
                        `fileName` TEXT NOT NULL,
                        `mimeType` TEXT,
                        `fileSize` INTEGER NOT NULL,
                        `imageWidth` INTEGER,
                        `imageHeight` INTEGER,
                        `dateAdded` INTEGER NOT NULL,
                        `dateModified` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`categoryId`) REFERENCES `disease_categories`(`id`) ON DELETE SET DEFAULT
                    )
                """.trimIndent())

                // Create indices for better performance
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_prescriptions_categoryId` ON `prescriptions` (`categoryId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_prescriptions_userId` ON `prescriptions` (`userId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_prescriptions_doctorName` ON `prescriptions` (`doctorName`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_prescriptions_dateAdded` ON `prescriptions` (`dateAdded`)")

                // Insert default disease categories
                val currentTime = System.currentTimeMillis()
                val defaultCategories = listOf(
                    "('cardiology', 'cardiology', 'Cardiology', 'ic_heart', 0, 1, 1, $currentTime, $currentTime)",
                    "('diabetes', 'diabetes', 'Diabetes', 'ic_diabetes', 0, 1, 2, $currentTime, $currentTime)",
                    "('respiratory', 'respiratory', 'Respiratory', 'ic_lungs', 0, 1, 3, $currentTime, $currentTime)",
                    "('orthopedic', 'orthopedic', 'Orthopedic', 'ic_bone', 0, 1, 4, $currentTime, $currentTime)",
                    "('dermatology', 'dermatology', 'Dermatology', 'ic_skin', 0, 1, 5, $currentTime, $currentTime)",
                    "('neurology', 'neurology', 'Neurology', 'ic_brain', 0, 1, 6, $currentTime, $currentTime)",
                    "('general', 'general', 'General Medicine', 'ic_medical', 0, 1, 7, $currentTime, $currentTime)",
                    "('other', 'other', 'Other', 'ic_more', 0, 1, 8, $currentTime, $currentTime)"
                )

                for (category in defaultCategories) {
                    database.execSQL(
                        "INSERT INTO disease_categories (id, name, displayName, iconResName, isCustom, isActive, sortOrder, createdAt, updatedAt) VALUES $category"
                    )
                }
            }
        }

        fun getDatabase(context: Context): HealthAssistantDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HealthAssistantDatabase::class.java,
                    "health_assistant_database"
                )
                .addMigrations(MIGRATION_1_2)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}